package com.hotel.system.config;

import com.hotel.system.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configure(http)) // Sử dụng cấu hình CORS từ WebConfig
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Các API cho phép truy cập công khai
                .requestMatchers("/api/auth/**", "/uploads/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                
                // Phân quyền API theo Role
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/manager/**").hasAnyAuthority("MANAGER", "ADMIN")
                .requestMatchers("/api/staff/**").hasAnyAuthority("STAFF", "MANAGER", "ADMIN")
                .requestMatchers("/api/customer/**").hasAuthority("CUSTOMER")
                
                // Tất cả các request khác phải có Token hợp lệ
                .anyRequest().authenticated()
            )
            // Thêm Filter kiểm tra JWT trước bộ lọc mật khẩu mặc định
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}