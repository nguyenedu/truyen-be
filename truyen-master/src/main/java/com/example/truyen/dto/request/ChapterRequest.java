package com.example.truyen.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterRequest {

    @NotNull(message = "Story ID không được để trống")
    private Long storyId;

    @NotNull(message = "Chapter number không được để trống")
    private Integer chapterNumber;

    @NotBlank(message = "Tiêu đề chương không được để trống")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    @Builder.Default
    private Boolean isLocked = false;

    @Min(value = 0, message = "Giá xu phải >= 0")
    @Builder.Default
    private Integer coinsPrice = 0;
}