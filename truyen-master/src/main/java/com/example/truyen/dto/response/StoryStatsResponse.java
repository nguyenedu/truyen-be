package com.example.truyen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryStatsResponse {

    private Long storyId;
    private String title;
    private String image;
    private Long viewsToday;
    private Long uniqueViewersToday;
    private Long views7Days;
    private Long views30Days;
    private Integer totalViews;
    private Double averageRating;
    private Long favoriteCount;
    private Long commentCount;
    private Double trendingScore;
    private Integer currentRank;
}