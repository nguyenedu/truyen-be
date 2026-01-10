package com.example.truyen.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    /**
     * Thêm token vào blacklist
     * @param token JWT token
     * @param expirationTime Thời gian hết hạn (milliseconds)
     */
    public void blacklistToken(String token, long expirationTime) {
        String key = BLACKLIST_PREFIX + token;

        // Tính thời gian còn lại đến khi token hết hạn
        long currentTime = System.currentTimeMillis();
        long ttl = expirationTime - currentTime;

        if (ttl > 0) {
            // Lưu vào Redis với TTL (Time To Live)
            redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
            log.info("Token added to blacklist with TTL: {} ms", ttl);
        }
    }

    /**
     * Kiểm tra token có trong blacklist không
     * @param token JWT token
     * @return true nếu token bị blacklist
     */
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    /**
     * Xóa token khỏi blacklist (thường không cần dùng vì Redis tự xóa khi hết TTL)
     * @param token JWT token
     */
    public void removeFromBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Token removed from blacklist");
    }

    /**
     * Xóa tất cả tokens trong blacklist (dùng cho admin/testing)
     */
    public void clearAllBlacklist() {
        redisTemplate.keys(BLACKLIST_PREFIX + "*")
                .forEach(redisTemplate::delete);
        log.info("All blacklisted tokens cleared");
    }
}