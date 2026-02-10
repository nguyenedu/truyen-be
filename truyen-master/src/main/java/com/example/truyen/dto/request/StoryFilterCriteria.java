package com.example.truyen.dto.request;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StoryFilterCriteria {
    private String keyword;
    private Long authorId;
    private String status;
    private Integer minChapters;
    private Integer maxChapters;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<Long> categoryIds;
    private int page;
    private int size;
    private String sort;
}
