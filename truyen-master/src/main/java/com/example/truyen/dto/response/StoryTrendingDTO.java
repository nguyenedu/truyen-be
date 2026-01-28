package com.example.truyen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryTrendingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String image;
    private Integer totalViews;
    private Double averageRating;
    private Long favoriteCount;
    private Long commentCount;
    private Double trendingScore;
    private Integer rank;
}