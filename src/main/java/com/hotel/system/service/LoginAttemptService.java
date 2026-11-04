package com.hotel.system.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private static final int MAX_ATTEMPT = 5;
    private static final long LOCK_TIME_DURATION = 15 * 60 * 1000; // Khóa 15 phút

    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lockTimeCache = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        if (!lockTimeCache.containsKey(email)) {
            return false;
        }
        long unlockTime = lockTimeCache.get(email);
        if (System.currentTimeMillis() > unlockTime) {
            lockTimeCache.remove(email);
            attemptsCache.remove(email);
            return false;
        }
        return true;
    }

    public void loginFailed(String email) {
        int attempts = attemptsCache.getOrDefault(email, 0) + 1;
        attemptsCache.put(email, attempts);
        if (attempts >= MAX_ATTEMPT) {
            lockTimeCache.put(email, System.currentTimeMillis() + LOCK_TIME_DURATION);
        }
    }

    public void loginSucceeded(String email) {
        attemptsCache.remove(email);
        lockTimeCache.remove(email);
    }
}