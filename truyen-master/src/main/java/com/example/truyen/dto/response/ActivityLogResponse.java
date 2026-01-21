package com.example.truyen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String tableName;
    private Integer recordId;
    private String description;
    private String ipAddress;
    private LocalDateTime createdAt;
}