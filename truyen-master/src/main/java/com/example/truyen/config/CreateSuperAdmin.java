package com.example.truyen.config;

// This class is created when the web is newly created, so there is no SuperAdmin to manage it

import com.example.truyen.entity.User;
import com.example.truyen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateSuperAdmin implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        boolean hasSuperAdmin = userRepository.findAll().stream()
                .anyMatch(user -> user.getRole().equals(User.Role.SUPER_ADMIN));
        if (!hasSuperAdmin) {
            User superAdmin = User.builder()
                    .username("superadmin")
                    .email("superadmin@gmail.com")
                    .password(passwordEncoder.encode("123456"))
                    .fullname("Super Admin")
                    .role(User.Role.SUPER_ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(superAdmin);
            log.info("Super Admin account created successfully");
            log.info("username: superadmin");
            log.info("password: 123456");
        } else {
            log.info("SUPER_ADMIN account already exists");
        }
    }
}
