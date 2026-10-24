package com.hotel.system.config;

import com.hotel.system.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
            .cors(cors -> cors.configure(http)) 
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 1. Mở cửa cho request OPTIONS (tránh lỗi CORS chặn đứng từ vòng gửi xe)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 2. Mở cửa cho endpoint báo lỗi của Spring Boot (để hiện đúng 404 thay vì 403)
                .requestMatchers("/error").permitAll()
                
                // 3. Các API công khai (đã gộp cho gọn)
                .requestMatchers("/api/auth/**", "/api/public/**", "/uploads/**").permitAll()
                
                // 4. Phân quyền API theo Role
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/manager/**").hasAnyAuthority("MANAGER", "ADMIN")
                .requestMatchers("/api/staff/**").hasAnyAuthority("STAFF", "MANAGER", "ADMIN")
                .requestMatchers("/api/customer/**").hasAuthority("CUSTOMER")
                
                // 5. Tất cả các request khác phải có Token hợp lệ
                .anyRequest().authenticated()
            )
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