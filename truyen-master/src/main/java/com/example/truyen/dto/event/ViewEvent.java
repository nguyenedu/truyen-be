package com.example.truyen.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewEvent {

    private Long storyId;
    private Long userId;
    private String ipAddress;
    private String sessionId;
    private LocalDateTime timestamp;

    public static ViewEvent create(Long storyId, Long userId, String ipAddress, String sessionId) {
        return ViewEvent.builder()
                .storyId(storyId)
                .userId(userId)
                .ipAddress(ipAddress)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
