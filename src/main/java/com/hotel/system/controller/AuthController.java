package com.hotel.system.controller;

import com.hotel.system.entity.*;
import com.hotel.system.entity.enums.AccountState;
import com.hotel.system.entity.enums.Gender;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Random;

@Controller
public class AuthController {

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountStatusRepository accountStatusRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Email, mật khẩu không đúng hoặc tài khoản đang bị khóa!");
        }
        return "auth/login"; 
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("genders", Gender.values());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(
            @ModelAttribute Users userForm,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/register";
        }

        if (usersRepository.findByEmail(userForm.getEmail()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email này đã được sử dụng!");
            return "redirect:/register";
        }

        if (usersRepository.findByPid(userForm.getPid()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Số CCCD/Passport này đã được đăng ký!");
            return "redirect:/register";
        }

        // Tạo ID ngẫu nhiên (hoặc bạn có thể dùng class Utils tạo ID cũ)
        String userId = "USR" + String.format("%07d", new Random().nextInt(10000000));

        // 1. Lưu Users
        userForm.setId(userId);
        userForm.setRole(Role.CUSTOMER);
        userForm.setCreateDate(LocalDateTime.now());
        userForm.setUpdateDate(LocalDateTime.now());
        usersRepository.save(userForm);

        // 2. Lưu Account
        Account account = new Account();
        account.setId(userId);
        account.setUser(userForm);
        account.setPassword(passwordEncoder.encode(password));
        accountRepository.save(account);

        // 3. Lưu AccountStatus (Mở khóa)
        AccountStatus status = new AccountStatus();
        status.setId("AST" + String.format("%07d", new Random().nextInt(10000000)));
        status.setName(AccountState.ACTIVE);
        status.setStartTime(LocalDateTime.now());
        status.setReason("Khởi tạo tài khoản khách hàng");
        status.setAccount(account);
        accountStatusRepository.save(status);

        // 4. Lưu Customer
        Customer customer = new Customer();
        customer.setId(userId);
        customer.setUser(userForm);
        customerRepository.save(customer);

        redirectAttributes.addFlashAttribute("message", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
    }
}