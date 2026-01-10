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
public class ChapterResponse {
    private Long id;
    private Long storyId;
    private String storyTitle;
    private Integer chapterNumber;
    private String title;
    private String content;
    private Integer views;
    private LocalDateTime createdAt;
}