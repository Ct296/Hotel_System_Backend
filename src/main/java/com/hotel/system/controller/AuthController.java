package com.hotel.system.controller;

import com.hotel.system.dto.auth.RegisterRequest;
import com.hotel.system.entity.Account;
import com.hotel.system.entity.AccountStatus;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.AccountState;
import com.hotel.system.repository.AccountRepository;
import com.hotel.system.repository.AccountStatusRepository;
import com.hotel.system.repository.ManagerRepository;
import com.hotel.system.repository.StaffRepository;
import com.hotel.system.repository.UsersRepository;
import com.hotel.system.security.JwtTokenProvider;
import com.hotel.system.service.AuthService;
import com.hotel.system.service.LoginAttemptService;
import com.hotel.system.util.AppUtils;
import com.hotel.system.util.PasswordUtils;
import com.hotel.system.dto.auth.ForgotPasswordRequest;
import com.hotel.system.dto.auth.ForgotPasswordVerifyOtpRequest;
import com.hotel.system.dto.auth.ResetPasswordRequest;
import org.springframework.validation.BindingResult;
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
    private final ManagerRepository managerRepository;
    private final StaffRepository staffRepository;
    private final LoginAttemptService loginAttemptService;

    @Data
    public static class LoginRequest {
        private String email; 
        private String password;
        @JsonProperty("isEmployee")
        private boolean isEmployee; 
    }

    // 1. ĐĂNG NHẬP
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String trimmedEmail = loginRequest.getEmail().trim().toLowerCase();
        
        if (loginAttemptService.isBlocked(trimmedEmail)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Thông tin đăng nhập không chính xác quá 5 lần. Vui lòng thử lại sau 15 phút."));
        }

        Optional<Users> userOpt = usersRepository.findByEmail(trimmedEmail);
        if (userOpt.isEmpty()) {
            loginAttemptService.loginFailed(trimmedEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email hoặc mật khẩu không đúng!"));
        }
        
        Users user = userOpt.get();
        String roleName = user.getRole() != null ? user.getRole().name() : "CUSTOMER";
        boolean isCustomer = roleName.equals("CUSTOMER");

        if (loginRequest.isEmployee() && isCustomer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tài khoản không có quyền truy cập cổng nội bộ!"));
        }
        if (!loginRequest.isEmployee() && !isCustomer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vui lòng đăng nhập qua cổng dành cho nhân viên!"));
        }

        if (roleName.equals("STAFF")) {
            boolean isResigned = staffRepository.findById(user.getId())
                    .map(staff -> staff.getWorkingStatus() == com.hotel.system.entity.enums.WorkingStatus.RESIGNED)
                    .orElse(false);
            if (isResigned) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tài khoản của bạn đã bị vô hiệu hóa do đã nghỉ việc."));
            }
        } else if (roleName.equals("MANAGER")) {
            boolean isResigned = managerRepository.findById(user.getId())
                    .map(manager -> manager.getWorkingStatus() == com.hotel.system.entity.enums.WorkingStatus.RESIGNED)
                    .orElse(false);
            if (isResigned) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tài khoản của bạn đã bị vô hiệu hóa do đã nghỉ việc."));
            }
        }

        Optional<Account> accountOpt = accountRepository.findById(user.getId());
        if (accountOpt.isEmpty()) {
            loginAttemptService.loginFailed(trimmedEmail);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email hoặc mật khẩu không đúng!"));
        }

        Account account = accountOpt.get();
        if (!PasswordUtils.matches(loginRequest.getPassword(), account.getPassword())) {
            loginAttemptService.loginFailed(trimmedEmail);
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

        loginAttemptService.loginSucceeded(trimmedEmail);

        UserDetails userDetails = User.withUsername(user.getEmail())
                .password(account.getPassword())
                .authorities(roleName)
                .build();
        String token = jwtTokenProvider.generateToken(userDetails);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("email", user.getEmail());
        userMap.put("fullName", AppUtils.buildFullName(user, "N/A"));
        userMap.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");

        if (roleName.equals("MANAGER")) {
            managerRepository.findById(user.getId()).ifPresent(manager -> {
                userMap.put("title", manager.getTitle().name());
            });
        }

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", roleName);
        response.put("user", userMap);

        return ResponseEntity.ok(response);
    }

    // 2. ĐĂNG XUẤT
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
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

    // 6. YÊU CẦU QUÊN MẬT KHẨU (Kiểm tra Email, SĐT, CCCD & Gửi OTP)
    @PostMapping("/forgot-password/request")
    public ResponseEntity<?> requestForgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                   BindingResult bindingResult,
                                                   HttpSession session) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            authService.clearPendingResetPassword(session);
            authService.processForgotPassword(request, session);
            return ResponseEntity.ok(Map.of(
                    "message", "Mã OTP 6 chữ số đã được gửi tới email của bạn (hiệu lực 5 phút).",
                    "email", request.getEmail().trim()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Không thể gửi email OTP lúc này. Vui lòng thử lại sau."));
        }
    }

    // 7. XÁC THỰC MÃ OTP QUÊN MẬT KHẨU
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyForgotPasswordOtp(@Valid @RequestBody ForgotPasswordVerifyOtpRequest request,
                                                     BindingResult bindingResult,
                                                     HttpSession session) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            authService.verifyForgotPasswordOtp(request.getOtp().trim(), session);
            return ResponseEntity.ok(Map.of("message", "Xác thực OTP thành công. Vui lòng nhập mật khẩu mới."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 8. ĐẶT LẠI MẬT KHẨU MỚI
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetForgotPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                 BindingResult bindingResult,
                                                 HttpSession session) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }
        try {
            authService.resetPassword(request, session);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 9. GỬI LẠI OTP QUÊN MẬT KHẨU
    @PostMapping("/forgot-password/resend-otp")
    public ResponseEntity<?> resendForgotPasswordOtp(HttpSession session) {
        try {
            authService.resendForgotPasswordOtp(session);
            return ResponseEntity.ok(Map.of("message", "Mã OTP mới đã được gửi vào email của bạn."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Không thể gửi lại mã OTP. Vui lòng thử lại sau."));
        }
    }
}