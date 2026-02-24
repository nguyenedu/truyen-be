package com.example.truyen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryRatingInfoResponse {
    private Long storyId;
    private String storyTitle;
    private Double averageRating;
    private Long totalRatings;
}
