package com.hotel.system.controller;

import com.hotel.system.dto.admin.AdminCreateAccountRequest;
import com.hotel.system.entity.Account;
import com.hotel.system.entity.AccountStatus;
import com.hotel.system.entity.Manager;
import com.hotel.system.entity.Staff;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.AccountState;
import com.hotel.system.entity.enums.ManagerType;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.entity.enums.WorkingStatus;
import com.hotel.system.repository.AccountRepository;
import com.hotel.system.repository.AccountStatusRepository;
import com.hotel.system.repository.CustomerRepository;
import com.hotel.system.repository.ManagerRepository;
import com.hotel.system.repository.StaffRepository;
import com.hotel.system.repository.UsersRepository;
import com.hotel.system.service.AdminAccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminAccountService adminAccountService;
    private final AccountRepository accountRepository;
    private final AccountStatusRepository accountStatusRepository;
    private final UsersRepository usersRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final ManagerRepository managerRepository;

    // Helper: Lấy User hiện tại từ JWT Security Context
    private Users getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            Optional<Users> userOpt = usersRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isPresent() && userOpt.get().getRole() == Role.ADMIN) {
                return userOpt.get();
            }
        }
        return null;
    }

    // Lấy trạng thái khóa mới nhất
    private AccountStatus getEffectiveLatestStatus(String userId) {
        List<AccountStatus> statuses = accountStatusRepository.findAll()
                .stream()
                .filter(status -> status.getAccount() != null
                        && status.getAccount().getId() != null
                        && status.getAccount().getId().equals(userId))
                .sorted(Comparator.comparing(AccountStatus::getStartTime))
                .toList();

        if (statuses.isEmpty()) return null;

        AccountStatus latestStatus = statuses.get(statuses.size() - 1);
        if (latestStatus.getName() == AccountState.LOCKED
                && latestStatus.getEndTime() != null
                && latestStatus.getEndTime().isBefore(LocalDateTime.now())) {
            return null;
        }

        return latestStatus;
    }

    // 1. LẤY DỮ LIỆU DASHBOARD (Danh sách tài khoản)
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDashboardData() {
        Users currentUser = getCurrentAdmin();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập bằng tài khoản quản trị."));
        }

        long lockedAccountsCount = 0;
        
        // TẠO MAP TRẠNG THÁI STAFF
        Map<String, WorkingStatus> staffWorkingStatuses = new HashMap<>();
        for (Staff s : staffRepository.findAll()) {
            staffWorkingStatuses.put(s.getId(), s.getWorkingStatus());
        }

        // Lấy danh sách tài khoản
        List<Account> accounts = accountRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        account -> account.getUser() != null ? account.getUser().getCreateDate() : null,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        List<Map<String, Object>> accountListResponse = new ArrayList<>();

        for (Account account : accounts) {
            if (account.getUser() == null || account.getUser().getId() == null) continue;
            
            String userId = account.getUser().getId();
            AccountStatus effectiveStatus = getEffectiveLatestStatus(userId);
            AccountState state = effectiveStatus != null && effectiveStatus.getName() != null
                    ? effectiveStatus.getName() : AccountState.ACTIVE;

            if (state == AccountState.LOCKED) lockedAccountsCount++;

            Map<String, Object> accInfo = new HashMap<>();
            accInfo.put("userId", userId);
            accInfo.put("email", account.getUser().getEmail());
            accInfo.put("fullName", account.getUser().getFirstName() + " " + account.getUser().getLastName());
            accInfo.put("role", account.getUser().getRole());
            accInfo.put("phoneNumber", account.getUser().getPhoneNumber());
            accInfo.put("pid", account.getUser().getPid());
            accInfo.put("dateOfBirth", account.getUser().getDateOfBirth());
            accInfo.put("sex", account.getUser().getSex());
            accInfo.put("nationality", account.getUser().getNationality());
            accInfo.put("createDate", account.getUser().getCreateDate());

            accInfo.put("state", state);
            accInfo.put("reason", effectiveStatus != null ? effectiveStatus.getReason() : "");
            accInfo.put("unlockTime", effectiveStatus != null ? effectiveStatus.getEndTime() : null);

            if (account.getUser().getRole() == Role.MANAGER) {
                managerRepository.findById(userId).ifPresent(m -> {
                    accInfo.put("workingStatus", m.getWorkingStatus());
                    accInfo.put("managerTitle", m.getTitle().getDisplayName());
                });
            } else if (account.getUser().getRole() == Role.STAFF) {
                accInfo.put("workingStatus", staffWorkingStatuses.get(userId));
            }

            accountListResponse.add(accInfo);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", usersRepository.count());
        response.put("totalCustomers", customerRepository.count());
        response.put("totalStaffs", staffRepository.count());
        response.put("totalManagers", managerRepository.count());
        response.put("lockedAccounts", lockedAccountsCount);
        
        response.put("managerTypes", ManagerType.values());
        response.put("accounts", accountListResponse);

        return ResponseEntity.ok(response);
    }

    // 2. PROFILE ADMIN KHÁC
    @GetMapping("/profile")
    public ResponseEntity<?> adminProfile() {
        Users currentUser = getCurrentAdmin();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", currentUser.getId());
        profile.put("email", currentUser.getEmail());
        profile.put("fullName", currentUser.getFirstName() + " " + currentUser.getLastName());
        profile.put("role", currentUser.getRole());
        return ResponseEntity.ok(profile);
    }

    // 3. TẠO TÀI KHOẢN
    @PostMapping("/accounts/create")
    public ResponseEntity<?> createAccount(@Valid @RequestBody AdminCreateAccountRequest request, BindingResult bindingResult) {
        if (getCurrentAdmin() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }

        try {
            adminAccountService.createAccount(request);
            return ResponseEntity.ok(Map.of("message", "Tạo tài khoản " + request.getRole() + " thành công."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 4. KHÓA TÀI KHOẢN
    @PostMapping("/accounts/{userId}/lock")
    public ResponseEntity<?> lockAccount(@PathVariable String userId, @RequestBody Map<String, Object> payload) {
        Users currentUser = getCurrentAdmin();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (currentUser.getId().equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Admin không thể tự khóa chính mình."));
        }

        try {
            Integer durationDays = payload.containsKey("durationDays") ? Integer.parseInt(payload.get("durationDays").toString()) : -1;
            String reason = (String) payload.getOrDefault("reason", "");
            
            adminAccountService.lockAccount(userId, durationDays, reason);
            return ResponseEntity.ok(Map.of("message", "Đã khóa tài khoản thành công."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 5. MỞ KHÓA TÀI KHOẢN
    @PostMapping("/accounts/{userId}/unlock")
    public ResponseEntity<?> unlockAccount(@PathVariable String userId, @RequestBody Map<String, String> payload) {
        if (getCurrentAdmin() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            String reason = payload.getOrDefault("reason", "");
            adminAccountService.unlockAccount(userId, reason);
            return ResponseEntity.ok(Map.of("message", "Mở khóa tài khoản thành công."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 6. CẬP NHẬT TRẠNG THÁI MANAGER
    @PostMapping("/managers/{managerId}/update-status")
    public ResponseEntity<?> updateManagerWorkingStatus(@PathVariable String managerId, @RequestBody Map<String, String> payload) {
        if (getCurrentAdmin() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Manager manager = managerRepository.findById(managerId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Quản lý với ID: " + managerId));

            String statusStr = payload.get("workingStatus");
            if (statusStr == null) throw new IllegalArgumentException("Thiếu workingStatus");

            WorkingStatus newStatus = WorkingStatus.valueOf(statusStr);
            manager.setWorkingStatus(newStatus);
            managerRepository.save(manager);

            if (newStatus == WorkingStatus.RESIGNED || newStatus == WorkingStatus.SUSPENDED) {
                String reason = (newStatus == WorkingStatus.RESIGNED) ? "Quản lý đã nghỉ việc." : "Quản lý đang bị đình chỉ/nghỉ tạm thời.";
                adminAccountService.lockAccount(manager.getUser().getId(), -1, "Hệ thống tự động khóa: " + reason);
            }

            return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái làm việc của Manager thành công!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

}
