package com.hotel.system.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("mediaViewSupport")
public class MediaViewSupport {

    private static final String DEFAULT_AVATAR_PATH = "/image/default_avatar_customer.jpg";
    private static final String DEFAULT_ROOM_PATH = "/image/default_room.jpg";
    private static final String DEFAULT_SERVICE_PATH = "/image/default_service.jpg";

    public String resolveAvatarPath(String avatarPath) {
        return resolveMediaPath(avatarPath, DEFAULT_AVATAR_PATH);
    }

    public String resolveRoomImagePath(String imagePath) {
        return resolveMediaPath(imagePath, DEFAULT_ROOM_PATH);
    }

    public String resolveServiceImagePath(String imagePath) {
        return resolveMediaPath(imagePath, DEFAULT_SERVICE_PATH);
    }

    public String getDefaultAvatarPath() {
        return DEFAULT_AVATAR_PATH;
    }

    public String getDefaultRoomPath() {
        return DEFAULT_ROOM_PATH;
    }

    public String getDefaultServicePath() {
        return DEFAULT_SERVICE_PATH;
    }

    public String resolveMediaPath(String mediaPath, String defaultPath) {
        if (!StringUtils.hasText(mediaPath)) {
            return defaultPath;
        }

        String trimmed = mediaPath.trim();
        if (trimmed.startsWith("/uploads/") || trimmed.startsWith("/image/") || isExternalUrl(trimmed)) {
            return trimmed;
        }

        return "/image/" + trimmed;
    }

    private boolean isExternalUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
