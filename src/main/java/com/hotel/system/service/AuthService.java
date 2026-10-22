package com.hotel.system.service;

import com.hotel.system.dto.auth.RegisterRequest;
import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.*;
import com.hotel.system.repository.*;
import com.hotel.system.util.AppUtils;
import com.hotel.system.util.PasswordUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsersRepository usersRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountStatusRepository accountStatusRepository;
    private final TierCustomerRepository tierCustomerRepository;
    private final TierHistoryRepository tierHistoryRepository;
    private final EmailService emailService;

    public static final String PENDING_REGISTER_REQUEST = "pendingRegisterRequest";
    public static final String PENDING_REGISTER_OTP = "pendingRegisterOtp";
    public static final String PENDING_REGISTER_OTP_EXPIRE_AT = "pendingRegisterOtpExpireAt";
    public static final String PENDING_REGISTER_OTP_ATTEMPTS = "pendingRegisterOtpAttempts";
    public static final int REGISTER_OTP_EXPIRE_MINUTES = 5;
    public static final int REGISTER_OTP_MAX_ATTEMPTS = 5;

    public void validateRegistration(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Xác nhận mật khẩu không khớp!");
        }
        LocalDate dob = LocalDate.parse(request.getDateOfBirth());
        if (dob.isBefore(LocalDate.of(1900, 1, 1)) || dob.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh phải từ năm 1900 đến hiện tại!");
        }
        if (usersRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã tồn tại!");
        }
        if (usersRepository.findByPid(request.getPid()).isPresent()) {
            throw new IllegalArgumentException("Số CCCD/CMND đã được đăng ký!");
        }
        if (usersRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký!");
        }
        if (tierCustomerRepository.findAll().isEmpty()) {
            throw new IllegalArgumentException("Hệ thống chưa cấu hình hạng khách hàng mặc định.");
        }
    }

    public void processRegistration(RegisterRequest request, HttpSession session) {
        validateRegistration(request);
        
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(REGISTER_OTP_EXPIRE_MINUTES);

        session.setAttribute(PENDING_REGISTER_REQUEST, request);
        session.setAttribute(PENDING_REGISTER_OTP, otpCode);
        session.setAttribute(PENDING_REGISTER_OTP_EXPIRE_AT, expireAt);
        session.setAttribute(PENDING_REGISTER_OTP_ATTEMPTS, 0);

        emailService.sendRegistrationOtpEmail(
                request.getEmail(),
                AppUtils.buildFullName(request.getFirstName(), request.getLastName()),
                otpCode,
                REGISTER_OTP_EXPIRE_MINUTES
        );
    }

    @Transactional
    public void verifyOtpAndCreateUser(String enteredOtp, HttpSession session) {
        String storedOtp = (String) session.getAttribute(PENDING_REGISTER_OTP);
        LocalDateTime expireAt = (LocalDateTime) session.getAttribute(PENDING_REGISTER_OTP_EXPIRE_AT);
        Integer attempts = (Integer) session.getAttribute(PENDING_REGISTER_OTP_ATTEMPTS);
        RegisterRequest request = (RegisterRequest) session.getAttribute(PENDING_REGISTER_REQUEST);

        if (enteredOtp == null || !enteredOtp.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("Mã OTP phải đúng 6 chữ số.");
        }
        if (expireAt == null || LocalDateTime.now().isAfter(expireAt)) {
            clearPendingRegistration(session);
            throw new IllegalStateException("Mã xác thực đã hết hạn. Vui lòng đăng ký lại.");
        }

        int nextAttempts = attempts == null ? 1 : attempts + 1;
        session.setAttribute(PENDING_REGISTER_OTP_ATTEMPTS, nextAttempts);

        if (!enteredOtp.equals(storedOtp)) {
            if (nextAttempts >= REGISTER_OTP_MAX_ATTEMPTS) {
                clearPendingRegistration(session);
                throw new IllegalStateException("Bạn đã nhập sai mã xác thực quá số lần cho phép. Vui lòng đăng ký lại.");
            }
            throw new IllegalArgumentException("Mã xác thực không đúng. Bạn còn " + (REGISTER_OTP_MAX_ATTEMPTS - nextAttempts) + " lần thử.");
        }

        // Validate lại lần cuối trước khi lưu
        validateRegistration(request);

        LocalDateTime now = LocalDateTime.now();
        List<TierCustomer> tiers = tierCustomerRepository.findAll();
        TierCustomer defaultTier = tiers.stream().filter(t -> t.getCondition() != null)
                .min(Comparator.comparing(TierCustomer::getCondition)).orElse(tiers.get(0));

        // 1. Lưu User
        Users newUser = new Users();
        newUser.setId(AppUtils.generateId("USR", 10));
        newUser.setFirstName(request.getFirstName().trim());
        newUser.setLastName(request.getLastName().trim());
        newUser.setEmail(request.getEmail().trim().toLowerCase());
        newUser.setPid(request.getPid().trim());
        newUser.setPhoneNumber(request.getPhoneNumber().trim());
        newUser.setNationality(request.getNationality().trim());
        newUser.setSex(Gender.valueOf(request.getSex().trim().toUpperCase()));
        newUser.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
        newUser.setRole(Role.CUSTOMER);
        newUser.setCreateDate(now);
        newUser.setUpdateDate(now);
        newUser.setAvatar("default_avatar_customer.jpg");
        usersRepository.saveAndFlush(newUser);

        // 2. Lưu Account
        Account newAccount = new Account();
        newAccount.setUser(newUser);
        newAccount.setPassword(PasswordUtils.hashPassword(request.getPassword()));
        accountRepository.saveAndFlush(newAccount);

        // 3. Lưu Customer
        Customer newCustomer = new Customer();
        newCustomer.setUser(newUser);
        customerRepository.saveAndFlush(newCustomer);

        // 4. Lưu AccountStatus
        AccountStatus accountStatus = new AccountStatus();
        accountStatus.setId(AppUtils.generateId("AST", 10));
        accountStatus.setName(AccountState.ACTIVE);
        accountStatus.setStartTime(now);
        accountStatus.setReason("Tài khoản kích hoạt sau khi xác thực email");
        accountStatus.setAccount(newAccount);
        accountStatusRepository.saveAndFlush(accountStatus);

        // 5. Lưu TierHistory
        TierHistory tierHistory = new TierHistory();
        tierHistory.setId(AppUtils.generateId("THI", 10));
        tierHistory.setStartDate(now);
        tierHistory.setTotalSpending(0.0);
        tierHistory.setReason("Khởi tạo hạng mặc định khi đăng ký");
        tierHistory.setCustomer(newCustomer);
        tierHistory.setTierCustomer(defaultTier);
        tierHistoryRepository.saveAndFlush(tierHistory);

        clearPendingRegistration(session);
    }

    public void clearPendingRegistration(HttpSession session) {
        session.removeAttribute(PENDING_REGISTER_REQUEST);
        session.removeAttribute(PENDING_REGISTER_OTP);
        session.removeAttribute(PENDING_REGISTER_OTP_EXPIRE_AT);
        session.removeAttribute(PENDING_REGISTER_OTP_ATTEMPTS);
    }
    
    public boolean hasPendingRegistration(HttpSession session) {
        return session.getAttribute(PENDING_REGISTER_REQUEST) != null
                && session.getAttribute(PENDING_REGISTER_OTP) != null;
    }
}