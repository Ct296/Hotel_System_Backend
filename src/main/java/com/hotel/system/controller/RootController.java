package com.hotel.system.controller;

import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RootController {

    private final UsersRepository usersRepository;

    private Users getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            Optional<Users> userOpt = usersRepository.findByEmail(userDetails.getUsername());
            return userOpt.orElse(null);
        }
        return null;
    }

    @GetMapping("/home-redirect")
    public ResponseEntity<?> resolveHomeRedirect() {
        Users user = getLoggedInUser();

        if (user != null && user.getRole() != null) {
            Role role = user.getRole();
            String redirectPath = switch (role) {
                case ADMIN -> "/admin/dashboard";
                case MANAGER -> "/manager";
                case STAFF -> "/staff/dashboard";
                case CUSTOMER -> "/customer/home";
            };
            return ResponseEntity.ok(Map.of("redirectPath", redirectPath));
        }

        return ResponseEntity.ok(Map.of("redirectPath", "/customer/home"));
    }
}