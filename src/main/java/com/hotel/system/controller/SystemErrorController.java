package com.hotel.system.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SystemErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        int statusCode = 500;
        String errorMsg = "Đã xảy ra lỗi hệ thống không xác định.";

        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
            
            // Xử lý các mã lỗi phổ biến trả về JSON cho Vue
            if (statusCode == 404) {
                errorMsg = "Không tìm thấy API hoặc endpoint bạn yêu cầu (404 Not Found).";
            } else if (statusCode == 401) {
                errorMsg = "Bạn chưa đăng nhập hoặc Token không hợp lệ (401 Unauthorized).";
            } else if (statusCode == 403) {
                errorMsg = "Bạn không có quyền truy cập vào tài nguyên này (403 Forbidden).";
            } else if (statusCode == 405) {
                errorMsg = "Phương thức HTTP không được hệ thống hỗ trợ (405 Method Not Allowed).";
            } else if (statusCode == 500) {
                errorMsg = "Lỗi máy chủ nội bộ. Vui lòng thử lại sau (500 Internal Server Error).";
            } else if (message != null && !message.toString().isEmpty()) {
                errorMsg = message.toString();
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", statusCode);
        response.put("error", errorMsg);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(statusCode).body(response);
    }
}