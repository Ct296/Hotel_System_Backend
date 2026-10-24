package com.hotel.system.controller;

import com.hotel.system.dto.auth.RegisterRequest;
import com.hotel.system.entity.Account;
import com.hotel.system.entity.AccountStatus;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.AccountState;
import com.hotel.system.repository.AccountRepository;
import com.hotel.system.repository.AccountStatusRepository;
import com.hotel.system.repository.UsersRepository;
import com.hotel.system.security.JwtTokenProvider;
import com.hotel.system.service.AuthService;
import com.hotel.system.util.PasswordUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UsersRepository usersRepository;
    private final AccountRepository accountRepository;
    private final AccountStatusRepository accountStatusRepository;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Data
    public static class LoginRequest {
        private String email; 
        private String password;
        @JsonProperty("isEmployee")
        private boolean isEmployee; 
    }

    // 1. ĐĂNG NHẬP (Chung cho Khách và Nhân viên)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String trimmedEmail = loginRequest.getEmail().trim().toLowerCase();
        
        Optional<Users> userOpt = usersRepository.findByEmail(trimmedEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email hoặc mật khẩu không đúng!"));
        }
        
        Users user = userOpt.get();
        String roleName = user.getRole() != null ? user.getRole().name() : "CUSTOMER";
        boolean isCustomer = roleName.equals("CUSTOMER");

        // Chặn truy cập chéo cổng
        if (loginRequest.isEmployee() && isCustomer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tài khoản không có quyền truy cập cổng nội bộ!"));
        }
        if (!loginRequest.isEmployee() && !isCustomer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vui lòng đăng nhập qua cổng dành cho nhân viên!"));
        }

        Optional<Account> accountOpt = accountRepository.findById(user.getId());
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email hoặc mật khẩu không đúng!"));
        }

        Account account = accountOpt.get();
        if (!PasswordUtils.matches(loginRequest.getPassword(), account.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email hoặc mật khẩu không đúng!"));
        }

        Optional<AccountStatus> latestStatusOpt = accountStatusRepository
                .findTopByAccountIdOrderByStartTimeDesc(account.getUser().getId());
                
        if (latestStatusOpt.isPresent() && latestStatusOpt.get().getName() == AccountState.LOCKED) {
            String lockReason = latestStatusOpt.get().getReason();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Tài khoản hiện đang bị khóa." + (lockReason != null && !lockReason.isBlank() ? " Lý do: " + lockReason : "")
            ));
        }

        if (PasswordUtils.isLegacyPlainText(account.getPassword())) {
            account.setPassword(PasswordUtils.hashPassword(loginRequest.getPassword()));
            accountRepository.save(account);
        }

        // Tạo JWT Token
        UserDetails userDetails = User.withUsername(user.getEmail())
                .password(account.getPassword())
                .authorities(roleName)
                .build();
        String token = jwtTokenProvider.generateToken(userDetails);

        // Trả về JSON cho Vue lưu vào LocalStorage/Cookie
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", roleName);
        response.put("user", Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "fullName", user.getLastName() + " " + user.getFirstName(),
            "avatar", user.getAvatar() != null ? user.getAvatar() : ""
        ));

        return ResponseEntity.ok(response);
    }

    // 2. ĐĂNG XUẤT
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Với JWT (Stateless), Backend không cần xóa Session.
        // Vue sẽ tự đảm nhận việc xóa Token ở phía Client.
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

    // 3. ĐĂNG KÝ
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request, HttpSession session) {
        try {
            authService.clearPendingRegistration(session);
            authService.processRegistration(request, session);
            return ResponseEntity.ok(Map.of(
                "message", "Mã xác thực đã được gửi tới email của bạn.",
                "email", request.getEmail()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 4. XÁC THỰC OTP ĐĂNG KÝ
    @PostMapping("/register/verify")
    public ResponseEntity<?> verifyRegisterOtp(@RequestBody Map<String, String> payload, HttpSession session) {
        String otp = payload.get("otp");
        if (!authService.hasPendingRegistration(session)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phiên xác thực đã hết hạn. Vui lòng đăng ký lại."));
        }

        try {
            authService.verifyOtpAndCreateUser(otp.trim(), session);
            return ResponseEntity.ok(Map.of("message", "Đăng ký thành công! Vui lòng đăng nhập."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 5. GỬI LẠI OTP
    @PostMapping("/register/resend-otp")
    public ResponseEntity<?> resendRegisterOtp(HttpSession session) {
        if (!authService.hasPendingRegistration(session)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phiên xác thực đã hết hạn. Vui lòng đăng ký lại."));
        }
        RegisterRequest request = (RegisterRequest) session.getAttribute(AuthService.PENDING_REGISTER_REQUEST);
        try {
            authService.processRegistration(request, session);
            return ResponseEntity.ok(Map.of("message", "Mã xác thực mới đã được gửi tới email của bạn."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Không thể gửi lại mã xác thực. Vui lòng thử lại sau."));
        }
    }
}