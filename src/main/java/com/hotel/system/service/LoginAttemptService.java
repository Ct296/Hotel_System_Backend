package com.hotel.system.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPT = 5;
    private static final long LOCK_TIME_DURATION = 15 * 60 * 1000L;
    private static final int MAX_CACHE_SIZE = 10_000;

    private final Map<String, AttemptInfo> cache = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        String key = normalizeKey(email);
        if (key.isEmpty()) return false;

        AttemptInfo info = cache.get(key);
        if (info == null || info.unlockTime == 0L) {
            return false;
        }

        if (System.currentTimeMillis() > info.unlockTime) {
            cache.remove(key, info);
            return false;
        }
        return true;
    }

    public void loginFailed(String email) {
        String key = normalizeKey(email);
        if (key.isEmpty()) return;

        // Chống spam email ngẫu nhiên làm phình RAM
        if (cache.size() >= MAX_CACHE_SIZE && !cache.containsKey(key)) {
            cleanupExpiredEntries();
            if (cache.size() >= MAX_CACHE_SIZE) {
                return;
            }
        }

        long now = System.currentTimeMillis();
        cache.compute(key, (k, existing) -> {
            // Nếu chưa có hoặc lần sai trước đã quá 15 phút -> reset đếm lại từ 1
            if (existing == null || (now - existing.lastAttemptTime) > LOCK_TIME_DURATION) {
                return new AttemptInfo(1, now, 0L);
            }
            // Nếu đang bị khóa và chưa hết hạn -> giữ nguyên mốc mở khóa
            if (existing.unlockTime > now) {
                return existing;
            }
            int nextAttempts = existing.attempts + 1;
            long unlockTime = nextAttempts >= MAX_ATTEMPT ? (now + LOCK_TIME_DURATION) : 0L;
            return new AttemptInfo(nextAttempts, now, unlockTime);
        });
    }

    public void loginSucceeded(String email) {
        String key = normalizeKey(email);
        if (!key.isEmpty()) {
            cache.remove(key);
        }
    }

    // Tự động dọn các bản ghi hết hạn sau mỗi 15 phút
    @Scheduled(fixedRate = LOCK_TIME_DURATION)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> {
            AttemptInfo info = entry.getValue();
            boolean lockExpired = info.unlockTime > 0L && now > info.unlockTime;
            boolean idleExpired = info.unlockTime == 0L && (now - info.lastAttemptTime) > LOCK_TIME_DURATION;
            return lockExpired || idleExpired;
        });
    }

    private String normalizeKey(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static class AttemptInfo {
        final int attempts;
        final long lastAttemptTime;
        final long unlockTime;

        AttemptInfo(int attempts, long lastAttemptTime, long unlockTime) {
            this.attempts = attempts;
            this.lastAttemptTime = lastAttemptTime;
            this.unlockTime = unlockTime;
        }
    }
}