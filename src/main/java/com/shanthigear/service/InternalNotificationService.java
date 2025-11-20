package com.shanthigear.service;

import com.shanthigear.config.NotificationConfig;
import com.shanthigear.model.*;
import com.shanthigear.repository.VendorPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling internal notifications to finance and operations teams.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalNotificationService {

    private final NotificationService notificationService;
    private final NotificationConfig notificationConfig;
    private final VendorPaymentRepository vendorPaymentRepository;
    
    // In-memory store for exceptions (in a real app, use a database)
    private final List<PaymentException> exceptionStore = Collections.synchronizedList(new ArrayList<>());
    private final List<ActionItem> actionItemStore = Collections.synchronizedList(new ArrayList<>());

    /**
     * Send a payment summary to the finance team.
     * @param batchId The batch ID for the payment summary
     */
    public void sendPaymentSummary(String batchId) {
        if (!notificationConfig.getPaymentSummary().isEnabled()) {
            log.info("Payment summary notifications are disabled");
            return;
        }

        // Get payments for the batch
        List<VendorPayment> payments = vendorPaymentRepository.findByBatchId(batchId);
        
        if (payments.isEmpty()) {
            log.warn("No payments found for batch: {}", batchId);
            return;
        }
        
        // Create payment summary
        PaymentSummary summary = new PaymentSummary();
        summary.setBatchId(batchId);
        summary.setPaymentDate(LocalDate.now());
        summary.setTotalPayments(payments.size());
        
        // Calculate total amount and group by status
        Map<String, Integer> statusCounts = new HashMap<>();
        Map<String, BigDecimal> amountByStatus = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (VendorPayment payment : payments) {
            String status = payment.getStatus() != null ? payment.getStatus().name() : "UNKNOWN";
            statusCounts.merge(status, 1, Integer::sum);
            
            BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            amountByStatus.merge(status, amount, BigDecimal::add);
            totalAmount = totalAmount.add(amount);
        }
        
        summary.setStatusCounts(statusCounts);
        summary.setCurrency("INR");
        summary.setTotalAmount(totalAmount);
        summary.setCurrencyBreakdown(amountByStatus);
        
        // Add payment details
        List<PaymentSummary.PaymentDetail> paymentDetails = payments.stream()
            .map(payment -> {
                PaymentSummary.PaymentDetail detail = new PaymentSummary.PaymentDetail();
                detail.setPaymentReference(payment.getReferenceNumber());
                detail.setVendorId(payment.getVendorId());
                detail.setVendorName(payment.getVendorName());
                detail.setAmount(payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO);
                detail.setStatus(payment.getStatus() != null ? payment.getStatus().name() : "UNKNOWN");
                detail.setBankAccount(payment.getBankAccount());
                detail.setIfscCode(payment.getIfscCode());
                detail.setUtrNumber(payment.getReferenceNumber());
                return detail;
            })
            .collect(Collectors.toList());
            
        summary.setPaymentDetails(paymentDetails);
        summary.setGeneratedBy("System");
        summary.setGeneratedAt(LocalDate.now());
        
        // Get recipients from config or use default
        List<String> recipients = notificationConfig.getPaymentSummary().getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            recipients = Collections.singletonList("finance-team@shanthigears.com");
        }
        
        // Send the notification
        notificationService.sendPaymentSummary(summary, recipients);
    }
    
    /**
     * Record a payment exception.
     * @param exception The exception to record
     */
    public void recordException(PaymentException exception) {
        exception.setTimestamp(LocalDateTime.now());
        exceptionStore.add(exception);
        
        // If this is a critical exception, send immediate notification
        if ("CRITICAL".equals(exception.getSeverity())) {
            sendExceptionReport(Collections.singletonList(exception), 
                Collections.singletonList("it-support@shanthigears.com"));
        }
        
        // Check if we should send the exception report based on threshold
        if (exceptionStore.size() >= notificationConfig.getExceptionReport().getThreshold()) {
            sendScheduledExceptionReport();
        }
    }
    
    /**
     * Send scheduled exception report.
     */
    @Scheduled(cron = "${app.notification.exception-report.schedule:0 0 17 * * MON-FRI}")
    public void sendScheduledExceptionReport() {
        if (!notificationConfig.getExceptionReport().isEnabled() || exceptionStore.isEmpty()) {
            return;
        }
        
        // Get unresolved exceptions
        List<PaymentException> unresolvedExceptions = exceptionStore.stream()
            .filter(e -> !"RESOLVED".equals(e.getStatus()))
            .collect(Collectors.toList());
            
        if (unresolvedExceptions.isEmpty()) {
            return;
        }
        
        // Get recipients from config or use default
        List<String> recipients = notificationConfig.getExceptionReport().getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            recipients = Arrays.asList(
                "operations@shanthigears.com", 
                "finance-team@shanthigears.com"
            );
        }
        
        // Send the report
        sendExceptionReport(unresolvedExceptions, recipients);
    }
    
    private void sendExceptionReport(List<PaymentException> exceptions, List<String> recipients) {
        if (exceptions.isEmpty() || recipients.isEmpty()) {
            return;
        }
        
        try {
            // Filter exceptions if needed
            List<PaymentException> exceptionsToSend = exceptions;
            if (!notificationConfig.getExceptionReport().isIncludeResolved()) {
                exceptionsToSend = exceptions.stream()
                    .filter(e -> !"RESOLVED".equals(e.getStatus()))
                    .collect(Collectors.toList());
            }
            
            // Limit the number of exceptions to send
            int maxExceptions = notificationConfig.getExceptionReport().getMaxExceptionsPerReport();
            if (exceptionsToSend.size() > maxExceptions) {
                exceptionsToSend = exceptionsToSend.subList(0, maxExceptions);
            }
            
            if (exceptionsToSend.isEmpty()) {
                return;
            }
            
            // Send the notification
            notificationService.sendExceptionReport(exceptionsToSend, recipients);
            
            // Mark exceptions as reported
            exceptionsToSend.forEach(e -> e.setReported(true));
            
        } catch (Exception e) {
            log.error("Failed to process payment exception: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Add a new action item.
     * @param actionItem The action item to add
     */
    public void addActionItem(ActionItem actionItem) {
        if (actionItem.getId() == null) {
            actionItem.setId(UUID.randomUUID().toString());
        }
        if (actionItem.getStatus() == null) {
            // actionItem.setReported(true); // Commented out - method not found
            actionItem.setStatus("OPEN");
        }
        if (actionItem.getPriority() == null) {
            actionItem.setPriority("MEDIUM");
        }
        if (actionItem.getAssignedTo() == null && notificationConfig.getActionItems().isAutoAssignToTeamLead()) {
            // In a real app, determine the team lead based on the category
            actionItem.setAssignedTo("team-lead@shanthigears.com");
        }
        
        actionItemStore.add(actionItem);
        
        // If this is a high priority item, send immediate notification
        if ("HIGH".equals(actionItem.getPriority())) {
            sendActionItemNotification(Collections.singletonList(actionItem), 
                Collections.singletonList(actionItem.getAssignedTo()));
        }
    }
    
    /**
     * Send action item reminders.
     */
    @Scheduled(cron = "${app.notification.action-items.reminder.schedule:0 0 9 * * MON-FRI}")
    public void sendActionItemReminders() {
        if (!notificationConfig.getActionItems().getReminder().isEnabled()) {
            return;
        }
        
        // Find action items that are due soon or overdue
        LocalDate dueDateThreshold = LocalDate.now().plusDays(
            notificationConfig.getActionItems().getReminder().getDaysBeforeDue());
            
        List<ActionItem> upcomingItems = actionItemStore.stream()
            .filter(item -> "OPEN".equals(item.getStatus()) || "IN_PROGRESS".equals(item.getStatus()))
            .filter(item -> item.getDueDate() != null && 
                !item.getDueDate().isAfter(dueDateThreshold))
            .collect(Collectors.toList());
            
        if (upcomingItems.isEmpty()) {
            return;
        }
        
        // Group by assignee
        Map<String, List<ActionItem>> itemsByAssignee = upcomingItems.stream()
            .filter(item -> item.getAssignedTo() != null)
            .collect(Collectors.groupingBy(ActionItem::getAssignedTo));
            
        // Send notifications to each assignee
        itemsByAssignee.forEach((assignee, items) -> 
            sendActionItemNotification(items, Collections.singletonList(assignee)));
    }
    
    private void sendActionItemNotification(List<ActionItem> items, List<String> recipients) {
        if (items.isEmpty() || recipients.isEmpty()) {
            return;
        }
        
        try {
            notificationService.sendActionItems(items, recipients);
            
            // Mark items as notified
            // items.forEach(item -> item.setLastNotified(LocalDateTime.now())); // Commented out - method not found
            
        } catch (Exception e) {
            log.error("Failed to send action item notification", e);
        }
    }
    
    /**
     * Send daily summary report.
     */
    @Scheduled(cron = "${app.notification.daily-summary.schedule:0 0 19 * * MON-FRI}")
    public void sendDailySummary() {
        if (!notificationConfig.getDailySummary().isEnabled()) {
            return;
        }
        
        // Get today's payments (in a real app, fetch from database)
        LocalDate today = LocalDate.now();
        List<VendorPayment> todaysPayments = vendorPaymentRepository.findByBatchId("DAILY-" + today);
        
        if (todaysPayments.isEmpty()) {
            log.info("No payments found for today, skipping daily summary");
            return;
        }
        
        // Create payment summary for today
        PaymentSummary summary = new PaymentSummary();
        summary.setBatchId("DAILY-" + today);
        summary.setPaymentDate(today);
        summary.setTotalPayments(todaysPayments.size());
        
        // Calculate total amount and group by status
        Map<String, Integer> statusCounts = new HashMap<>();
        Map<String, BigDecimal> amountByStatus = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (VendorPayment payment : todaysPayments) {
            String status = payment.getStatus() != null ? payment.getStatus().name() : "UNKNOWN";
            statusCounts.merge(status, 1, Integer::sum);
            
            BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            amountByStatus.merge(status, amount, BigDecimal::add);
            totalAmount = totalAmount.add(amount);
        }
        
        summary.setStatusCounts(statusCounts);
        summary.setCurrency("INR");
        summary.setTotalAmount(totalAmount);
        summary.setCurrencyBreakdown(amountByStatus);
        
        // Add payment details (limited)
        int maxPayments = notificationConfig.getDailySummary().getMaxPaymentsToShow();
        List<PaymentSummary.PaymentDetail> paymentDetails = todaysPayments.stream()
            .limit(maxPayments)
            .map(payment -> {
                PaymentSummary.PaymentDetail detail = new PaymentSummary.PaymentDetail();
                detail.setPaymentReference(payment.getReferenceNumber());
                detail.setVendorId(payment.getVendorId());
                // detail.setVendorName(vendorService.getVendorName(payment.getVendorId())); // Commented out - method not found
                detail.setAmount(payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO);
                detail.setStatus(payment.getStatus() != null ? payment.getStatus().name() : "UNKNOWN");
                // detail.setTimestamp(payment.getCreatedAt()); // Commented out - method not found in PaymentDetail
                return detail;
            })
            .collect(Collectors.toList());
            
        summary.setPaymentDetails(paymentDetails);
        summary.setGeneratedBy("Daily Summary Job");
        summary.setGeneratedAt(LocalDate.now());
        
        // Get recipients from config or use default
        List<String> recipients = notificationConfig.getDailySummary().getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            recipients = Arrays.asList(
                "management@shanthigears.com",
                "finance-team@shanthigears.com",
                "operations@shanthigears.com"
            );
        }
        
        // Send the notification
        notificationService.sendDailySummary(summary, recipients);
    }
}
