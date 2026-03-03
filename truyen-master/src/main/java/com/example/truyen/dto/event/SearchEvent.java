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
public class SearchEvent {

    private String query;

    private Long userId;

    private Integer resultCount;

    private Long clickedStoryId;

    private Long categoryId;

    private LocalDateTime timestamp;

    public static SearchEvent create(String query, Long userId, Integer resultCount) {
        return SearchEvent.builder()
                .query(query)
                .userId(userId)
                .resultCount(resultCount)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SearchEvent createWithClick(String query, Long userId, Long clickedStoryId) {
        return SearchEvent.builder()
                .query(query)
                .userId(userId)
                .clickedStoryId(clickedStoryId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
