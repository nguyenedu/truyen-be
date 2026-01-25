package com.example.truyen.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải có 3-50 ký tự")
    private String username;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, message = "Password phải có ít nhất 6 ký tự")
    private String password;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String fullname;

    @Size(min = 10, max = 15, message = "Số điện thoại phải có 10-15 ký tự")
    private String phone;

    private String avatar;

    private String role;
}