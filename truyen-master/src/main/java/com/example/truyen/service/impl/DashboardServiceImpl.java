package com.example.truyen.service.impl;

import com.example.truyen.dto.response.DashboardStatsResponse;
import com.example.truyen.dto.response.DashboardStatsResponse.*;
import com.example.truyen.entity.ActivityLog;
import com.example.truyen.entity.Author;
import com.example.truyen.entity.User;
import com.example.truyen.repository.*;
import com.example.truyen.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final AuthorRepository authorRepository;
    private final ActivityLogRepository activityLogRepository;
    private final StoryViewRepository storyViewRepository;

    // Lấy toàn bộ thống kê dashboard
    @Transactional(readOnly = true)
    @Override
    public DashboardStatsResponse getDashboardStats(String period) {
        var stats = new DashboardStatsResponse();

        var now = LocalDateTime.now();
        LocalDateTime currentPeriodStart;
        LocalDateTime previousPeriodStart;
        LocalDateTime previousPeriodEnd;

        if ("week".equalsIgnoreCase(period)) {
            currentPeriodStart = now.minusDays(7);
            previousPeriodStart = now.minusDays(14);
            previousPeriodEnd = now.minusDays(7);
        } else {
            currentPeriodStart = now.minusDays(30);
            previousPeriodStart = now.minusDays(60);
            previousPeriodEnd = now.minusDays(30);
        }

        // Tổng quan (dùng COUNT/SUM trên DB thay vì findAll)
        stats.setTotalStories(storyRepository.count());
        stats.setTotalAuthors(authorRepository.count());
        stats.setTotalUsers(userRepository.count());
        stats.setTotalViews(storyRepository.sumTotalViews());

        // So sánh với kỳ trước (dùng COUNT query trên DB)
        stats.setStoriesComparison(compareStories(currentPeriodStart, previousPeriodStart, previousPeriodEnd));
        stats.setAuthorsComparison(compareAuthors(currentPeriodStart, previousPeriodStart, previousPeriodEnd));
        stats.setUsersComparison(compareUsers(currentPeriodStart, previousPeriodStart, previousPeriodEnd));
        stats.setViewsComparison(compareViews(currentPeriodStart, previousPeriodStart, previousPeriodEnd));

        // Top stories/authors (dùng JPQL query với JOIN FETCH)
        stats.setTopStoriesByViews(getTopStoriesByViews());
        stats.setTopAuthorsByStories(getTopAuthorsByStories());

        // Biểu đồ (1 query GROUP BY DATE thay vì 30x findAll)
        stats.setNewStoriesChart(getNewStoriesChart(period));
        stats.setNewUsersChart(getNewUsersChart(period));

        // Hoạt động gần đây
        stats.setRecentActivities(getRecentActivities());

        return stats;
    }

    // So sánh story: 2 COUNT queries thay vì 2x findAll()
    private StatsComparison compareStories(LocalDateTime currentStart, LocalDateTime prevStart, LocalDateTime prevEnd) {
        long currentCount = storyRepository.countByCreatedAtAfter(currentStart);
        long previousCount = storyRepository.countByCreatedAtBetween(prevStart, prevEnd);
        return calculateComparison(currentCount, previousCount);
    }

    // So sánh author: 2 COUNT queries thay vì 2x findAll()
    private StatsComparison compareAuthors(LocalDateTime currentStart, LocalDateTime prevStart, LocalDateTime prevEnd) {
        long currentCount = authorRepository.countByCreatedAtAfter(currentStart);
        long previousCount = authorRepository.countByCreatedAtBetween(prevStart, prevEnd);
        return calculateComparison(currentCount, previousCount);
    }

    // So sánh user: 2 COUNT queries thay vì 2x findAll()
    private StatsComparison compareUsers(LocalDateTime currentStart, LocalDateTime prevStart, LocalDateTime prevEnd) {
        long currentCount = userRepository.countByCreatedAtAfter(currentStart);
        long previousCount = userRepository.countByCreatedAtBetween(prevStart, prevEnd);
        return calculateComparison(currentCount, previousCount);
    }

    // So sánh views: dùng StoryViewRepository thay vì dữ liệu giả
    private StatsComparison compareViews(LocalDateTime currentStart, LocalDateTime prevStart, LocalDateTime prevEnd) {
        long currentViews = storyViewRepository.countByViewedAtAfter(currentStart);
        long previousViews = storyViewRepository.countByViewedAtBetween(prevStart, prevEnd);
        return calculateComparison(currentViews, previousViews);
    }

    private StatsComparison calculateComparison(long current, long previous) {
        double changePercent = 0.0;
        if (previous > 0) {
            changePercent = ((double) (current - previous) / previous) * 100;
        }

        String trend = changePercent >= 0 ? "up" : "down";

        return StatsComparison.builder()
                .currentValue(current)
                .previousValue(previous)
                .changePercent(Math.abs(changePercent))
                .trend(trend)
                .build();
    }

    // Top stories: 1 query với JOIN FETCH thay vì findAll + N+1
    private List<TopStoryDTO> getTopStoriesByViews() {
        return storyRepository.findTopByViewsWithAuthor(PageRequest.of(0, 10))
                .stream()
                .map(story -> TopStoryDTO.builder()
                        .id(story.getId())
                        .title(story.getTitle())
                        .authorName(story.getAuthor() != null ? story.getAuthor().getName() : "N/A")
                        .totalViews(story.getTotalViews())
                        .status(story.getStatus().name())
                        .image(story.getImage())
                        .build())
                .collect(Collectors.toList());
    }

    // Top authors: 1 JPQL GROUP BY query thay vì findAll + groupBy trên Java
    private List<TopAuthorDTO> getTopAuthorsByStories() {
        return storyRepository.findTopAuthorsByStoryCount(PageRequest.of(0, 10))
                .stream()
                .map(row -> {
                    Author author = (Author) row[0];
                    Long storyCount = (Long) row[1];
                    return TopAuthorDTO.builder()
                            .id(author.getId())
                            .name(author.getName())
                            .totalStories(storyCount)
                            .avatar(author.getAvatar())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // Biểu đồ story mới: 1 query GROUP BY DATE thay vì 30x findAll()
    private List<ChartDataPoint> getNewStoriesChart(String period) {
        int days = "week".equalsIgnoreCase(period) ? 7 : 30;
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(days - 1).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        // 1 query lấy tất cả ngày có data
        var dbResults = storyRepository.countStoriesByDateRange(start, end);
        Map<String, Long> dateCountMap = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Object[] row : dbResults) {
            String date = row[0].toString();
            Long count = (Long) row[1];
            dateCountMap.put(date, count);
        }

        // Điền đủ tất cả các ngày (kể cả ngày không có data = 0)
        List<ChartDataPoint> chartData = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String dateStr = today.minusDays(i).format(formatter);
            chartData.add(ChartDataPoint.builder()
                    .date(dateStr)
                    .count(dateCountMap.getOrDefault(dateStr, 0L))
                    .build());
        }

        return chartData;
    }

    // Biểu đồ user mới: 1 query GROUP BY DATE thay vì 30x findAll()
    private List<ChartDataPoint> getNewUsersChart(String period) {
        int days = "week".equalsIgnoreCase(period) ? 7 : 30;
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(days - 1).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        // 1 query lấy tất cả ngày có data
        var dbResults = userRepository.countUsersByDateRange(start, end);
        Map<String, Long> dateCountMap = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Object[] row : dbResults) {
            String date = row[0].toString();
            Long count = (Long) row[1];
            dateCountMap.put(date, count);
        }

        // Điền đủ tất cả các ngày
        List<ChartDataPoint> chartData = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String dateStr = today.minusDays(i).format(formatter);
            chartData.add(ChartDataPoint.builder()
                    .date(dateStr)
                    .count(dateCountMap.getOrDefault(dateStr, 0L))
                    .build());
        }

        return chartData;
    }

    private List<RecentActivityDTO> getRecentActivities() {
        List<ActivityLog> activities = activityLogRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

        return activities.stream()
                .map(activity -> {

                    Long userId = activity.getUserId();
                    String username = "System";

                    if (userId != null) {
                        username = userRepository.findById(userId)
                                .map(User::getUsername)
                                .orElse("Unknown");
                    }

                    return RecentActivityDTO.builder()
                            .id(activity.getId())
                            .username(username)
                            .action(activity.getAction())
                            .tableName(activity.getTableName())
                            .target(activity.getTableName() + " #" + activity.getRecordId())
                            .timeAgo(getTimeAgo(activity.getCreatedAt()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1)
            return "Just now";
        if (minutes < 60)
            return minutes + " minutes ago";
        if (hours < 24)
            return hours + " hours ago";
        if (days < 30)
            return days + " days ago";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
