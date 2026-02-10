package com.example.truyen.service.impl;

import com.example.truyen.config.RedisKeyConstants;
import com.example.truyen.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Blacklist một JWT token
    @Override
    public void blacklistToken(String token, long expirationTimeMillis) {
        long ttl = expirationTimeMillis - System.currentTimeMillis();
        if (ttl > 0) {
            String key = RedisKeyConstants.TOKEN_BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
        }
    }

    // Kiểm tra token có bị blacklist không
    @Override
    public boolean isTokenBlacklisted(String token) {
        String key = RedisKeyConstants.TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // Xóa token khỏi blacklist
    @Override
    public void removeFromBlacklist(String token) {
        String key = RedisKeyConstants.TOKEN_BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
    }

    // Xóa toàn bộ blacklist
    @Override
    public void clearAllBlacklist() {
        Set<String> keys = redisTemplate.keys(RedisKeyConstants.TOKEN_BLACKLIST_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
