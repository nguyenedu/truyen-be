package com.example.truyen.service;

import com.example.truyen.dto.request.LoginRequest;
import com.example.truyen.dto.request.RegisterRequest;
import com.example.truyen.dto.response.AuthResponse;
import com.example.truyen.entity.User;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.repository.UserRepository;
import com.example.truyen.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Xác thực người dùng và tạo mã thông báo JWT.
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Người dùng không tồn tại"));

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name());
    }

    /**
     * Đăng ký người dùng mới với vai trò mặc định.
     */
    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã tồn tại");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullname(request.getFullname())
                .role(User.Role.USER)
                .isActive(true)
                .build();

        userRepository.save(user);

        return "Đăng ký thành công";
    }

    /**
     * Đăng xuất người dùng bằng cách vô hiệu hóa mã thông báo JWT hiện tại.
     */
    @Transactional
    public String logout(String token) {
        try {
            long expirationTime = jwtTokenProvider.getExpirationTimeMillis(token);
            tokenBlacklistService.blacklistToken(token, expirationTime);
            return "Đăng xuất thành công";
        } catch (Exception e) {
            throw new BadRequestException("Mã thông báo không hợp lệ");
        }
    }
}