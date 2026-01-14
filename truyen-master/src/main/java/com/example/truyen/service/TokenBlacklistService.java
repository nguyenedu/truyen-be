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

    public void blacklistToken(String token, long expirationTime) {
        String key = BLACKLIST_PREFIX + token;

        long currentTime = System.currentTimeMillis();
        long ttl = expirationTime - currentTime;

        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
            log.info("Token added to blacklist with TTL: {} ms", ttl);
        }
    }


    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }


    public void removeFromBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Token removed from blacklist");
    }


    public void clearAllBlacklist() {
        redisTemplate.keys(BLACKLIST_PREFIX + "*")
                .forEach(redisTemplate::delete);
        log.info("All blacklisted tokens cleared");
    }
}