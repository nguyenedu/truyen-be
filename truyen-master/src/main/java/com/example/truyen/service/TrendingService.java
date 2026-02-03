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
     * Calculate trending score for a story based on multiple metrics:
     * - View Score (40%)
     * - Rating Score (20%)
     * - Engagement Score (30%)
     * - Recency Score (10%)
     */
    public double calculateTrendingScore(Story story, int days) {
        try {
            // 1. View Score (40%)
            long recentViews = viewService.getRecentViews(story.getId(), days);
            Long maxViews = getMaxRecentViews(days);
            double viewScore = maxViews > 0 ? (recentViews * 40.0 / maxViews) : 0;

            // 2. Rating Score (20%)
            Double avgRating = ratingRepository.getAverageRating(story.getId());
            double ratingScore = avgRating != null ? (avgRating / 5.0 * 20.0) : 0;

            // 3. Engagement Score (30%): favorites, comments, and rating counts
            Long favoriteCount = favoriteRepository.countByStoryId(story.getId());
            Long commentCount = commentRepository.countByStoryId(story.getId());
            Long ratingCount = ratingRepository.countByStoryId(story.getId());

            double engagement = (favoriteCount * 3) + (commentCount * 2) + (ratingCount * 2);
            double engagementScore = Math.min(engagement / 100.0 * 30.0, 30.0);

            // 4. Recency Score (10%): degradation over time since last update
            long daysSinceUpdate = 0;
            if (story.getUpdatedAt() != null) {
                daysSinceUpdate = ChronoUnit.DAYS.between(story.getUpdatedAt().toLocalDate(), LocalDate.now());
            } else if (story.getCreatedAt() != null) {
                daysSinceUpdate = ChronoUnit.DAYS.between(story.getCreatedAt().toLocalDate(), LocalDate.now());
            }
            double recencyScore = 10.0 * Math.exp(-0.1 * daysSinceUpdate);

            return viewScore + ratingScore + engagementScore + recencyScore;
        } catch (Exception e) {
            log.error("Error calculating trending score for story ID {}: {}", story.getId(), e.getMessage());
            return 0;
        }
    }

    /**
     * Refreshes DAILY trending rankings every 30 minutes.
     */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void refreshDailyTrending() {
        log.info("Starting DAILY trending refresh...");
        refreshTrending(Ranking.RankingType.DAILY, 1, RedisKeyConstants.TRENDING_DAILY);
        log.info("Finished DAILY trending refresh");
    }

    /**
     * Refreshes WEEKLY trending rankings every 2 hours.
     */
    @Scheduled(cron = "0 0 */2 * * *")
    @Transactional
    public void refreshWeeklyTrending() {
        log.info("Starting WEEKLY trending refresh...");
        refreshTrending(Ranking.RankingType.WEEKLY, 7, RedisKeyConstants.TRENDING_WEEKLY);
        log.info("Finished WEEKLY trending refresh");
    }

    /**
     * Refreshes MONTHLY trending rankings every 6 hours.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void refreshMonthlyTrending() {
        log.info("Starting MONTHLY trending refresh...");
        refreshTrending(Ranking.RankingType.MONTHLY, 30, RedisKeyConstants.TRENDING_MONTHLY);
        log.info("Finished MONTHLY trending refresh");
    }

    /**
     * Core logic to calculate trending scores and update both database and Redis
     * cache.
     */
    private void refreshTrending(Ranking.RankingType rankingType, int days, String redisKey) {
        try {
            LocalDate today = LocalDate.now();

            List<Story> stories = storyRepository.findByStatusInWithDetails(
                    Arrays.asList(Story.Status.ONGOING, Story.Status.COMPLETED));

            log.info("Calculating {} trending for {} stories", rankingType, stories.size());

            if (stories.isEmpty()) {
                log.warn("No active stories found for trending calculation");
                return;
            }

            List<StoryTrendingDTO> trendingList = stories.parallelStream()
                    .map(story -> {
                        try {
                            double score = calculateTrendingScore(story, days);
                            Double avgRating = ratingRepository.getAverageRating(story.getId());
                            Long favoriteCount = favoriteRepository.countByStoryId(story.getId());
                            Long commentCount = commentRepository.countByStoryId(story.getId());

                            List<String> categoryNames = story.getCategories() != null
                                    ? story.getCategories().stream().map(cat -> cat.getName())
                                            .collect(Collectors.toList())
                                    : new java.util.ArrayList<>();

                            return StoryTrendingDTO.builder()
                                    .id(story.getId())
                                    .storyId(story.getId())
                                    .title(story.getTitle())
                                    .image(story.getImage())
                                    .totalViews(story.getTotalViews())
                                    .totalChapters(story.getTotalChapters())
                                    .authorName(story.getAuthor() != null ? story.getAuthor().getName() : null)
                                    .categories(categoryNames)
                                    .averageRating(avgRating)
                                    .favoriteCount(favoriteCount)
                                    .commentCount(commentCount)
                                    .trendingScore(score)
                                    .build();
                        } catch (Exception e) {
                            log.error("Failed to process trending for story ID {}: {}", story.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .sorted((a, b) -> Double.compare(b.getTrendingScore(), a.getTrendingScore()))
                    .limit(100)
                    .collect(Collectors.toList());

            if (trendingList.isEmpty()) {
                log.warn("Trending calculation resulted in an empty list");
                return;
            }

            rankingRepository.deleteByRankingTypeAndDate(rankingType, today);

            int rank = 1;
            for (StoryTrendingDTO dto : trendingList) {
                final int currentRank = rank;
                storyRepository.findById(dto.getId()).ifPresent(story -> {
                    Ranking ranking = Ranking.builder()
                            .story(story)
                            .rankPosition(currentRank)
                            .rankingType(rankingType)
                            .rankingDate(today)
                            .views(dto.getTotalViews())
                            .build();
                    rankingRepository.save(ranking);
                });
                dto.setRank(rank++);
            }

            cacheToRedis(redisKey, trendingList, rankingType);
            log.info("Successfully refreshed {} trending with {} items", rankingType, trendingList.size());

        } catch (Exception e) {
            log.error("Critical error during trending refresh: {}", e.getMessage(), e);
        }
    }

    /**
     * Cache trending data to Redis with appropriate TTL.
     */
    private void cacheToRedis(String redisKey, List<StoryTrendingDTO> trendingList, Ranking.RankingType rankingType) {
        try {
            redisTemplate.delete(redisKey);

            if (!trendingList.isEmpty()) {
                trendingList.forEach(dto -> {
                    try {
                        redisTemplate.opsForList().rightPush(redisKey, dto);
                    } catch (Exception e) {
                        log.error("Failed to cache story ID {} to Redis: {}", dto.getStoryId(), e.getMessage());
                    }
                });

                Duration ttl = switch (rankingType) {
                    case DAILY -> Duration.ofMinutes(30);
                    case WEEKLY -> Duration.ofHours(2);
                    case MONTHLY -> Duration.ofHours(6);
                };
                redisTemplate.expire(redisKey, ttl);
            }
        } catch (Exception e) {
            log.error("Failed to update Redis cache for key {}: {}", redisKey, e.getMessage(), e);
        }
    }

    /**
     * Retrieve max views across all stories for the last N days to normalize
     * scores.
     */
    private Long getMaxRecentViews(int days) {
        String key = RedisKeyConstants.MAX_VIEWS_PREFIX + days + "d";

        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return Long.parseLong(cached.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to read max views from cache: {}", e.getMessage());
        }

        long max = storyRepository.findAll().stream()
                .mapToLong(s -> viewService.getRecentViews(s.getId(), days))
                .max()
                .orElse(1L);

        try {
            redisTemplate.opsForValue().set(key, max, Duration.ofMinutes(30));
        } catch (Exception e) {
            log.warn("Failed to cache max views: {}", e.getMessage());
        }

        return max;
    }

    /**
     * Retrieve trending stories for a given ranking type.
     * Attempts to fetch from Redis first, falling back to database on cache miss.
     */
    @Transactional(readOnly = true)
    public List<StoryTrendingDTO> getTrending(Ranking.RankingType rankingType, int limit) {
        String redisKey = switch (rankingType) {
            case DAILY -> RedisKeyConstants.TRENDING_DAILY;
            case WEEKLY -> RedisKeyConstants.TRENDING_WEEKLY;
            case MONTHLY -> RedisKeyConstants.TRENDING_MONTHLY;
        };

        try {
            List<Object> cached = redisTemplate.opsForList().range(redisKey, 0, limit - 1);
            if (cached != null && !cached.isEmpty()) {
                return cached.stream()
                        .map(obj -> (StoryTrendingDTO) obj)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to read trending from Redis: {}", e.getMessage());
        }

        log.warn("Cache miss for {} trending, retrieving from database", rankingType);
        return getTrendingFromDB(rankingType, limit);
    }

    /**
     * Fallback method to retrieve trending rankings from the database.
     */
    private List<StoryTrendingDTO> getTrendingFromDB(Ranking.RankingType rankingType, int limit) {
        try {
            List<Ranking> rankings = rankingRepository.findLatestByRankingType(rankingType);

            return rankings.stream()
                    .limit(limit)
                    .map(ranking -> {
                        try {
                            Story story = ranking.getStory();
                            Double avgRating = ratingRepository.getAverageRating(story.getId());
                            Long favoriteCount = favoriteRepository.countByStoryId(story.getId());
                            Long commentCount = commentRepository.countByStoryId(story.getId());

                            List<String> categoryNames = story.getCategories() != null
                                    ? story.getCategories().stream().map(cat -> cat.getName())
                                            .collect(Collectors.toList())
                                    : new java.util.ArrayList<>();

                            return StoryTrendingDTO.builder()
                                    .id(story.getId())
                                    .storyId(story.getId())
                                    .title(story.getTitle())
                                    .image(story.getImage())
                                    .totalViews(story.getTotalViews())
                                    .totalChapters(story.getTotalChapters())
                                    .authorName(story.getAuthor() != null ? story.getAuthor().getName() : null)
                                    .categories(categoryNames)
                                    .averageRating(avgRating)
                                    .favoriteCount(favoriteCount)
                                    .commentCount(commentCount)
                                    .rank(ranking.getRankPosition())
                                    .build();
                        } catch (Exception e) {
                            log.error("Failed to map ranking item to DTO: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to retrieve trending from database: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Manually trigger a refresh for a specific trending period.
     */
    @Transactional
    public void manualRefresh(Ranking.RankingType rankingType) {
        switch (rankingType) {
            case DAILY -> refreshTrending(Ranking.RankingType.DAILY, 1, RedisKeyConstants.TRENDING_DAILY);
            case WEEKLY -> refreshTrending(Ranking.RankingType.WEEKLY, 7, RedisKeyConstants.TRENDING_WEEKLY);
            case MONTHLY -> refreshTrending(Ranking.RankingType.MONTHLY, 30, RedisKeyConstants.TRENDING_MONTHLY);
        }
    }

    /**
     * Cleanup historical rankings older than 90 days.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldRankings() {
        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        rankingRepository.deleteOlderThan(cutoffDate);
        log.info("Cleaned up historical rankings older than {}", cutoffDate);
    }
}
