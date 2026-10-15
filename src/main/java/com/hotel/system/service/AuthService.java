package com.hotel.system.service;

import com.hotel.system.dto.auth.JwtResponse;
import com.hotel.system.dto.auth.LoginRequest;
import com.hotel.system.entity.Account;
import com.hotel.system.entity.Users;
import com.hotel.system.repository.AccountRepository;
import com.hotel.system.repository.UsersRepository;
import com.hotel.system.util.JwtUtils;
import com.hotel.system.util.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JwtUtils jwtUtils;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        // 1. Tìm user theo email
        Users user = usersRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống."));

        // 2. Lấy Account chứa mật khẩu (ánh xạ 1-1 với Users qua ID)
        Account account = accountRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại."));

        // 3. Kiểm tra mật khẩu bằng PasswordUtils
        if (!PasswordUtils.matches(loginRequest.getPassword(), account.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác.");
        }

        // 4. Nếu đúng, tạo JWT Token
        String token = jwtUtils.generateJwtToken(user.getEmail());

        // 5. Trả về Token và thông tin cơ bản
        return new JwtResponse(token, user.getEmail(), user.getRole().name());
    }
}