package com.example.truyen.service;

import com.example.truyen.dto.request.LoginRequest;
import com.example.truyen.dto.request.RegisterRequest;
import com.example.truyen.dto.response.AuthResponse;

// Interface AuthService
public interface AuthService {

    // Xác thực người dùng và tạo token JWT
    AuthResponse login(LoginRequest request);

    // Đăng ký tài khoản mới với vai trò mặc định
    String register(RegisterRequest request);

    // Đăng xuất và blacklist token
    String logout(String token);

    // Tạo token đặt lại mật khẩu
    String generateResetToken(String email);

    // Đặt lại mật khẩu bằng token
    void resetPassword(String token, String newPassword);
}