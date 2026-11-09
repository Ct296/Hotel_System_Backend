package com.hotel.system.controller;

import com.hotel.system.dto.profile.ChangePasswordRequest;
import com.hotel.system.dto.profile.ProfileUpdateRequest;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.repository.UsersRepository;
import com.hotel.system.service.ProfileService;
import com.hotel.system.repository.AccountStatusRepository;
import com.hotel.system.entity.AccountStatus;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class CommonProfileController {

    private final ProfileService profileService;
    private final UsersRepository usersRepository;
    private final AccountStatusRepository accountStatusRepository;

    // Helper: Lấy User hiện tại từ JWT Security Context, chỉ trả về User có Role khác CUSTOMER
    private Users getLoggedInInternalUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            Optional<Users> userOpt = usersRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isPresent()) {
                Users user = userOpt.get();
                if (user.getRole() != null && user.getRole() != Role.CUSTOMER) {
                    return user;
                }
            }
        }
        return null;
    }

    // API: Lấy thông tin hồ sơ của người dùng hiện tại (ADMIN, MANAGER, STAFF)
    @GetMapping("")
    public ResponseEntity<?> getMyProfile() {
        Users currentUser = getLoggedInInternalUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(currentUser);
    }

    // API: Cập nhật hồ sơ của người dùng hiện tại (ADMIN, MANAGER, STAFF)
    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(@Valid @ModelAttribute ProfileUpdateRequest request, BindingResult bindingResult) {
        Users currentUser = getLoggedInInternalUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            Users updatedUser = profileService.updateProfile(currentUser, request);
            return ResponseEntity.ok(Map.of(
                "message", "Cập nhật hồ sơ thành công.",
                "user", Map.of(
                    "fullName", updatedUser.getFirstName() + " " + updatedUser.getLastName(),
                    "avatar", updatedUser.getAvatar() != null ? updatedUser.getAvatar() : ""
                )
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // API: Đổi mật khẩu của người dùng hiện tại (ADMIN, MANAGER, STAFF)
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, BindingResult bindingResult) {
        Users currentUser = getLoggedInInternalUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            profileService.changePassword(currentUser, request);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // API: Lấy danh sách trạng thái tài khoản của người dùng hiện tại (ADMIN, MANAGER, STAFF)
    @GetMapping("/account-statuses")
    public ResponseEntity<?> getAccountStatuses() {
        Users currentUser = getLoggedInInternalUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        List<AccountStatus> statuses = accountStatusRepository
                .findByAccount_User_IdOrderByStartTimeDesc(currentUser.getId());
        return ResponseEntity.ok(statuses);
    }
}

    // Chặn lỗi upload file quá dung lượng (Trả về JSON)
    @RestControllerAdvice
    class CommonProfileExceptionAdvice {
    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<?> handleUploadSizeException() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "Ảnh tải lên không được vượt quá 2MB."));
    }
}