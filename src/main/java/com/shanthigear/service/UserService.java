package com.shanthigear.service;

import com.shanthigear.util.DateTimeUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Service class for user-related operations.
 */
@Service
public class UserService {

    private final Random random = new Random();

    /**
     * Gets subscription details for a user.
     *
     * @param username The username
     * @return Map containing subscription details
     */
    public Map<String, Object> getUserSubscriptionDetails(String username) {
        // In a real application, this would fetch data from the database
        // This is a simplified example with mock data
        
        // Example: Get subscription details with expiration
        LocalDate subscriptionDate = LocalDate.now().minusMonths(random.nextInt(12)); // Random subscription date within last year
        LocalDate expirationDate = subscriptionDate.plusYears(1); // 1-year subscription
        
        long daysUntilExpiration = ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
        boolean isTrial = random.nextBoolean();
        
        // Calculate next billing date (first of next month)
        LocalDate nextBillingDate = DateTimeUtil.nowDate()
            .withDayOfMonth(1)
            .plusMonths(1);
        
        // Format dates for display
        Map<String, Object> subscriptionDetails = new LinkedHashMap<>();
        subscriptionDetails.put("username", username);
        subscriptionDetails.put("subscriptionStart", DateTimeUtil.formatDate(subscriptionDate));
        subscriptionDetails.put("expirationDate", DateTimeUtil.formatDate(expirationDate));
        subscriptionDetails.put("daysUntilExpiration", daysUntilExpiration);
        subscriptionDetails.put("nextBillingDate", DateTimeUtil.formatDate(nextBillingDate));
        subscriptionDetails.put("isActive", daysUntilExpiration > 0);
        subscriptionDetails.put("isTrial", isTrial);
        subscriptionDetails.put("planName", isTrial ? "Free Trial" : "Premium");
        subscriptionDetails.put("lastPayment", DateTimeUtil.formatDateTime(DateTimeUtil.nowDateTime().minusDays(30)));
        subscriptionDetails.put("paymentMethod", "Credit Card ****-****-****-1234");
        subscriptionDetails.put("autoRenew", true);
        subscriptionDetails.put("maxUsers", isTrial ? 5 : 50);
        subscriptionDetails.put("storageLimitGB", isTrial ? 10 : 1000);
        
        return subscriptionDetails;
    }
    
    // Additional service methods would be added here
    // For example: createUser, updateUser, deleteUser, etc.
    
    /**
     * Example method to demonstrate service layer validation.
     * In a real application, this would interact with the database.
     */
    public boolean isUsernameAvailable(String username) {
        // This is a mock implementation
        // In a real application, you would check the database
        return !username.equalsIgnoreCase("taken");
    }
    
    /**
     * Example method to demonstrate business logic.
     * In a real application, this would interact with the database.
     */
    public Map<String, Object> getUserActivity(String username, LocalDate startDate, LocalDate endDate) {
        // This is a mock implementation
        return Map.of(
            "username", username,
            "periodStart", DateTimeUtil.formatDate(startDate),
            "periodEnd", DateTimeUtil.formatDate(endDate),
            "loginCount", random.nextInt(100),
            "documentsCreated", random.nextInt(50),
            "apiCalls", random.nextInt(1000),
            "lastActive", DateTimeUtil.formatDateTime(DateTimeUtil.nowDateTime().minusHours(random.nextInt(24)))
        );
    }
}
