package com.example.truyen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String fullname;
    private String avatar;
    private String email;
    private String role;
    private String phone;
    private boolean isActive;
    private LocalDateTime createdAt;

}
