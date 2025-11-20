package com.shanthigear.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Data Transfer Object for payment requests.
 * Contains all necessary information to process a payment to a vendor.
 * All payments are processed in Indian Rupees (INR).
 */
public class PaymentRequestDTO {
    private static final String DEFAULT_CURRENCY = "INR";
    
    @NotBlank(message = "Vendor ID is required")
    @Size(max = 50, message = "Vendor ID must be less than 50 characters")
    private String vendorId;

    @NotBlank(message = "Payment reference is required")
    @Size(max = 100, message = "Payment reference must be less than 100 characters")
    private String paymentReference;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 12, fraction = 2, message = "Amount must have up to 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency = DEFAULT_CURRENCY; // Fixed to INR as all vendors are in India

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(BANK_TRANSFER|CREDIT_CARD|PAYPAL|OTHER)$", 
             message = "Invalid payment method. Must be one of: BANK_TRANSFER, CREDIT_CARD, PAYPAL, OTHER")
    private String paymentMethod;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @Size(max = 100, message = "Invoice number must be less than 100 characters")
    private String invoiceNumber;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Due date must be in YYYY-MM-DD format")
    private String dueDate;
    
    @Size(max = 1000, message = "Additional notes must be less than 1000 characters")
    private String additionalNotes;
    
    public PaymentRequestDTO() {
        // Default constructor
    }
    
    private PaymentRequestDTO(PaymentRequestDTOBuilder builder) {
        this.vendorId = builder.vendorId;
        this.paymentReference = builder.paymentReference;
        this.amount = builder.amount;
        this.currency = builder.currency != null ? builder.currency : DEFAULT_CURRENCY;
        this.paymentMethod = builder.paymentMethod;
        this.description = builder.description;
        this.invoiceNumber = builder.invoiceNumber;
        this.dueDate = builder.dueDate;
        this.additionalNotes = builder.additionalNotes;
        
        // Validate the object after construction
        validate();
    }
    
    private void validate() {
        // Validation is handled by the annotations, but we keep this for additional validation if needed
        if (!DEFAULT_CURRENCY.equals(currency)) {
            throw new IllegalArgumentException("Only " + DEFAULT_CURRENCY + " currency is supported for payments");
        }
    }
    
    public static PaymentRequestDTOBuilder builder() {
        return new PaymentRequestDTOBuilder();
    }

    // Builder class
    public static class PaymentRequestDTOBuilder {
        private String vendorId;
        private String paymentReference;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String description;
        private String invoiceNumber;
        private String dueDate;
        private String additionalNotes;
        
        public PaymentRequestDTOBuilder vendorId(String vendorId) {
            this.vendorId = vendorId;
            return this;
        }
        
        public PaymentRequestDTOBuilder paymentReference(String paymentReference) {
            this.paymentReference = paymentReference;
            return this;
        }
        
        public PaymentRequestDTOBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        
        public PaymentRequestDTOBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        
        public PaymentRequestDTOBuilder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }
        
        public PaymentRequestDTOBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public PaymentRequestDTOBuilder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }
        
        public PaymentRequestDTOBuilder dueDate(String dueDate) {
            this.dueDate = dueDate;
            return this;
        }
        
        public PaymentRequestDTOBuilder additionalNotes(String additionalNotes) {
            this.additionalNotes = additionalNotes;
            return this;
        }
        
        public PaymentRequestDTO build() {
            return new PaymentRequestDTO(this);
        }
    }
    
    // Getters and Setters
    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentRequestDTO that = (PaymentRequestDTO) o;
        return Objects.equals(vendorId, that.vendorId) &&
               Objects.equals(paymentReference, that.paymentReference) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(paymentMethod, that.paymentMethod) &&
               Objects.equals(description, that.description) &&
               Objects.equals(invoiceNumber, that.invoiceNumber) &&
               Objects.equals(dueDate, that.dueDate) &&
               Objects.equals(additionalNotes, that.additionalNotes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendorId, paymentReference, amount, paymentMethod, 
                          description, invoiceNumber, dueDate, additionalNotes);
    }

    @Override
    public String toString() {
        return "PaymentRequestDTO{" +
               "vendorId='" + vendorId + '\'' +
               ", paymentReference='" + paymentReference + '\'' +
               ", amount=" + amount + " " + currency +
               ", paymentMethod='" + paymentMethod + '\'' +
               ", description='" + description + '\'' +
               ", invoiceNumber='" + invoiceNumber + '\'' +
               ", dueDate='" + dueDate + '\'' +
               ", additionalNotes='" + additionalNotes + '\'' +
               '}';
    }
    
    public String getAdditionalNotes() {
        return additionalNotes;
    }
    
    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }
}
