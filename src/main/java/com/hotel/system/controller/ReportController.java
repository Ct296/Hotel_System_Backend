package com.hotel.system.controller;

import com.hotel.system.repository.BillRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/report")
public class ReportController {

    private final BillRepository billRepository;

    public ReportController(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    // 1. API LẤY DỮ LIỆU BIỂU ĐỒ (DASHBOARD)
    @GetMapping("/dashboard")
    public ResponseEntity<?> reportDashboard(@RequestParam(defaultValue = "2026") int year) {
        List<Object[]> revenueData = billRepository.sumRevenueByMonth(year);
        
        // Chuẩn bị mảng 12 tháng (mặc định là 0) để truyền xuống Chart.js bên Vue
        List<Double> monthlyRevenues = new ArrayList<>();
        for (int i = 0; i < 12; i++) monthlyRevenues.add(0.0);

        double totalYearRevenue = 0;
        for (Object[] row : revenueData) {
            int monthIndex = ((Number) row[0]).intValue() - 1; // Index từ 0-11
            double amount = ((Number) row[1]).doubleValue();
            monthlyRevenues.set(monthIndex, amount);
            totalYearRevenue += amount;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("selectedYear", year);
        response.put("totalYearRevenue", totalYearRevenue);
        response.put("monthlyRevenues", monthlyRevenues); // Data cho biểu đồ

        return ResponseEntity.ok(response); 
    }

    // 2. API XUẤT FILE EXCEL
    @GetMapping("/export-excel")
    public void exportToExcel(@RequestParam(defaultValue = "2026") int year, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Bao_Cao_Doanh_Thu_" + year + ".xlsx";
        response.setHeader(headerKey, headerValue);

        List<Object[]> revenueData = billRepository.sumRevenueByMonth(year);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Doanh Thu " + year);
            
            // Định dạng Header
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            headerRow.createCell(0).setCellValue("Tháng");
            headerRow.createCell(1).setCellValue("Tổng Doanh Thu (VNĐ)");
            headerRow.getCell(0).setCellStyle(headerStyle);
            headerRow.getCell(1).setCellStyle(headerStyle);

            // Đổ dữ liệu
            int rowIdx = 1;
            double total = 0;
            for (Object[] row : revenueData) {
                Row dataRow = sheet.createRow(rowIdx++);
                dataRow.createCell(0).setCellValue("Tháng " + ((Number) row[0]).intValue());
                
                double amount = ((Number) row[1]).doubleValue();
                dataRow.createCell(1).setCellValue(amount);
                total += amount;
            }

            // Dòng tổng cộng
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.createCell(0).setCellValue("TỔNG CỘNG");
            totalRow.createCell(1).setCellValue(total);
            totalRow.getCell(0).setCellStyle(headerStyle);
            totalRow.getCell(1).setCellStyle(headerStyle);

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            workbook.write(response.getOutputStream());
        }
    }
}