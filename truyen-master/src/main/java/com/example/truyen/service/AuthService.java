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
     * Authenticate user and generate JWT token.
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("User does not exist"));

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name());
    }

    /**
     * Register a new user with default role.
     */
    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
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

        return "Registration successful";
    }

    /**
     * Logout user by blacklisting the current JWT token.
     */
    @Transactional
    public String logout(String token) {
        try {
            long expirationTime = jwtTokenProvider.getExpirationTimeMillis(token);
            tokenBlacklistService.blacklistToken(token, expirationTime);
            return "Logout successful";
        } catch (Exception e) {
            throw new BadRequestException("Invalid token");
        }
    }
}