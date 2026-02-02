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
     * Score = ViewScore(40%) + RatingScore(20%) + EngagementScore(30%) +
     * RecencyScore(10%)
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

            // 3. Engagement Score (30%)
            Long favoriteCount = favoriteRepository.countByStoryId(story.getId());
            Long commentCount = commentRepository.countByStoryId(story.getId());
            Long ratingCount = ratingRepository.countByStoryId(story.getId());

            double engagement = (favoriteCount * 3) + (commentCount * 2) + (ratingCount * 2);
            double engagementScore = Math.min(engagement / 100.0 * 30.0, 30.0);

            // 4. Recency Score (10%)
            long daysSinceUpdate = 0;
            if (story.getUpdatedAt() != null) {
                daysSinceUpdate = ChronoUnit.DAYS.between(
                        story.getUpdatedAt().toLocalDate(),
                        LocalDate.now());
            } else if (story.getCreatedAt() != null) {
                daysSinceUpdate = ChronoUnit.DAYS.between(
                        story.getCreatedAt().toLocalDate(),
                        LocalDate.now());
            }
            double recencyScore = 10.0 * Math.exp(-0.1 * daysSinceUpdate);

            return viewScore + ratingScore + engagementScore + recencyScore;
        } catch (Exception e) {
            log.error("Error calculating trending score for story {}: {}", story.getId(), e.getMessage());
            return 0;
        }
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

            // 1. Lấy stories ONGOING và COMPLETED with eager fetch
            List<Story> stories = storyRepository.findByStatusInWithDetails(
                    Arrays.asList(Story.Status.ONGOING, Story.Status.COMPLETED));

            log.info("Calculating {} trending for {} stories", rankingType, stories.size());

            if (stories.isEmpty()) {
                log.warn("No stories found with status ONGOING or COMPLETED!");
                return;
            }

            // 2. Tính score
            List<StoryTrendingDTO> trendingList = stories.parallelStream()
                    .map(story -> {
                        try {
                            double score = calculateTrendingScore(story, days);
                            Double avgRating = ratingRepository.getAverageRating(story.getId());
                            Long favoriteCount = favoriteRepository.countByStoryId(story.getId());
                            Long commentCount = commentRepository.countByStoryId(story.getId());

                            // Categories are now eagerly fetched
                            List<String> categoryNames = story.getCategories() != null
                                    ? story.getCategories().stream()
                                    .map(cat -> cat.getName())
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
                            log.error("Error processing story {}: {}", story.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .sorted((a, b) -> Double.compare(b.getTrendingScore(), a.getTrendingScore()))
                    .limit(100)
                    .collect(Collectors.toList());

            log.info("Calculated trending list size: {}", trendingList.size());

            if (trendingList.isEmpty()) {
                log.warn(
                        "Trending list is empty after calculation! All stories may have been filtered out due to errors.");
                return;
            }

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
            cacheToRedis(redisKey, trendingList, rankingType);

        } catch (Exception e) {
            log.error("Error refreshing trending: {}", e.getMessage(), e);
        }
    }

    /**
     * Improved Redis caching with error handling
     */
    private void cacheToRedis(String redisKey, List<StoryTrendingDTO> trendingList, Ranking.RankingType rankingType) {
        try {
            log.info("Deleting old Redis cache for key: {}", redisKey);

            // Delete old cache
            Boolean deleted = redisTemplate.delete(redisKey);
            log.debug("Old cache deleted: {}", deleted);

            if (!trendingList.isEmpty()) {
                // Set rank numbers
                for (int i = 0; i < trendingList.size(); i++) {
                    trendingList.get(i).setRank(i + 1);
                }

                log.info("Caching {} trending stories to Redis key: {}", trendingList.size(), redisKey);

                // Push each item to Redis list
                for (StoryTrendingDTO dto : trendingList) {
                    try {
                        redisTemplate.opsForList().rightPush(redisKey, dto);
                        log.debug("Cached story: {} - {}", dto.getRank(), dto.getTitle());
                    } catch (Exception e) {
                        log.error("Failed to cache story {}: {}", dto.getStoryId(), e.getMessage());
                    }
                }

                // Set TTL
                Duration ttl = switch (rankingType) {
                    case DAILY -> Duration.ofMinutes(30);
                    case WEEKLY -> Duration.ofHours(2);
                    case MONTHLY -> Duration.ofHours(6);
                };

                Boolean expireSet = redisTemplate.expire(redisKey, ttl);
                log.info("Successfully cached {} trending stories with TTL: {} (expire set: {})",
                        trendingList.size(), ttl, expireSet);
            } else {
                log.warn("Trending list is empty, nothing to cache");
            }

        } catch (Exception e) {
            log.error("Error caching to Redis key {}: {}", redisKey, e.getMessage(), e);
        }
    }

    private Long getMaxRecentViews(int days) {
        String key = RedisKeyConstants.MAX_VIEWS_PREFIX + days + "d";

        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return Long.parseLong(cached.toString());
            }
        } catch (Exception e) {
            log.warn("Error reading max views from cache: {}", e.getMessage());
        }

        List<Story> stories = storyRepository.findAll();
        long max = stories.stream()
                .mapToLong(s -> viewService.getRecentViews(s.getId(), days))
                .max()
                .orElse(1L);

        try {
            redisTemplate.opsForValue().set(key, max, Duration.ofMinutes(30));
        } catch (Exception e) {
            log.warn("Error caching max views: {}", e.getMessage());
        }

        return max;
    }

    /**
     * API: Lấy trending
     * FIXED: Added @Transactional(readOnly = true) to keep session open for lazy loading
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
                log.info("Cache HIT for {} trending, size: {}", rankingType, cached.size());
                return cached.stream()
                        .map(obj -> (StoryTrendingDTO) obj)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error reading trending from Redis: {}", e.getMessage());
            // Fallback to DB
        }

        // Cache miss → Get from DB
        log.warn("Cache MISS for {}, loading from DB", rankingType);
        return getTrendingFromDB(rankingType, limit);
    }

    /**
     * Fallback: Get trending from database
     * IMPORTANT: This method is called within @Transactional context from getTrending()
     */
    private List<StoryTrendingDTO> getTrendingFromDB(Ranking.RankingType rankingType, int limit) {
        try {
            // Rankings now have Story, Author, and Categories eagerly fetched via JOIN FETCH
            List<Ranking> rankings = rankingRepository.findLatestByRankingType(rankingType);

            log.info("Loaded {} rankings from DB for {}", rankings.size(), rankingType);

            return rankings.stream()
                    .limit(limit)
                    .map(ranking -> {
                        try {
                            Story story = ranking.getStory();

                            // These are now safe to access because of @Transactional and JOIN FETCH
                            Double avgRating = ratingRepository.getAverageRating(story.getId());
                            Long favoriteCount = favoriteRepository.countByStoryId(story.getId());
                            Long commentCount = commentRepository.countByStoryId(story.getId());

                            // Categories and Author are eagerly loaded, safe to access
                            List<String> categoryNames = new java.util.ArrayList<>();
                            if (story.getCategories() != null) {
                                categoryNames = story.getCategories().stream()
                                        .map(cat -> cat.getName())
                                        .collect(Collectors.toList());
                            }

                            String authorName = null;
                            if (story.getAuthor() != null) {
                                authorName = story.getAuthor().getName();
                            }

                            return StoryTrendingDTO.builder()
                                    .id(story.getId())
                                    .storyId(story.getId())
                                    .title(story.getTitle())
                                    .image(story.getImage())
                                    .totalViews(story.getTotalViews())
                                    .totalChapters(story.getTotalChapters())
                                    .authorName(authorName)
                                    .categories(categoryNames)
                                    .averageRating(avgRating)
                                    .favoriteCount(favoriteCount)
                                    .commentCount(commentCount)
                                    .rank(ranking.getRankPosition())
                                    .build();
                        } catch (Exception e) {
                            log.error("Error mapping ranking {} to DTO: {}", ranking.getId(), e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting trending from DB: {}", e.getMessage(), e);
            return List.of(); // Return empty list instead of null
        }
    }

    /**
     * Manual refresh
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