package com.shanthigear.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportResponse {
    private int totalRecords;
    private int successCount;
    private int failureCount;
    private List<String> errors;
    private String reportUrl;
}
