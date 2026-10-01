package com.hotel.system.controller;

import com.hotel.system.entity.Account;
import com.hotel.system.entity.Users;
import com.hotel.system.repository.AccountRepository;
import com.hotel.system.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class UserProfileController {

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // Nếu bạn có MediaStorageService trong project cũ, hãy uncomment:
    // @Autowired
    // private MediaStorageService mediaStorageService;

    @PostMapping("/update")
    public String updateProfile(
            @ModelAttribute Users userForm,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            Principal principal,
            RedirectAttributes redirectAttributes,
            @RequestHeader(value = "Referer", required = false) String referer) {
        
        Optional<Users> userOpt = usersRepository.findByEmail(principal.getName());
        if (userOpt.isPresent()) {
            Users existingUser = userOpt.get();
            existingUser.setFirstName(userForm.getFirstName());
            existingUser.setLastName(userForm.getLastName());
            existingUser.setPhoneNumber(userForm.getPhoneNumber());
            existingUser.setPid(userForm.getPid());
            existingUser.setNationality(userForm.getNationality());
            existingUser.setSex(userForm.getSex());
            existingUser.setDateOfBirth(userForm.getDateOfBirth());
            existingUser.setUpdateDate(LocalDateTime.now());

            // Lưu ảnh Avatar (Bỏ comment nếu dùng)
            /*
            if (avatarFile != null && !avatarFile.isEmpty()) {
                String avatarPath = mediaStorageService.storeFile(avatarFile, "avatars");
                existingUser.setAvatar(avatarPath);
            }
            */

            usersRepository.save(existingUser);
            redirectAttributes.addFlashAttribute("message", "Cập nhật hồ sơ thành công!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Lỗi dữ liệu người dùng!");
        }

        // Tự động quay lại trang trước đó
        return "redirect:" + (referer != null ? referer : "/router");
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam("oldPass") String oldPass,
            @RequestParam("newPass") String newPass,
            @RequestParam("confirmPass") String confirmPass,
            Principal principal,
            RedirectAttributes redirectAttributes,
            @RequestHeader(value = "Referer", required = false) String referer) {
        
        if (!newPass.equals(confirmPass)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:" + (referer != null ? referer : "/router");
        }

        Optional<Users> userOpt = usersRepository.findByEmail(principal.getName());
        if (userOpt.isPresent()) {
            Account account = accountRepository.findById(userOpt.get().getId()).orElse(null);
            if (account != null && passwordEncoder.matches(oldPass, account.getPassword())) {
                account.setPassword(passwordEncoder.encode(newPass));
                accountRepository.save(account);
                redirectAttributes.addFlashAttribute("message", "Đổi mật khẩu thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu hiện tại không đúng!");
            }
        }

        return "redirect:" + (referer != null ? referer : "/router");
    }
}