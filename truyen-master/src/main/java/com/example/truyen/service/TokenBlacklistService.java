package com.example.truyen.service;

// Interface TokenBlacklistService
public interface TokenBlacklistService {

    // Blacklist một JWT token
    void blacklistToken(String token, long expirationTimeMillis);

    // Kiểm tra token có bị blacklist không
    boolean isTokenBlacklisted(String token);

    // Xóa token khỏi blacklist
    void removeFromBlacklist(String token);

    // Xóa toàn bộ blacklist
    void clearAllBlacklist();
}