package com.example.truyen.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoryRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotNull(message = "Author ID không được để trống")
    private Long authorId;

    private String description;

    private String image;

    private String status;

    private Set<Long> categoryIds;
}