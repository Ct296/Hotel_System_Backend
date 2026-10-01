package com.hotel.system.controller;

import com.hotel.system.entity.Account;
import com.hotel.system.entity.AccountStatus;
import com.hotel.system.entity.enums.AccountState;
import com.hotel.system.repository.AccountRepository;
import com.hotel.system.repository.AccountStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Controller
@RequestMapping("/admin/accounts")
public class AccountPenaltyController {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountStatusRepository accountStatusRepository;

    @PostMapping("/{userId}/lock")
    public String lockAccount(
            @PathVariable("userId") String userId,
            @RequestParam("reason") String reason,
            @RequestParam(value = "lockDays", defaultValue = "30") int lockDays,
            RedirectAttributes redirectAttributes,
            @RequestHeader(value = "Referer", required = false) String referer) {

        Optional<Account> accountOpt = accountRepository.findById(userId);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();

            AccountStatus status = new AccountStatus();
            status.setId("AST" + String.format("%07d", new Random().nextInt(10000000)));
            status.setName(AccountState.LOCKED);
            status.setStartTime(LocalDateTime.now());
            if (lockDays > 0) {
                status.setEndTime(LocalDateTime.now().plusDays(lockDays));
            } 
            status.setReason(reason);
            status.setAccount(account);

            accountStatusRepository.save(status);
            redirectAttributes.addFlashAttribute("message", "Đã khóa tài khoản thành công!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản!");
        }

        return "redirect:" + (referer != null ? referer : "/admin/dashboard");
    }

    @PostMapping("/{userId}/unlock")
    public String unlockAccount(
            @PathVariable("userId") String userId,
            @RequestParam(value = "reason", defaultValue = "Mở khóa tài khoản trước hạn") String reason,
            RedirectAttributes redirectAttributes,
            @RequestHeader(value = "Referer", required = false) String referer) {

        Optional<Account> accountOpt = accountRepository.findById(userId);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();

            AccountStatus status = new AccountStatus();
            status.setId("AST" + String.format("%07d", new Random().nextInt(10000000)));
            status.setName(AccountState.ACTIVE);
            status.setStartTime(LocalDateTime.now());
            status.setReason(reason);
            status.setAccount(account);

            accountStatusRepository.save(status);
            redirectAttributes.addFlashAttribute("message", "Đã mở khóa tài khoản!");
        }

        return "redirect:" + (referer != null ? referer : "/admin/dashboard");
    }
}