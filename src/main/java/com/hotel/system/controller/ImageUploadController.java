package com.hotel.system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageUploadController {

    @PostMapping("/upload/{type}")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, @PathVariable("type") String type) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn một file."));
        }

        try {
            Path uploadDir = Paths.get("uploads", type);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : "";
            String newFileName = UUID.randomUUID().toString() + extension;

            Path filePath = uploadDir.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath);

            String fileUrl = "/uploads/" + type + "/" + newFileName;
            
            return ResponseEntity.ok(Map.of("url", fileUrl));
            
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi khi lưu file trên server."));
        }
    }
}