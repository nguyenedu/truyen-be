package com.example.truyen.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {

    private String eventType;
    private Long userId;
    private Long storyId;
    private Long categoryId;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;

    public static AnalyticsEvent forStoryView(Long storyId, Long userId, Long categoryId) {
        return AnalyticsEvent.builder()
                .eventType("STORY_VIEW")
                .storyId(storyId)
                .userId(userId)
                .categoryId(categoryId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static AnalyticsEvent forFavorite(Long storyId, Long userId, Long categoryId) {
        return AnalyticsEvent.builder()
                .eventType("FAVORITE")
                .storyId(storyId)
                .userId(userId)
                .categoryId(categoryId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static AnalyticsEvent forRating(Long storyId, Long userId, Long categoryId, int rating) {
        return AnalyticsEvent.builder()
                .eventType("RATING")
                .storyId(storyId)
                .userId(userId)
                .categoryId(categoryId)
                .metadata(Map.of("rating", rating))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static AnalyticsEvent forComment(Long storyId, Long userId, Long categoryId) {
        return AnalyticsEvent.builder()
                .eventType("COMMENT")
                .storyId(storyId)
                .userId(userId)
                .categoryId(categoryId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
