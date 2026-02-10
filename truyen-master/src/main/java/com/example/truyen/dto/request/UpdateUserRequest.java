package com.example.truyen.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateUserRequest {
    private String fullname;
    private String email;
    private String phone;
    private String password;
    private Boolean isActive;
    private MultipartFile avatar;
}