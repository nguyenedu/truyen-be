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

    // Thêm token vào danh sách đen
    public void blacklistToken(String token, long expirationTime) {
        String key = BLACKLIST_PREFIX + token;

        long currentTime = System.currentTimeMillis();
        long ttl = expirationTime - currentTime;

        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
            log.info("Token added to blacklist with TTL: {} ms", ttl);
        }
    }

    // Kiểm tra token có trong danh sách đen không
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    // Xóa token khỏi danh sách đen
    public void removeFromBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Token removed from blacklist");
    }

    // Xóa tất cả token trong danh sách đen
    public void clearAllBlacklist() {
        redisTemplate.keys(BLACKLIST_PREFIX + "*")
                .forEach(redisTemplate::delete);
        log.info("All blacklisted tokens cleared");
    }
}