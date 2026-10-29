package com.hotel.system.controller;

import com.hotel.system.dto.profile.ChangePasswordRequest;
import com.hotel.system.dto.profile.ProfileUpdateRequest;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.repository.UsersRepository;
import com.hotel.system.service.ProfileService;
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

    private Users getLoggedInInternalUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            Optional<Users> userOpt = usersRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isPresent()) {
                Users user = userOpt.get();
                // Chỉ nội bộ mới được dùng API này
                if (user.getRole() != null && user.getRole() != Role.CUSTOMER) {
                    return user;
                }
            }
        }
        return null;
    }


    @GetMapping("")
    public ResponseEntity<?> getMyProfile() {
        Users currentUser = getLoggedInInternalUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        // Trả về full object User (hoặc map ra DTO nếu muốn giấu password)
        return ResponseEntity.ok(currentUser);
    }



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
                    "fullName", updatedUser.getLastName() + " " + updatedUser.getFirstName(),
                    "avatar", updatedUser.getAvatar() != null ? updatedUser.getAvatar() : ""
                )
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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