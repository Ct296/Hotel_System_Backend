package com.hotel.system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootRoutingController {

    @GetMapping("/")
    public String root() {
        return "redirect:/customer/home";
    }

    @GetMapping("/router")
    public String routeUserBasedOnRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        // Chuyển hướng theo Role (Spring Security thường gắn prefix ROLE_)
        if (role.contains("ADMIN")) {
            return "redirect:/admin/dashboard";
        } else if (role.contains("MANAGER")) {
            return "redirect:/manager"; // Link mặc định của Manager
        } else if (role.contains("STAFF")) {
            return "redirect:/staff/dashboard"; // Link của Lễ tân
        } else if (role.contains("CUSTOMER")) {
            return "redirect:/customer/home"; // Link của Khách hàng
        }

        return "redirect:/login?error=invalid_role";
    }
}