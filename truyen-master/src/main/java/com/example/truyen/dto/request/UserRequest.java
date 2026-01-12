package com.example.truyen.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    private String fullname;

    private String avatar;

    private String email;

    private String phone;

    private boolean is_active;

}
