package com.shanthigear.service;

import com.shanthigear.exception.BatchProcessingException;
import com.shanthigear.exception.ExcelProcessingException;
import com.shanthigear.model.VendorPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorPaymentExcelService {

    private final OracleHostToHostService paymentService;
    private final VendorService vendorService;

    public List<VendorPayment> processExcelFile(MultipartFile file) throws BatchProcessingException {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            
            // Skip header row
            if (rows.hasNext()) {
                rows.next();
            }
            
            List<VendorPayment> payments = new ArrayList<>();
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                try {
                    VendorPayment payment = mapRowToPayment(currentRow);
                    if (payment != null) {
                        payments.add(payment);
                    }
                } catch (Exception e) {
                    log.error("Error processing row {}: {}", currentRow.getRowNum() + 1, e.getMessage());
                    throw new ExcelProcessingException("Error in row " + (currentRow.getRowNum() + 1) + ": " + e.getMessage());
                }
            }
            
            // Process payments through Oracle H2H
            return paymentService.processBatchPayments(payments);
            
        } catch (IOException e) {
            log.error("Error processing Excel file: {}", e.getMessage(), e);
            throw new ExcelProcessingException("Failed to process Excel file: " + e.getMessage(), e);
        }
    }
    
    private VendorPayment mapRowToPayment(Row row) {
        if (isRowEmpty(row)) {
            return null;
        }
        
        VendorPayment payment = new VendorPayment();
        
        // Map Excel columns to VendorPayment fields
        String vendorCode = getStringValue(row.getCell(0));
        String invoiceNumber = getStringValue(row.getCell(1));
        BigDecimal amount = new BigDecimal(getStringValue(row.getCell(2)));
        String currency = getStringValue(row.getCell(3));
        LocalDate dueDate = row.getCell(4).getLocalDateTimeCellValue().toLocalDate();
        
        // Set payment details
        payment.setPaymentReference(generatePaymentReference());
        payment.setInvoiceNumber(invoiceNumber);
        payment.setAmount(amount);
        payment.setPaymentDate(dueDate); // Using dueDate as payment date
        payment.setRemarks("Currency: " + currency); // Store currency in remarks
        payment.setStatus("PENDING");
        
        // Set vendor details
        payment.setVendor(vendorService.findByVendorNumber(vendorCode)
            .orElseThrow(() -> new ExcelProcessingException("Vendor not found: " + vendorCode)));
            
        return payment;
    }
    
    private String generatePaymentReference() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private String getStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
    
    public byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Vendor Payments");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Vendor Code", "Invoice Number", "Amount", "Currency", "Due Date"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // Add sample data row
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("VENDOR001");
            dataRow.createCell(1).setCellValue("INV-2023-001");
            dataRow.createCell(2).setCellValue(1000.00);
            dataRow.createCell(3).setCellValue("USD");
            dataRow.createCell(4).setCellValue(LocalDate.now().plusDays(30).toString());
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
