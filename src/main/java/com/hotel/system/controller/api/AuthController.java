package com.hotel.system.controller.api;

import com.hotel.system.dto.auth.JwtResponse;
import com.hotel.system.dto.auth.LoginRequest;
import com.hotel.system.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse response = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Trả về lỗi 401 Unauthorized nếu sai thông tin
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}