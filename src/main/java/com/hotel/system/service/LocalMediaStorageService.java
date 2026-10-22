package com.hotel.system.service;

import com.hotel.system.repository.MediaStorageDirectory;
import com.hotel.system.util.StoredMedia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalMediaStorageService implements MediaStorageService {

    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public StoredMedia storeImage(MultipartFile file, MediaStorageDirectory directory, String entityKey) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh trống.");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Ảnh không được vượt quá 2MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        if (!StringUtils.hasText(extension)) {
            extension = "png";
        }
        extension = extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ.");
        }

        try {
            if (!hasValidImageSignature(file)) {
                throw new IllegalArgumentException("Nội dung file không phải ảnh hợp lệ.");
            }

            String safeEntityKey = StringUtils.hasText(entityKey)
                    ? entityKey.replaceAll("[^A-Za-z0-9_-]", "")
                    : UUID.randomUUID().toString().replace("-", "");

            Path targetDirectory = Paths.get(uploadDir, directory.getFolderName()).toAbsolutePath().normalize();
            Files.createDirectories(targetDirectory);

            String fileName = safeEntityKey + "_" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path targetPath = targetDirectory.resolve(fileName).normalize();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String publicPath = "/uploads/" + directory.getFolderName() + "/" + fileName;
            return new StoredMedia(publicPath, fileName, targetPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể lưu ảnh.", ex);
        }
    }

    @Override
    public void deleteByPublicPath(String publicPath) {
        if (!StringUtils.hasText(publicPath)) {
            return;
        }

        String normalized = publicPath.trim();
        if (!normalized.startsWith("/uploads/")) {
            return;
        }

        String relativePath = normalized.substring("/uploads/".length());
        Path targetPath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(relativePath).normalize();
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();

        if (!targetPath.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException ignored) {
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }

    private boolean hasValidImageSignature(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);
            if (header.length < 4) {
                return false;
            }
            return isJpeg(header) || isPng(header) || isWebp(header);
        }
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        return header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && (header[4] & 0xFF) == 0x0D
                && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A
                && (header[7] & 0xFF) == 0x0A;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50;
    }
}