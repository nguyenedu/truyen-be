package com.example.truyen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryTrendingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long storyId; // For frontend compatibility
    private String title;
    private String image;
    private Integer totalViews;
    private Integer totalChapters;
    private String authorName;
    private List<String> categories;
    private Double averageRating;
    private Long favoriteCount;
    private Long commentCount;
    private Double trendingScore;
    private Integer rank;
}