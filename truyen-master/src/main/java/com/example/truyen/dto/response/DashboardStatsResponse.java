package com.example.truyen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    private Long totalStories;
    private Long totalAuthors;
    private Long totalUsers;
    private Long totalViews;

    private StatsComparison storiesComparison;
    private StatsComparison authorsComparison;
    private StatsComparison usersComparison;
    private StatsComparison viewsComparison;

    private List<TopStoryDTO> topStoriesByViews;

    private List<TopAuthorDTO> topAuthorsByStories;

    private List<ChartDataPoint> newStoriesChart;

    private List<ChartDataPoint> newUsersChart;

    private List<RecentActivityDTO> recentActivities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatsComparison {
        private Long currentValue;
        private Long previousValue;
        private Double changePercent;
        private String trend; // "up" hoặc "down"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopStoryDTO {
        private Long id;
        private String title;
        private String authorName;
        private Integer totalViews;
        private String status;
        private String image;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopAuthorDTO {
        private Long id;
        private String name;
        private Long totalStories;
        private String avatar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChartDataPoint {
        private String date;
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentActivityDTO {
        private Long id;
        private String username;
        private String action;
        private String tableName;
        private String target;
        private String timeAgo;
    }
}