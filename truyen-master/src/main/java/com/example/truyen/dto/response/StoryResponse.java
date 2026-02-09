package com.example.truyen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryResponse {
    private Long id;
    private String title;
    private String authorName;
    private Long authorId;
    private String description;
    private String image;
    private String status;
    private Integer totalChapters;
    private Integer totalViews;
    private Boolean isHot;
    private Set<String> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double averageRating;
    private Integer totalRatings;
}