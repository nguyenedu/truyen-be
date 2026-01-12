package com.example.truyen.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private String fullname;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String avatar;

    private String phone;

    private Boolean isActive;
}