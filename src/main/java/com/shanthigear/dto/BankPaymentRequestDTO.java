package com.shanthigear.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing a payment request to the bank's API.
 * Includes validation annotations to ensure data integrity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankPaymentRequestDTO {

    @NotBlank(message = "Transaction reference is required")
    @Size(max = 35, message = "Transaction reference must not exceed 35 characters")
    @JsonProperty("transaction_reference")
    private String transactionReference;

    @NotBlank(message = "Debit account number is required")
    @Size(max = 34, message = "Debit account number must not exceed 34 characters")
    @JsonProperty("debit_account_number")
    private String debitAccountNumber;

    @NotBlank(message = "Beneficiary account number is required")
    @Size(max = 34, message = "Beneficiary account number must not exceed 34 characters")
    @JsonProperty("beneficiary_account_number")
    private String beneficiaryAccountNumber;

    @NotBlank(message = "Beneficiary name is required")
    @Size(max = 140, message = "Beneficiary name must not exceed 140 characters")
    @JsonProperty("beneficiary_name")
    private String beneficiaryName;

    @NotBlank(message = "Beneficiary bank code is required")
    @Size(max = 11, message = "Beneficiary bank code must not exceed 11 characters")
    @JsonProperty("beneficiary_bank_code")
    private String beneficiaryBankCode;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 2, message = "Amount must have up to 15 digits with 2 decimal places")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency code must be in ISO 4217 format (e.g., USD, EUR, GBP)")
    @JsonProperty("currency")
    private String currency;

    @NotBlank(message = "Value date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Value date must be in YYYY-MM-DD format")
    @JsonProperty("value_date")
    private String valueDate;

    @Size(max = 140, message = "Payment details must not exceed 140 characters")
    @JsonProperty("payment_details")
    private String paymentDetails;

    @Size(max = 35, message = "End to end ID must not exceed 35 characters")
    @JsonProperty("end_to_end_id")
    private String endToEndId;

    @Size(max = 35, message = "Instruction ID must not exceed 35 characters")
    @JsonProperty("instruction_id")
    private String instructionId;

    @Size(max = 35, message = "Category purpose must not exceed 35 characters")
    @JsonProperty("category_purpose")
    private String categoryPurpose;

    @Size(max = 35, message = "Purpose code must not exceed 35 characters")
    @JsonProperty("purpose_code")
    private String purposeCode;

    @Size(max = 35, message = "Charge bearer must not exceed 35 characters")
    @JsonProperty("charge_bearer")
    private String chargeBearer;

    @Size(max = 35, message = "Remittance information must not exceed 35 characters")
    @JsonProperty("remittance_information")
    private String remittanceInformation;

    @Size(max = 35, message = "Regulatory reporting must not exceed 35 characters")
    @JsonProperty("regulatory_reporting")
    private String regulatoryReporting;

    @Size(max = 35, message = "Purpose must not exceed 35 characters")
    @JsonProperty("purpose")
    private String purpose;
}
