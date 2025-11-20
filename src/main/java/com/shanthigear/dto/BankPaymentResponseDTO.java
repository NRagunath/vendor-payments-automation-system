package com.shanthigear.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a payment response from the bank's API.
 * Maps the JSON response from the bank's payment processing endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankPaymentResponseDTO {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("status_code")
    private String statusCode;

    @JsonProperty("status_description")
    private String statusDescription;

    @JsonProperty("transaction_reference")
    private String transactionReference;

    @JsonProperty("debit_account_number")
    private String debitAccountNumber;

    @JsonProperty("beneficiary_account_number")
    private String beneficiaryAccountNumber;

    @JsonProperty("beneficiary_name")
    private String beneficiaryName;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("value_date")
    private String valueDate;

    @JsonProperty("processing_date")
    private String processingDate;

    @JsonProperty("end_to_end_id")
    private String endToEndId;

    @JsonProperty("instruction_id")
    private String instructionId;

    @JsonProperty("bank_reference")
    private String bankReference;

    @JsonProperty("fees")
    private BigDecimal fees;

    @JsonProperty("tax")
    private BigDecimal tax;

    @JsonProperty("exchange_rate")
    private BigDecimal exchangeRate;

    @JsonProperty("debit_currency")
    private String debitCurrency;

    @JsonProperty("credit_currency")
    private String creditCurrency;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("additional_info")
    private Object additionalInfo;

    /**
     * Creates a failed payment response with the given error message.
     *
     * @param errorMessage the error message
     * @return a failed payment response
     */
    public static BankPaymentResponseDTO createFailedResponse(String errorMessage) {
        return BankPaymentResponseDTO.builder()
                .success(false)
                .status("FAILED")
                .statusCode("PAYMENT_FAILED")
                .statusDescription(errorMessage)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Creates a successful payment response with the given transaction details.
     *
     * @param transactionId the transaction ID
     * @param reference the transaction reference
     * @param status the transaction status
     * @return a successful payment response
     */
    public static BankPaymentResponseDTO createSuccessResponse(
            String transactionId, String reference, String status) {
        return BankPaymentResponseDTO.builder()
                .success(true)
                .transactionId(transactionId)
                .transactionReference(reference)
                .status(status)
                .statusCode("SUCCESS")
                .statusDescription("Payment processed successfully")
                .build();
    }

    /**
     * Checks if the payment was successful.
     *
     * @return true if the payment was successful, false otherwise
     */
    public boolean isSuccessful() {
        return success && 
               (status != null && 
                (status.equalsIgnoreCase("SUCCESS") || 
                 status.equalsIgnoreCase("COMPLETED") || 
                 status.equalsIgnoreCase("PROCESSING")));
    }

    /**
     * Checks if the payment is still processing.
     *
     * @return true if the payment is still processing, false otherwise
     */
    public boolean isProcessing() {
        return status != null && 
               (status.equalsIgnoreCase("PENDING") || 
                status.equalsIgnoreCase("PROCESSING") ||
                status.equalsIgnoreCase("IN_PROGRESS"));
    }

    /**
     * Checks if the payment failed.
     *
     * @return true if the payment failed, false otherwise
     */
    public boolean isFailed() {
        return !success || 
               (status != null && 
                (status.equalsIgnoreCase("FAILED") || 
                 status.equalsIgnoreCase("REJECTED") || 
                 status.equalsIgnoreCase("CANCELLED") ||
                 status.equalsIgnoreCase("DECLINED")));
    }
}
