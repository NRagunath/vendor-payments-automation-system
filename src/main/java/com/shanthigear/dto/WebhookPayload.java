package com.shanthigear.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WebhookPayload {
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("payment_id")
    private String paymentId;
    
    @JsonProperty("transaction_id")
    private String transactionId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("metadata")
    private Object metadata;  // For any additional data
    
    @JsonProperty("utr_number")
    private String utrNumber;  // UTR/Reference number for the transaction
}
