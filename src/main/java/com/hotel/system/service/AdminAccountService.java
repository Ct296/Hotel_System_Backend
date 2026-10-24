package com.hotel.system.service;

import com.hotel.system.dto.admin.AdminCreateAccountRequest;
import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.*;
import com.hotel.system.repository.*;
import com.hotel.system.util.AppUtils;
import com.hotel.system.util.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminAccountService {
    private final UsersRepository usersRepository;
    private final AccountRepository accountRepository;
    private final ManagerRepository managerRepository;
    private final StaffRepository staffRepository;
    private final AccountStatusRepository accountStatusRepository;
    private final EmailService emailService;

    @Transactional
    public void createAccount(AdminCreateAccountRequest request) {
        // 1. Validate Null an toàn để chống NullPointerException
        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        String pid = request.getPid() != null ? request.getPid().trim() : "CHUA_CAP_NHAT_" + System.currentTimeMillis();
        
        if (usersRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email đã tồn tại trong hệ thống.");
        }
        
        // Chỉ check trùng PID nếu người dùng có nhập PID
        if (!pid.startsWith("CHUA_CAP_NHAT_") && usersRepository.findByPid(pid).isPresent()) {
            throw new IllegalArgumentException("CCCD/CMND đã tồn tại.");
        }

        // Xử lý ngày sinh an toàn
        LocalDate dob = (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) 
                        ? LocalDate.parse(request.getDateOfBirth()) 
                        : LocalDate.of(2000, 1, 1);

        LocalDateTime now = LocalDateTime.now();
        String rawPassword = AppUtils.generateRandomPassword(12);

        Users newUser = new Users();
        newUser.setId(AppUtils.generateId("USR", 10));
        newUser.setFirstName(request.getFirstName() != null ? request.getFirstName().trim() : "Chưa có tên");
        newUser.setLastName(request.getLastName() != null ? request.getLastName().trim() : "");
        newUser.setEmail(email);
        newUser.setPid(pid);
        newUser.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : "");
        newUser.setNationality(request.getNationality() != null ? request.getNationality().trim() : "Việt Nam");
        newUser.setSex(request.getSex() != null ? request.getSex() : Gender.MALE);
        newUser.setDateOfBirth(dob);
        newUser.setRole(request.getRole());
        newUser.setCreateDate(now);
        newUser.setUpdateDate(now);
        newUser.setAvatar(request.getRole() == Role.STAFF ? "default_staff_avatar.jpg" : "default_manager_avatar.jpg");
        
        // SỬA LỖI BUG: Phải gán lại biến để Hibernate quản lý object này
        newUser = usersRepository.saveAndFlush(newUser);

        Account newAccount = new Account();
        newAccount.setUser(newUser); // Lúc này newUser đã là entity được quản lý
        newAccount.setPassword(PasswordUtils.hashPassword(rawPassword));
        
        // SỬA LỖI BUG: Gán lại biến Account
        newAccount = accountRepository.saveAndFlush(newAccount);

        if (request.getRole() == Role.MANAGER) {
            Manager manager = new Manager();
            manager.setUser(newUser);
            String mType = request.getManagerType() != null ? request.getManagerType().trim().toUpperCase() : "GENERAL_MANAGER";
            manager.setTitle(ManagerType.valueOf(mType));
            managerRepository.saveAndFlush(manager);
        } else if (request.getRole() == Role.STAFF) {
            Staff staff = new Staff();
            staff.setUser(newUser);
            LocalDateTime empTime = (request.getEmploymentTime() != null && !request.getEmploymentTime().isBlank()) 
                                    ? LocalDateTime.parse(request.getEmploymentTime()) 
                                    : now;
            staff.setEmploymentTime(empTime);
            staffRepository.saveAndFlush(staff);
        }

        AccountStatus status = new AccountStatus();
        status.setId(AppUtils.generateId("AST", 10));
        status.setName(AccountState.ACTIVE);
        status.setStartTime(now);
        status.setReason("Tài khoản được admin tạo mới");
        status.setAccount(newAccount);
        accountStatusRepository.saveAndFlush(status);

        // Bọc try-catch khi gửi mail. Nếu mail lỗi, tài khoản vẫn được tạo.
        try {
            emailService.sendCreatedAccountEmail(
                    newUser.getEmail(), 
                    AppUtils.buildFullName(newUser.getFirstName(), newUser.getLastName()), 
                    rawPassword, 
                    request.getRole().name()
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi email cấp mật khẩu: " + e.getMessage());
        }
    }

    @Transactional
    public void lockAccount(String userId, Integer durationDays, String reason) {
        Account targetAccount = accountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        closeCurrentOpenStatus(targetAccount);
        LocalDateTime now = LocalDateTime.now();
        boolean indefinite = durationDays == -1 || durationDays == 0;
        
        AccountStatus lockedStatus = new AccountStatus();
        lockedStatus.setId(AppUtils.generateId("AST", 10));
        lockedStatus.setName(AccountState.LOCKED);
        lockedStatus.setStartTime(now);
        lockedStatus.setEndTime(indefinite ? null : now.plusDays(durationDays));
        lockedStatus.setReason(reason == null || reason.isBlank() ? "Khóa tài khoản" : reason);
        lockedStatus.setAccount(targetAccount);
        accountStatusRepository.saveAndFlush(lockedStatus);
    }

    @Transactional
    public void unlockAccount(String userId, String reason) {
        Account targetAccount = accountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        closeCurrentOpenStatus(targetAccount);
        
        AccountStatus activeStatus = new AccountStatus();
        activeStatus.setId(AppUtils.generateId("AST", 10));
        activeStatus.setName(AccountState.ACTIVE);
        activeStatus.setStartTime(LocalDateTime.now());
        activeStatus.setReason(reason == null || reason.isBlank() ? "Admin mở khóa" : reason);
        activeStatus.setAccount(targetAccount);
        accountStatusRepository.saveAndFlush(activeStatus);
    }

    private void closeCurrentOpenStatus(Account account) {
        LocalDateTime now = LocalDateTime.now();
        // Lọc trạng thái mới nhất của đúng 1 User thay vì findAll()
        Optional<AccountStatus> latestStatusOpt = accountStatusRepository.findTopByAccountIdOrderByStartTimeDesc(account.getUser().getId());
        
        if (latestStatusOpt.isPresent()) {
            AccountStatus latestStatus = latestStatusOpt.get();
            if (latestStatus.getEndTime() == null || latestStatus.getEndTime().isAfter(now)) {
                latestStatus.setEndTime(now);
                accountStatusRepository.saveAndFlush(latestStatus);
            }
        }
    }
}