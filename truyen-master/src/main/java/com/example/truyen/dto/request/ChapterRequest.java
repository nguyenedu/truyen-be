package com.example.truyen.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterRequest {

    @NotNull(message = "Story ID không được để trống")
    private Long storyId;

    @NotNull(message = "Chapter number không được để trống")
    private Integer chapterNumber;

    @NotBlank(message = "Tiêu đề chương không được để trống")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;
}