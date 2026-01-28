package com.example.truyen.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackViewRequest {

    @NotNull(message = "Story ID is required")
    private Long storyId;

    private Long userId;
    private Long chapterId;
}