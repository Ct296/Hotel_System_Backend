package com.hotel.system.security;

import com.hotel.system.entity.Account;
import com.hotel.system.entity.Users;
import com.hotel.system.repository.AccountRepository;
import com.hotel.system.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;
    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Tìm User theo email
        Users user = usersRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        // Tìm Account để lấy mật khẩu
        Account account = accountRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản cho email: " + email));

        // Lấy quyền (Role)
        String roleName = user.getRole() != null ? user.getRole().name() : "CUSTOMER";

        // Trả về đối tượng UserDetails chuẩn của Spring Security
        return User.withUsername(user.getEmail())
                .password(account.getPassword())
                .authorities(roleName)
                .build();
    }
}