package com.example.truyen.service;

import com.example.truyen.dto.response.DashboardStatsResponse;
import com.example.truyen.dto.response.DashboardStatsResponse.*;
import com.example.truyen.entity.ActivityLog;
import com.example.truyen.entity.Author;
import com.example.truyen.entity.Story;
import com.example.truyen.entity.User;
import com.example.truyen.repository.*;
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
public class DashboardService {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final AuthorRepository authorRepository;
    private final ActivityLogRepository activityLogRepository;

    // Lấy toàn bộ thống kê dashboard
    @Transactional(readOnly = true)
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

        // Tổng quan
        stats.setTotalStories(storyRepository.count());
        stats.setTotalAuthors(authorRepository.count());
        stats.setTotalUsers(userRepository.count());
        stats.setTotalViews(getTotalViews());

        // So sánh với kỳ trước
        stats.setStoriesComparison(compareStories(currentPeriodStart, previousPeriodStart, previousPeriodEnd));
        stats.setAuthorsComparison(compareAuthors(currentPeriodStart, previousPeriodStart, previousPeriodEnd));
        stats.setUsersComparison(compareUsers(currentPeriodStart, previousPeriodStart, previousPeriodEnd));
        stats.setViewsComparison(compareViews(currentPeriodStart, previousPeriodStart, previousPeriodEnd));

        // Người dùng/Tác giả hàng đầu
        stats.setTopStoriesByViews(getTopStoriesByViews());
        stats.setTopAuthorsByStories(getTopAuthorsByStories());

        // Biểu đồ
        stats.setNewStoriesChart(getNewStoriesChart(period));
        stats.setNewUsersChart(getNewUsersChart(period));

        // Hoạt động gần đây
        stats.setRecentActivities(getRecentActivities());

        return stats;
    }

    private Long getTotalViews() {
        return storyRepository.findAll().stream()
                .mapToLong(story -> story.getTotalViews() != null ? story.getTotalViews() : 0L)
                .sum();
    }

    private StatsComparison compareStories(LocalDateTime currentStart, LocalDateTime prevStart, LocalDateTime prevEnd) {
        long currentCount = storyRepository.findAll().stream()
                .filter(s -> s.getCreatedAt().isAfter(currentStart))
                .count();

        long previousCount = storyRepository.findAll().stream()
                .filter(s -> s.getCreatedAt().isAfter(prevStart) && s.getCreatedAt().isBefore(prevEnd))
                .count();

        return calculateComparison(currentCount, previousCount);
    }

    private StatsComparison compareAuthors(LocalDateTime currentStart, LocalDateTime prevStart, LocalDateTime prevEnd) {
        long currentCount = authorRepository.findAll().stream()
                .filter(a -> a.getCreatedAt().isAfter(currentStart))
                .count();

        long previousCount = authorRepository.findAll().stream()
                .filter(a -> a.getCreatedAt().isAfter(prevStart) && a.getCreatedAt().isBefore(prevEnd))
                .count();

        return calculateComparison(currentCount, previousCount);
    }

    private StatsComparison compareUsers(LocalDateTime currentStart, LocalDateTime prevStart, LocalDateTime prevEnd) {
        long currentCount = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt().isAfter(currentStart))
                .count();

        long previousCount = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt().isAfter(prevStart) && u.getCreatedAt().isBefore(prevEnd))
                .count();

        return calculateComparison(currentCount, previousCount);
    }

    private StatsComparison compareViews(LocalDateTime currentStart, LocalDateTime prevStart, LocalDateTime prevEnd) {
        long currentViews = getTotalViews();
        long previousViews = (long) (currentViews * 0.9); // Simulated data
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

    private List<TopStoryDTO> getTopStoriesByViews() {
        return storyRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "totalViews")))
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

    private List<TopAuthorDTO> getTopAuthorsByStories() {
        Map<Author, Long> authorStoryCount = storyRepository.findAll().stream()
                .filter(story -> story.getAuthor() != null)
                .collect(Collectors.groupingBy(Story::getAuthor, Collectors.counting()));

        return authorStoryCount.entrySet().stream()
                .sorted(Map.Entry.<Author, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> TopAuthorDTO.builder()
                        .id(entry.getKey().getId())
                        .name(entry.getKey().getName())
                        .totalStories(entry.getValue())
                        .avatar(entry.getKey().getAvatar())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ChartDataPoint> getNewStoriesChart(String period) {
        int days = "week".equalsIgnoreCase(period) ? 7 : 30;
        LocalDate today = LocalDate.now();
        List<ChartDataPoint> chartData = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            long count = storyRepository.findAll().stream()
                    .filter(s -> s.getCreatedAt().isAfter(startOfDay) && s.getCreatedAt().isBefore(endOfDay))
                    .count();

            chartData.add(ChartDataPoint.builder()
                    .date(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .count(count)
                    .build());
        }

        return chartData;
    }

    private List<ChartDataPoint> getNewUsersChart(String period) {
        int days = "week".equalsIgnoreCase(period) ? 7 : 30;
        LocalDate today = LocalDate.now();
        List<ChartDataPoint> chartData = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            long count = userRepository.findAll().stream()
                    .filter(u -> u.getCreatedAt().isAfter(startOfDay) && u.getCreatedAt().isBefore(endOfDay))
                    .count();

            chartData.add(ChartDataPoint.builder()
                    .date(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .count(count)
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
