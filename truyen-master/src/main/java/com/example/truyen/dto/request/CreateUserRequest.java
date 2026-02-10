package com.example.truyen.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private String email;
    private String fullname;
    private String phone;
    private String role;
    private MultipartFile avatar;
}