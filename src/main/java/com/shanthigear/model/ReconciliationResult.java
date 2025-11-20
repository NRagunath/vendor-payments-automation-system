package com.shanthigear.model;

import com.shanthigear.service.OracleHostToHostService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a payment reconciliation process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResult implements OracleHostToHostService.ReconciliationResult {
    private boolean success;
    private String batchId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDateTime reconciliationDate;
    private int totalRecords;
    private int matchedRecords;
    private int unmatchedRecords;
    private double totalAmount;
    private double matchedAmount;
    @Builder.Default
    private List<OracleHostToHostService.ReconciliationMismatch> mismatches = new ArrayList<>();
    
    @Override
    public LocalDate getFromDate() {
        return fromDate;
    }

    @Override
    public LocalDate getToDate() {
        return toDate;
    }

    @Override
    public int getTotalRecords() {
        return totalRecords;
    }

    @Override
    public int getMatchedRecords() {
        return matchedRecords;
    }

    @Override
    public int getUnmatchedRecords() {
        return unmatchedRecords;
    }

    @Override
    public List<OracleHostToHostService.ReconciliationMismatch> getMismatches() {
        return mismatches;
    }

    public void addMismatch(OracleHostToHostService.ReconciliationMismatch mismatch) {
        this.mismatches.add(mismatch);
        this.unmatchedRecords++;
    }
    
    public void incrementMatchedRecords() {
        this.matchedRecords++;
    }
    
    public void incrementUnmatchedRecords() {
        this.unmatchedRecords++;
    }
    
    public void incrementTotalRecords() {
        this.totalRecords++;
    }
}
