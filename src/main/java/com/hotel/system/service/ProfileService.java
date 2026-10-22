package com.hotel.system.service;

import com.hotel.system.dto.profile.ChangePasswordRequest;
import com.hotel.system.dto.profile.ProfileUpdateRequest;
import com.hotel.system.entity.Account;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.Gender;
import com.hotel.system.repository.AccountRepository;
import com.hotel.system.repository.MediaStorageDirectory;
import com.hotel.system.repository.UsersRepository;
import com.hotel.system.util.PasswordUtils;
import com.hotel.system.util.StoredMedia;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UsersRepository usersRepository;
    private final AccountRepository accountRepository;
    private final MediaStorageService mediaStorageService;

    @Transactional
    public Users updateProfile(Users currentUser, ProfileUpdateRequest request) {
        Users user = usersRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));

        LocalDate dob = LocalDate.parse(request.getDateOfBirth());
        if (dob.isBefore(LocalDate.of(1900, 1, 1)) || dob.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh phải từ năm 1900 đến hiện tại.");
        }

        Optional<Users> existingPid = usersRepository.findByPid(request.getPid().trim());
        if (existingPid.isPresent() && !existingPid.get().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("CCCD/CMND/Hộ chiếu đã được đăng ký bởi tài khoản khác.");
        }

        user.setLastName(request.getLastName().trim());
        user.setFirstName(request.getFirstName().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setPid(request.getPid().trim());
        user.setNationality(request.getNationality().trim());
        user.setSex(Gender.valueOf(request.getSex().trim().toUpperCase()));
        user.setDateOfBirth(dob);

        if (request.getAvatarFile() != null && !request.getAvatarFile().isEmpty()) {
            String previousAvatar = user.getAvatar();
            StoredMedia storedMedia = mediaStorageService.storeImage(request.getAvatarFile(), MediaStorageDirectory.AVATARS, user.getId());
            user.setAvatar(storedMedia.publicPath());
            
            if (previousAvatar != null && !previousAvatar.equals(storedMedia.publicPath())) {
                mediaStorageService.deleteByPublicPath(previousAvatar);
            }
        }

        user.setUpdateDate(LocalDateTime.now());
        return usersRepository.save(user);
    }

    @Transactional
    public void changePassword(Users currentUser, ChangePasswordRequest request) {
        Account account = accountRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        if (!PasswordUtils.matches(request.getOldPass(), account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }
        if (!request.getNewPass().equals(request.getConfirmPass())) {
            throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp.");
        }
        if (PasswordUtils.matches(request.getNewPass(), account.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        account.setPassword(PasswordUtils.hashPassword(request.getNewPass()));
        accountRepository.save(account);
    }
}