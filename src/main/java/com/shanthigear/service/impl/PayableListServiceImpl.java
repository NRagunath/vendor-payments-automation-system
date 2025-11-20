package com.shanthigear.service.impl;

import com.shanthigear.dto.PayableItemDTO;
import com.shanthigear.exception.PaymentProcessingException;
import com.shanthigear.exception.PaymentValidationException;
import com.shanthigear.model.PaymentDetails;
import com.shanthigear.repository.PaymentDetailsRepository;
import com.shanthigear.repository.VendorRepository;
import com.shanthigear.service.PayableListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayableListServiceImpl implements PayableListService {

    private final PaymentDetailsRepository paymentDetailsRepository;
    private final VendorRepository vendorRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public List<PaymentDetails> processPayableList(MultipartFile file, String currentUser) {
        try {
            List<PayableItemDTO> payableItems = parseExcelFile(file);
            validatePayableItems(payableItems);
            return savePaymentDetails(payableItems, currentUser);
        } catch (IOException e) {
            log.error("Error processing payable list file: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Error processing file: " + e.getMessage());
        }
    }

    private List<PayableItemDTO> parseExcelFile(MultipartFile file) throws IOException {
        List<PayableItemDTO> payableItems = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            
            // Skip header row
            if (rows.hasNext()) {
                rows.next();
            }
            
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (isRowEmpty(currentRow)) {
                    continue;
                }
                
                PayableItemDTO item = new PayableItemDTO();
                item.setVendorNumber(getStringCellValue(currentRow.getCell(0)));
                item.setVendorName(getStringCellValue(currentRow.getCell(1)));
                item.setVendorSite(getStringCellValue(currentRow.getCell(2)));
                item.setPayGroup(getStringCellValue(currentRow.getCell(3)));
                item.setAmountToPay(getNumericCellValue(currentRow.getCell(4)));
                
                payableItems.add(item);
            }
        }
        
        return payableItems;
    }
    
    private void validatePayableItems(List<PayableItemDTO> payableItems) {
        List<String> errors = new ArrayList<>();
        
        for (int i = 0; i < payableItems.size(); i++) {
            PayableItemDTO item = payableItems.get(i);
            int rowNum = i + 2; // 1-based index + header row
            
            if (!vendorRepository.existsByVendorNumber(item.getVendorNumber())) {
                errors.add(String.format("Row %d: Vendor with number %s not found", rowNum, item.getVendorNumber()));
            }
            
            if (item.getAmountToPay() == null || item.getAmountToPay().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(String.format("Row %d: Invalid amount %s", rowNum, item.getAmountToPay()));
            }
        }
        
        if (!errors.isEmpty()) {
            throw new PaymentValidationException("Validation failed: " + String.join(", ", errors));
        }
    }
    
    private List<PaymentDetails> savePaymentDetails(List<PayableItemDTO> payableItems, String currentUser) {
        return payableItems.stream()
            .map(item -> {
                PaymentDetails details = modelMapper.map(item, PaymentDetails.class);
                details.setAmount(item.getAmountToPay());
                details.setCreatedBy(currentUser);
                return paymentDetailsRepository.save(details);
            })
            .collect(Collectors.toList());
    }
    
    // Helper methods for Excel parsing
    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
    
    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        
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
            case BLANK:
                return "";
            default:
                return "";
        }
    }
    
    private BigDecimal getNumericCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<PaymentDetails> getPaymentDetailsByVendor(String vendorNumber) {
        return paymentDetailsRepository.findByVendorNumber(vendorNumber);
    }

    @Override
    public List<PaymentDetails> getPendingPayments() {
        return paymentDetailsRepository.findByStatus("PENDING");
    }

    @Override
    @Transactional
    public PaymentDetails updatePaymentStatus(String paymentReference, String newStatus, String updatedBy) {
        return paymentDetailsRepository.findByPaymentReference(paymentReference)
            .map(payment -> {
                payment.setStatus(newStatus);
                payment.setCreatedBy(updatedBy);
                return paymentDetailsRepository.save(payment);
            })
            .orElseThrow(() -> new PaymentValidationException("Payment not found with reference: " + paymentReference));
    }
}
