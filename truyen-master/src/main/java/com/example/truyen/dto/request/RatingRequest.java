package com.example.truyen.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingRequest {

    @NotNull(message = "Story ID không được để trống")
    private Long storyId;

    @NotNull(message = "Rating không được null")
    @Min(value = 1, message = "Rating phải từ 1-5 sao")
    @Max(value = 5, message = "Rating phải từ 1-5 sao")
    private Integer rating;

    private String review;
}