package com.shanthigear.util;

import com.shanthigear.model.Vendor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {
    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static String[] HEADERs = {"Vendor ID", "Name", "Email", "Bank Account Number", "IFSC Code", "Bank Branch", "Phone"};
    static String SHEET = "Vendors";

    public static boolean hasExcelFormat(MultipartFile file) {
        return TYPE.equals(file.getContentType());
    }

    public static List<Vendor> excelToVendors(InputStream is) {
        try {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheet(SHEET);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0); // Fallback to first sheet
            }
            
            Iterator<Row> rows = sheet.iterator();
            List<Vendor> vendors = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                // Skip header
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                Iterator<Cell> cellsInRow = currentRow.iterator();
                Vendor.VendorBuilder vendorBuilder = Vendor.builder();
                int cellIdx = 0;
                
                while (cellsInRow.hasNext()) {
                    Cell currentCell = cellsInRow.next();
                    String cellValue = "";
                    if (currentCell != null) {
                        cellValue = currentCell.getCellType() == CellType.STRING ? 
                            currentCell.getStringCellValue() : 
                            String.valueOf(currentCell.getNumericCellValue());
                    }
                    
                    switch (cellIdx) {
                        case 0:
                            vendorBuilder.vendorNumber(cellValue.trim());
                            break;
                        case 1:
                            vendorBuilder.vendorName(cellValue.trim());
                            break;
                        case 2:
                            vendorBuilder.emailAddress(cellValue.trim());
                            break;
                        case 3:
                            vendorBuilder.bankAccountNum(cellValue.trim());
                            break;
                        case 4:
                            vendorBuilder.ifscCode(cellValue.trim());
                            break;
                        case 5:
                            vendorBuilder.branch(cellValue.trim());
                            break;
                        default:
                            break;
                    }
                    cellIdx++;
                }
                
                // Build the vendor with the mapped fields
                Vendor vendor = vendorBuilder.build();
                    
                vendors.add(vendor);
            }
            workbook.close();
            return vendors;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }
    }
}
