package com.hotel.system.controller;

import com.hotel.system.entity.Manager;
import com.hotel.system.entity.Users;
import com.hotel.system.entity.enums.Role;
import com.hotel.system.repository.ManagerRepository;
import com.hotel.system.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final UsersRepository usersRepository;
    private final ManagerRepository managerRepository;

    private Users getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            return usersRepository.findByEmail(userDetails.getUsername()).orElse(null);
        }
        return null;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> managerProfile() {
        Users currentUser = getLoggedInUser();
        if (currentUser == null || (currentUser.getRole() != Role.MANAGER && currentUser.getRole() != Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", currentUser.getId());
        profile.put("email", currentUser.getEmail());
        profile.put("fullName", currentUser.getLastName() + " " + currentUser.getFirstName());
        profile.put("role", currentUser.getRole());

        if (currentUser.getRole() == Role.MANAGER) {
            Manager manager = managerRepository.findById(currentUser.getId()).orElse(null);
            if (manager != null) {
                profile.put("managerType", manager.getTitle());
                profile.put("workingStatus", manager.getWorkingStatus());
            }
        }
        return ResponseEntity.ok(profile);
    }
}