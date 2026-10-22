package com.hotel.system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Cấu hình CORS cho phép VueJS gọi API
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000") // Các port thường dùng của Vue
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Chỉ giữ lại cấu hình phục vụ file upload (ví dụ: ảnh phòng, avatar)
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath.toString() + "/");
    }
}