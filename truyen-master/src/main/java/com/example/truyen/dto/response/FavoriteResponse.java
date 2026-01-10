package com.example.truyen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
}