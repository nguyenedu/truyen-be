package com.example.truyen.service;

import com.example.truyen.config.RedisKeyConstants;
import com.example.truyen.dto.response.StoryTrendingDTO;
import com.example.truyen.entity.Ranking;
import com.example.truyen.entity.Story;
import com.example.truyen.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StoryRepository storyRepository;
    private final RatingRepository ratingRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;
    private final RankingRepository rankingRepository;
    private final StoryViewService viewService;

    /**
     * Tính trending score cho 1 story
     * Score = ViewScore(40%) + RatingScore(20%) + EngagementScore(30%) + RecencyScore(10%)
     */
    public double calculateTrendingScore(Story story, int days) {
        // 1. View Score (40%)
        long recentViews = viewService.getRecentViews(story.getId(), days);
        Long maxViews = getMaxRecentViews(days);
        double viewScore = maxViews > 0 ? (recentViews * 40.0 / maxViews) : 0;

        // 2. Rating Score (20%)
        Double avgRating = ratingRepository.getAverageRating(story.getId());
        double ratingScore = avgRating != null ? (avgRating / 5.0 * 20.0) : 0;

        // 3. Engagement Score (30%)
        Long favoriteCount = favoriteRepository.countByStoryId(story.getId());
        Long commentCount = commentRepository.countByStoryId(story.getId());
        Long ratingCount = ratingRepository.countByStoryId(story.getId());

        double engagement = (favoriteCount * 3) + (commentCount * 2) + (ratingCount * 2);
        double engagementScore = Math.min(engagement / 100.0 * 30.0, 30.0);

        // 4. Recency Score (10%)
        long daysSinceUpdate = ChronoUnit.DAYS.between(
                story.getUpdatedAt().toLocalDate(),
                LocalDate.now()
        );
        double recencyScore = 10.0 * Math.exp(-0.1 * daysSinceUpdate);

        return viewScore + ratingScore + engagementScore + recencyScore;
    }


    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void refreshDailyTrending() {
        log.info("Starting refresh DAILY trending...");
        refreshTrending(Ranking.RankingType.DAILY, 1, RedisKeyConstants.TRENDING_DAILY);
        log.info("Finished refresh DAILY trending");
    }


    @Scheduled(cron = "0 0 */2 * * *")
    @Transactional
    public void refreshWeeklyTrending() {
        log.info("Starting refresh WEEKLY trending...");
        refreshTrending(Ranking.RankingType.WEEKLY, 7, RedisKeyConstants.TRENDING_WEEKLY);
        log.info("Finished refresh WEEKLY trending");
    }

    /**
     * Scheduled: Refresh MONTHLY trending (mỗi 6 giờ)
     */
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void refreshMonthlyTrending() {
        log.info("Starting refresh MONTHLY trending...");
        refreshTrending(Ranking.RankingType.MONTHLY, 30, RedisKeyConstants.TRENDING_MONTHLY);
        log.info("Finished refresh MONTHLY trending");
    }

    /**
     * Core: Tính và lưu trending
     */
    private void refreshTrending(Ranking.RankingType rankingType, int days, String redisKey) {
        try {
            LocalDate today = LocalDate.now();

            // 1. Lấy stories ONGOING và COMPLETED
            List<Story> stories = storyRepository.findByStatusIn(
                    Arrays.asList(Story.Status.ONGOING, Story.Status.COMPLETED)
            );

            log.info("Calculating {} trending for {} stories", rankingType, stories.size());

            // 2. Tính score
            List<StoryTrendingDTO> trendingList = stories.parallelStream()
                    .map(story -> {
                        double score = calculateTrendingScore(story, days);
                        Double avgRating = ratingRepository.getAverageRating(story.getId());
                        Long favoriteCount = favoriteRepository.countByStoryId(story.getId());
                        Long commentCount = commentRepository.countByStoryId(story.getId());

                        return StoryTrendingDTO.builder()
                                .id(story.getId())
                                .title(story.getTitle())
                                .image(story.getImage())
                                .totalViews(story.getTotalViews())
                                .averageRating(avgRating)
                                .favoriteCount(favoriteCount)
                                .commentCount(commentCount)
                                .trendingScore(score)
                                .build();
                    })
                    .sorted((a, b) -> Double.compare(b.getTrendingScore(), a.getTrendingScore()))
                    .limit(100)
                    .collect(Collectors.toList());

            // 3. Xóa rankings cũ
            rankingRepository.deleteByRankingTypeAndDate(rankingType, today);

            // 4. Lưu vào DB
            int rank = 1;
            for (StoryTrendingDTO dto : trendingList) {
                Story story = storyRepository.findById(dto.getId()).orElse(null);
                if (story != null) {
                    Ranking ranking = Ranking.builder()
                            .story(story)
                            .rankPosition(rank++)
                            .rankingType(rankingType)
                            .rankingDate(today)
                            .views(dto.getTotalViews())
                            .build();

                    rankingRepository.save(ranking);
                }
            }

            // 5. Cache vào Redis
            redisTemplate.delete(redisKey);

            if (!trendingList.isEmpty()) {
                for (int i = 0; i < trendingList.size(); i++) {
                    trendingList.get(i).setRank(i + 1);
                }

                trendingList.forEach(dto ->
                        redisTemplate.opsForList().rightPush(redisKey, dto)
                );

                Duration ttl = switch (rankingType) {
                    case DAILY -> Duration.ofMinutes(30);
                    case WEEKLY -> Duration.ofHours(2);
                    case MONTHLY -> Duration.ofHours(6);
                };
                redisTemplate.expire(redisKey, ttl);

                log.info("Cached {} trending stories", trendingList.size());
            }

        } catch (Exception e) {
            log.error("Error refreshing trending: {}", e.getMessage(), e);
        }
    }


    private Long getMaxRecentViews(int days) {
        String key = RedisKeyConstants.MAX_VIEWS_PREFIX + days + "d";

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return Long.parseLong(cached.toString());
        }

        List<Story> stories = storyRepository.findAll();
        long max = stories.stream()
                .mapToLong(s -> viewService.getRecentViews(s.getId(), days))
                .max()
                .orElse(1L);

        redisTemplate.opsForValue().set(key, max, Duration.ofMinutes(30));
        return max;
    }

    /**
     * API: Lấy trending
     */
    public List<StoryTrendingDTO> getTrending(Ranking.RankingType rankingType, int limit) {
        String redisKey = switch (rankingType) {
            case DAILY -> RedisKeyConstants.TRENDING_DAILY;
            case WEEKLY -> RedisKeyConstants.TRENDING_WEEKLY;
            case MONTHLY -> RedisKeyConstants.TRENDING_MONTHLY;
        };

        List<Object> cached = redisTemplate.opsForList().range(redisKey, 0, limit - 1);

        if (cached != null && !cached.isEmpty()) {
            return cached.stream()
                    .map(obj -> (StoryTrendingDTO) obj)
                    .collect(Collectors.toList());
        }

        // Cache miss → Get from DB
        log.warn("Cache MISS, loading from DB");
        List<Ranking> rankings = rankingRepository.findLatestByRankingType(rankingType);

        return rankings.stream()
                .limit(limit)
                .map(ranking -> {
                    Story story = ranking.getStory();
                    Double avgRating = ratingRepository.getAverageRating(story.getId());
                    Long favoriteCount = favoriteRepository.countByStoryId(story.getId());
                    Long commentCount = commentRepository.countByStoryId(story.getId());

                    return StoryTrendingDTO.builder()
                            .id(story.getId())
                            .title(story.getTitle())
                            .image(story.getImage())
                            .totalViews(story.getTotalViews())
                            .averageRating(avgRating)
                            .favoriteCount(favoriteCount)
                            .commentCount(commentCount)
                            .rank(ranking.getRankPosition())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Manual refresh
     */
    public void manualRefresh(Ranking.RankingType rankingType) {
        switch (rankingType) {
            case DAILY -> refreshTrending(Ranking.RankingType.DAILY, 1, RedisKeyConstants.TRENDING_DAILY);
            case WEEKLY -> refreshTrending(Ranking.RankingType.WEEKLY, 7, RedisKeyConstants.TRENDING_WEEKLY);
            case MONTHLY -> refreshTrending(Ranking.RankingType.MONTHLY, 30, RedisKeyConstants.TRENDING_MONTHLY);
        }
    }

    /**
     * Cleanup old rankings
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldRankings() {
        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        rankingRepository.deleteOlderThan(cutoffDate);
        log.info("Cleaned up rankings older than {}", cutoffDate);
    }
}