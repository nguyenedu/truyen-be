package com.example.truyen.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO cho Activity Log Event gửi qua Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogEvent {

    private String action;
    private String entityType;
    private Long entityId;
    private String entityName;
    private Long userId;
    private String username;
    private String details;
    private LocalDateTime timestamp;

    /**
     * Tạo ActivityLogEvent
     */
    public static ActivityLogEvent create(String action, String entityType, Long entityId,
            String entityName, Long userId, String username, String details) {
        return ActivityLogEvent.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .userId(userId)
                .username(username)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
