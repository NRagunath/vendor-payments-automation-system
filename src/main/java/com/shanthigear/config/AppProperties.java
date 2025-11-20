package com.shanthigear.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final PaymentProperties payment = new PaymentProperties();
    private final NotificationProperties notification = new NotificationProperties();
    private final SchedulingProperties scheduling = new SchedulingProperties();
    private final SecurityProperties security = new SecurityProperties();

    // Getters
    public PaymentProperties getPayment() {
        return payment;
    }

    public NotificationProperties getNotification() {
        return notification;
    }

    public SchedulingProperties getScheduling() {
        return scheduling;
    }
    public SecurityProperties getSecurity() {
        return security;
    }

    public static class PaymentProperties {
        private List<String> allowedCurrencies;
        private String defaultCurrency;
        private double minAmount;
        private double maxAmount;
        private int rateLimit;
        private Duration rateLimitDuration;

        public void validate() {
            Assert.notNull(allowedCurrencies, "Allowed currencies must not be null");
            Assert.notNull(defaultCurrency, "Default currency must not be null");
            if (minAmount < 0) {
                throw new IllegalStateException("Min amount must be greater than or equal to 0");
            }
            if (maxAmount <= minAmount) {
                throw new IllegalStateException("Max amount must be greater than min amount");
            }
        }


        public List<String> getAllowedCurrencies() {
            return allowedCurrencies;
        }

        public void setAllowedCurrencies(List<String> allowedCurrencies) {
            this.allowedCurrencies = allowedCurrencies;
        }

        public String getDefaultCurrency() {
            return defaultCurrency;
        }

        public void setDefaultCurrency(String defaultCurrency) {
            this.defaultCurrency = defaultCurrency;
        }

        public double getMinAmount() {
            return minAmount;
        }

        public void setMinAmount(double minAmount) {
            this.minAmount = minAmount;
        }

        public double getMaxAmount() {
            return maxAmount;
        }

        public void setMaxAmount(double maxAmount) {
            this.maxAmount = maxAmount;
        }


        public int getRateLimit() {
            return rateLimit;
        }

        public void setRateLimit(int rateLimit) {
            this.rateLimit = rateLimit;
        }

        public Duration getRateLimitDuration() {
            return rateLimitDuration;
        }

        public void setRateLimitDuration(Duration rateLimitDuration) {
            this.rateLimitDuration = rateLimitDuration;
        }
    }


    public static class NotificationProperties {
        private final EmailProperties email = new EmailProperties();
        private final SmsProperties sms = new SmsProperties();

        public EmailProperties getEmail() {
            return email;
        }

        public SmsProperties getSms() {
            return sms;
        }

        public static class EmailProperties {
            private boolean enabled;
            private String from;

            public boolean isEnabled() {
                return enabled;
            }
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
            public String getFrom() {
                return from;
            }
            public void setFrom(String from) {
                this.from = from;
            }
        }


        public static class SmsProperties {
            private boolean enabled;

            public boolean isEnabled() {
                return enabled;
            }
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }


    public static class SchedulingProperties {
        private boolean enabled;
        private int poolSize;
        private String reconcilePaymentsCron;
        private String updateVendorStatusCron;

        public boolean isEnabled() {
            return enabled;
        }
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        public int getPoolSize() {
            return poolSize;
        }
        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }
        public String getReconcilePaymentsCron() {
            return reconcilePaymentsCron;
        }
        public void setReconcilePaymentsCron(String reconcilePaymentsCron) {
            this.reconcilePaymentsCron = reconcilePaymentsCron;
        }
        public String getUpdateVendorStatusCron() {
            return updateVendorStatusCron;
        }
        public void setUpdateVendorStatusCron(String updateVendorStatusCron) {
            this.updateVendorStatusCron = updateVendorStatusCron;
        }
    }


    public static class SecurityProperties {
        private final JwtProperties jwt = new JwtProperties();
        private final CorsProperties cors = new CorsProperties();

        public JwtProperties getJwt() {
            return jwt;
        }
        public CorsProperties getCors() {
            return cors;
        }

        public static class JwtProperties {
            private String secret;
            private long expiration;

            public String getSecret() {
                return secret;
            }
            public void setSecret(String secret) {
                this.secret = secret;
            }
            public long getExpiration() {
                return expiration;
            }
            public void setExpiration(long expiration) {
                this.expiration = expiration;
            }
        }


        public static class CorsProperties {
            private String[] allowedOrigins;
            private String[] allowedMethods;

            public String[] getAllowedOrigins() {
                return allowedOrigins;
            }
            public void setAllowedOrigins(String allowedOrigins) {
                this.allowedOrigins = allowedOrigins.split(",");
            }
            public String[] getAllowedMethods() {
                return allowedMethods;
            }
            public void setAllowedMethods(String allowedMethods) {
                this.allowedMethods = allowedMethods.split(",");
            }
        }
    }
}
