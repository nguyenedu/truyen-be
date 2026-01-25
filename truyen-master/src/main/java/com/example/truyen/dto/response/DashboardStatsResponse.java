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

    // Thống kê tổng quan
    private Long totalStories;
    private Long totalAuthors;
    private Long totalUsers;
    private Long totalViews;

    // Thống kê so sánh với kỳ trước
    private StatsComparison storiesComparison;
    private StatsComparison authorsComparison;
    private StatsComparison usersComparison;
    private StatsComparison viewsComparison;

    // Top truyện nhiều view nhất
    private List<TopStoryDTO> topStoriesByViews;

    // Top tác giả nhiều truyện nhất
    private List<TopAuthorDTO> topAuthorsByStories;

    // Biểu đồ truyện mới theo ngày
    private List<ChartDataPoint> newStoriesChart;

    // Biểu đồ user đăng ký mới theo ngày
    private List<ChartDataPoint> newUsersChart;

    // Recent activities
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
        private String date; // yyyy-MM-dd hoặc label như "T2", "T3"
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentActivityDTO {
        private Long id;
        private String username;
        private String action; // CREATE, UPDATE, DELETE
        private String tableName;
        private String target;
        private String timeAgo;
    }
}