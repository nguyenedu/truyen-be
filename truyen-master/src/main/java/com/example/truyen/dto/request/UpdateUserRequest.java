package com.example.truyen.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
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

    @Size(min = 10, max = 15, message = "Số điện thoại phải có 10-15 ký tự")
    private String phone;

    private Boolean isActive;


    @Size(min = 6, message = "Password phải có ít nhất 6 ký tự")
    private String password;
}