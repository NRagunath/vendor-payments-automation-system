package com.shanthigear.payload.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment request DTO for bank payment processing.
 */
public class PaymentRequest {
    
    @NotBlank(message = "Payment reference is required")
    private String paymentReference;
    
    @NotBlank(message = "Beneficiary account number is required")
    @Pattern(regexp = "^[0-9]{9,18}$", message = "Invalid account number format")
    private String beneficiaryAccountNumber;
    
    @NotBlank(message = "Beneficiary name is required")
    @Size(max = 100, message = "Beneficiary name must be less than 100 characters")
    private String beneficiaryName;
    
    @NotBlank(message = "Beneficiary bank code is required")
    @Size(min = 8, max = 11, message = "Bank code must be between 8 and 11 characters")
    private String beneficiaryBankCode;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency;
    
    @NotBlank(message = "Payment purpose is required")
    @Size(max = 50, message = "Payment purpose must be less than 50 characters")
    private String paymentPurpose;
    
    @Size(max = 255, message = "Remarks must be less than 255 characters")
    private String remarks;
    
    private String debitAccountNumber;
    private String description;
    private LocalDateTime requestTime;
    
    // Getters and Setters
    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getBeneficiaryAccountNumber() {
        return beneficiaryAccountNumber;
    }

    public void setBeneficiaryAccountNumber(String beneficiaryAccountNumber) {
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getBeneficiaryBankCode() {
        return beneficiaryBankCode;
    }

    public void setBeneficiaryBankCode(String beneficiaryBankCode) {
        this.beneficiaryBankCode = beneficiaryBankCode;
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

    public String getPaymentPurpose() {
        return paymentPurpose;
    }

    public void setPaymentPurpose(String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    
    public String getDebitAccountNumber() {
        return debitAccountNumber;
    }
    
    public void setDebitAccountNumber(String debitAccountNumber) {
        this.debitAccountNumber = debitAccountNumber;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getRequestTime() {
        return requestTime;
    }
    
    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }
}
