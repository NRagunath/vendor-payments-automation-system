package com.shanthigear.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for notification settings.
 */
@Configuration
@ConfigurationProperties(prefix = "app.notification")
@Data
public class NotificationConfig {
    private PaymentSummary paymentSummary = new PaymentSummary();
    private ExceptionReport exceptionReport = new ExceptionReport();
    private ActionItems actionItems = new ActionItems();
    private DailySummary dailySummary = new DailySummary();
    
    @Data
    public static class PaymentSummary {
        private boolean enabled = true;
        private String schedule = "0 0 18 * * MON-FRI";  // 6 PM on weekdays
        private List<String> recipients;
        private boolean includeFailedPayments = true;
        private boolean includePendingApprovals = true;
    }
    
    @Data
    public static class ExceptionReport {
        private boolean enabled = true;
        private int threshold = 1;  // Send report if at least 1 exception
        private String schedule = "0 0 17 * * MON-FRI";  // 5 PM on weekdays
        private List<String> recipients;
        private boolean includeResolved = false;
        private int maxExceptionsPerReport = 100;
    }
    
    @Data
    public static class ActionItems {
        private Reminder reminder = new Reminder();
        private boolean autoAssignToTeamLead = true;
        private int defaultDueDays = 3;
        
        @Data
        public static class Reminder {
            private boolean enabled = true;
            private String schedule = "0 0 9 * * MON-FRI";  // 9 AM on weekdays
            private int daysBeforeDue = 1;  // Send reminder 1 day before due date
        }
    }
    
    @Data
    public static class DailySummary {
        private boolean enabled = true;
        private String schedule = "0 0 19 * * MON-FRI";  // 7 PM on weekdays
        private List<String> recipients;
        private boolean includePerformanceMetrics = true;
        private boolean includeSystemStatus = true;
        private int maxPaymentsToShow = 10;
    }
}
