package com.example.truyen.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.Set;

@Data
public class StoryRequest {
    private String title;
    private Long authorId;
    private String description;
    private String image;
    private String status;
    private Set<Long> categoryIds;
    private MultipartFile coverImage;
}