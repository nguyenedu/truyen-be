package com.example.truyen.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    private Long storyId;

    private Long chapterId;

    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content;
}