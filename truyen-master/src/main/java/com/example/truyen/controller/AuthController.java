package com.example.truyen.controller;

import com.example.truyen.dto.request.LoginRequest;
import com.example.truyen.dto.request.RegisterRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.AuthResponse;
import com.example.truyen.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Xác thực người dùng và trả về token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authService.login(request)));
    }

    /**
     * Đăng ký người dùng mới.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request), null));
    }

    /**
     * Đăng xuất người dùng hiện tại bằng cách hủy token.
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return ResponseEntity.ok(ApiResponse.success(authService.logout(bearerToken.substring(7)), null));
        }

        return ResponseEntity.badRequest().body(ApiResponse.error("Token không hợp lệ"));
    }
}