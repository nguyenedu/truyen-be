package com.example.truyen.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleRequest {

    @NotNull(message = "User ID không được để trống")
    private Long userId;

    @NotNull(message = "Role không được để trống")
    private String role;
}
