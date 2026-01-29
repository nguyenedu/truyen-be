package com.example.truyen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long storyId;
    private String storyTitle;
    private String storyImage;
    private String authorName;
    private List<String> categories;
    private Integer totalViews;
    private Integer totalChapters;
    private LocalDateTime createdAt;
}