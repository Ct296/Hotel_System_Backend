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
        if (usersRepository.findByEmail(request.getEmail().trim()).isPresent()) {
            throw new IllegalArgumentException("Email đã tồn tại trong hệ thống.");
        }
        if (usersRepository.findByPid(request.getPid().trim()).isPresent()) {
            throw new IllegalArgumentException("CCCD/CMND đã tồn tại.");
        }

        LocalDate dob = LocalDate.parse(request.getDateOfBirth());
        LocalDateTime now = LocalDateTime.now();
        String rawPassword = AppUtils.generateRandomPassword(12);

        Users newUser = new Users();
        newUser.setId(AppUtils.generateId("USR", 10));
        newUser.setFirstName(request.getFirstName().trim());
        newUser.setLastName(request.getLastName().trim());
        newUser.setEmail(request.getEmail().trim());
        newUser.setPid(request.getPid().trim());
        newUser.setPhoneNumber(request.getPhoneNumber().trim());
        newUser.setNationality(request.getNationality().trim());
        newUser.setSex(request.getSex());
        newUser.setDateOfBirth(dob);
        newUser.setRole(request.getRole());
        newUser.setCreateDate(now);
        newUser.setUpdateDate(now);
        newUser.setAvatar(request.getRole() == Role.STAFF ? "default_staff_avatar.jpg" : "default_manager_avatar.jpg");
        usersRepository.saveAndFlush(newUser);

        Account newAccount = new Account();
        newAccount.setUser(newUser);
        newAccount.setPassword(PasswordUtils.hashPassword(rawPassword));
        accountRepository.saveAndFlush(newAccount);

        if (request.getRole() == Role.MANAGER) {
            Manager manager = new Manager();
            manager.setUser(newUser);
            manager.setTitle(ManagerType.valueOf(request.getManagerType().trim().toUpperCase()));
            managerRepository.saveAndFlush(manager);
        } else if (request.getRole() == Role.STAFF) {
            Staff staff = new Staff();
            staff.setUser(newUser);
            staff.setEmploymentTime(LocalDateTime.parse(request.getEmploymentTime()));
            staffRepository.saveAndFlush(staff);
        }

        AccountStatus status = new AccountStatus();
        status.setId(AppUtils.generateId("AST", 10));
        status.setName(AccountState.ACTIVE);
        status.setStartTime(now);
        status.setReason("Tài khoản được admin tạo mới");
        status.setAccount(newAccount);
        accountStatusRepository.saveAndFlush(status);

        emailService.sendCreatedAccountEmail(
                newUser.getEmail(), 
                AppUtils.buildFullName(newUser.getFirstName(), newUser.getLastName()), 
                rawPassword, 
                request.getRole().name()
        );
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
        lockedStatus.setReason(reason == null ? "Khóa tài khoản" : reason);
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
        accountStatusRepository.findAll().stream()
                .filter(s -> s.getAccount().getId().equals(account.getId()) && (s.getEndTime() == null || s.getEndTime().isAfter(now)))
                .forEach(s -> {
                    s.setEndTime(now);
                    accountStatusRepository.save(s);
                });
    }
}