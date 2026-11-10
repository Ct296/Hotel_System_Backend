package com.hotel.system.security;

import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UsersRepository usersRepository;

    public Users getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            return usersRepository.findByEmail(userDetails.getUsername()).orElse(null);
        }
        return null;
    }

    public Users getCurrentUserWithRole(Role expectedRole) {
        Users user = getCurrentUser();
        return (user != null && user.getRole() == expectedRole) ? user : null;
    }

    public Users getCurrentInternalUser() {
        Users user = getCurrentUser();
        return (user != null && user.getRole() != null && user.getRole() != Role.CUSTOMER) ? user : null;
    }
}