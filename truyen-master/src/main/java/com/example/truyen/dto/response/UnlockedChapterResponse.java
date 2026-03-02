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
public class UnlockedChapterResponse {
    // Thông tin mở khóa
    private Long accessId;
    private Integer coinsSpent;
    private LocalDateTime unlockedAt;

    // Thông tin chương
    private Long chapterId;
    private Integer chapterNumber;
    private String chapterTitle;
    private Integer coinsPrice;

    // Thông tin truyện
    private Long storyId;
    private String storyTitle;
    private String storyImage;
}
