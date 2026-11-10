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
import com.hotel.system.security.SecurityUtils;
import com.hotel.system.service.AdminAccountService;
import com.hotel.system.util.AppUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final SecurityUtils securityUtils;

    // Helper: Lấy trạng thái hiệu lực mới nhất của tài khoản
    private AccountStatus getEffectiveLatestStatus(String userId) {
        List<AccountStatus> statuses = accountStatusRepository.findByAccount_User_IdOrderByStartTimeDesc(userId);
        if (statuses.isEmpty()) return null;

        LocalDateTime now = LocalDateTime.now();
        // Danh sách đã được sắp xếp từ mới nhất đến cũ nhất (Desc)
        for (AccountStatus status : statuses) {
            if (status.getName() == AccountState.ACTIVE) {
                return status;
            }
            if (status.getEndTime() == null || !status.getEndTime().isBefore(now)) {
                return status;
            }
        }
        return null;
    }

    // 1. LẤY DỮ LIỆU DASHBOARD (Danh sách tài khoản)
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDashboardData() {
        Users currentUser = securityUtils.getCurrentUserWithRole(Role.ADMIN);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập bằng tài khoản quản trị."));
        }

        long lockedAccountsCount = 0;
        
        Map<String, WorkingStatus> staffWorkingStatuses = new HashMap<>();
        for (Staff s : staffRepository.findAll()) {
            staffWorkingStatuses.put(s.getId(), s.getWorkingStatus());
        }

        List<Account> accounts = accountRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        account -> account.getUser() != null ? account.getUser().getCreateDate() : null,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        List<Map<String, Object>> accountListResponse = new ArrayList<>();

        for (Account account : accounts) {
            Users accUser = account.getUser();
            if (accUser == null || accUser.getId() == null) continue;
            
            String userId = accUser.getId();
            AccountStatus effectiveStatus = getEffectiveLatestStatus(userId);
            AccountState state = effectiveStatus != null && effectiveStatus.getName() != null
                    ? effectiveStatus.getName() : AccountState.ACTIVE;

            if (state == AccountState.LOCKED) lockedAccountsCount++;

            Map<String, Object> accInfo = new HashMap<>();
            accInfo.put("userId", userId);
            accInfo.put("email", accUser.getEmail());
            accInfo.put("fullName", AppUtils.buildFullName(accUser, "N/A"));
            accInfo.put("role", accUser.getRole());
            accInfo.put("phoneNumber", accUser.getPhoneNumber());
            accInfo.put("pid", accUser.getPid());
            accInfo.put("dateOfBirth", accUser.getDateOfBirth());
            accInfo.put("sex", accUser.getSex());
            accInfo.put("nationality", accUser.getNationality());
            accInfo.put("createDate", accUser.getCreateDate());

            accInfo.put("state", state);
            accInfo.put("reason", effectiveStatus != null ? effectiveStatus.getReason() : "");
            accInfo.put("unlockTime", effectiveStatus != null ? effectiveStatus.getEndTime() : null);

            if (accUser.getRole() == Role.MANAGER) {
                managerRepository.findById(userId).ifPresent(m -> {
                    accInfo.put("workingStatus", m.getWorkingStatus());
                    accInfo.put("managerTitle", m.getTitle().getDisplayName());
                });
            } else if (accUser.getRole() == Role.STAFF) {
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

    // 2. PROFILE ADMIN
    @GetMapping("/profile")
    public ResponseEntity<?> adminProfile() {
        Users currentUser = securityUtils.getCurrentUserWithRole(Role.ADMIN);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", currentUser.getId());
        profile.put("email", currentUser.getEmail());
        profile.put("fullName", AppUtils.buildFullName(currentUser, "N/A"));
        profile.put("role", currentUser.getRole());
        return ResponseEntity.ok(profile);
    }

    // 3. TẠO TÀI KHOẢN
    @PostMapping("/accounts/create")
    public ResponseEntity<?> createAccount(@Valid @RequestBody AdminCreateAccountRequest request, BindingResult bindingResult) {
        if (securityUtils.getCurrentUserWithRole(Role.ADMIN) == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

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

    // 4. CẬP NHẬT TRẠNG THÁI TÀI KHOẢN (LOCKED, ACTIVE, BOOKING_BLOCKED)
    @PutMapping("/accounts/{userId}/status")
    public ResponseEntity<?> updateAccountStatus(@PathVariable String userId, @RequestBody Map<String, Object> payload) {
        Users currentUser = securityUtils.getCurrentUserWithRole(Role.ADMIN);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        if (currentUser.getId().equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Admin không thể tự đổi trạng thái của chính mình."));
        }
        
        try {
            String statusStr = (String) payload.get("status");
            if (statusStr == null || statusStr.isBlank()) {
                throw new IllegalArgumentException("Thiếu trạng thái (status).");
            }
            
            AccountState newState = AccountState.valueOf(statusStr);
            Integer durationDays = payload.containsKey("durationDays") ? Integer.parseInt(payload.get("durationDays").toString()) : -1;
            String reason = (String) payload.getOrDefault("reason", "");
            
            adminAccountService.updateAccountStatus(userId, newState, durationDays, reason);
            return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái tài khoản thành công."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 5. CẬP NHẬT TRẠNG THÁI MANAGER
    @PostMapping("/managers/{managerId}/update-status")
    public ResponseEntity<?> updateManagerWorkingStatus(@PathVariable String managerId, @RequestBody Map<String, String> payload) {
        if (securityUtils.getCurrentUserWithRole(Role.ADMIN) == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

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
                adminAccountService.updateAccountStatus(manager.getUser().getId(), AccountState.LOCKED, -1, "Hệ thống tự động khóa: " + reason);
            }

            return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái làm việc của Manager thành công!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }
}