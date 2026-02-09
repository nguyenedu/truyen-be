package com.example.truyen.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event DTO cho search analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchEvent {

    /**
     * Từ khóa tìm kiếm
     */
    private String query;

    /**
     * User ID (null nếu anonymous)
     */
    private Long userId;

    /**
     * Số lượng kết quả trả về
     */
    private Integer resultCount;

    /**
     * Story ID mà user đã click (null nếu chưa click)
     */
    private Long clickedStoryId;

    /**
     * Category ID được filter (null nếu không filter)
     */
    private Long categoryId;

    /**
     * Timestamp
     */
    private LocalDateTime timestamp;

    /**
     * Factory method để tạo search event
     */
    public static SearchEvent create(String query, Long userId, Integer resultCount) {
        return SearchEvent.builder()
                .query(query)
                .userId(userId)
                .resultCount(resultCount)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method với click tracking
     */
    public static SearchEvent createWithClick(String query, Long userId, Long clickedStoryId) {
        return SearchEvent.builder()
                .query(query)
                .userId(userId)
                .clickedStoryId(clickedStoryId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
