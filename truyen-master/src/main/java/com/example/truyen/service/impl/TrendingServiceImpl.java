package com.example.truyen.service.impl;

import com.example.truyen.config.RedisKeyConstants;
import com.example.truyen.dto.response.StoryTrendingDTO;
import com.example.truyen.entity.Ranking;
import com.example.truyen.entity.Story;
import com.example.truyen.repository.*;
import com.example.truyen.service.StoryViewService;
import com.example.truyen.service.TrendingService;
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
public class TrendingServiceImpl implements TrendingService {

    private static final double VIEW_WEIGHT = 40.0;
    private static final double RATING_WEIGHT = 20.0;
    private static final double ENGAGEMENT_WEIGHT = 30.0;
    private static final double RECENCY_WEIGHT = 10.0;

    private static final int FAVORITE_SCORE = 3;
    private static final int COMMENT_SCORE = 2;
    private static final int RATING_SCORE = 2;

    private final RedisTemplate<String, Object> redisTemplate;
    private final StoryRepository storyRepository;
    private final RatingRepository ratingRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;
    private final RankingRepository rankingRepository;
    private final StoryViewService viewService;

    // Logic cốt lõi tính toán và cập nhật xu hướng
    @Override
    public double calculateTrendingScore(Story story, int days) {
        try {
            // 1. Điểm lượt xem (40%)
            long recentViews = viewService.getRecentViews(story.getId(), days);
            Long maxViews = getMaxRecentViews(days);
            double viewScore = maxViews > 0 ? (recentViews * VIEW_WEIGHT / maxViews) : 0;

            // 2. Điểm đánh giá (20%)
            Double avgRating = ratingRepository.getAverageRating(story.getId());
            double ratingScore = avgRating != null ? (avgRating / 5.0 * RATING_WEIGHT) : 0;

            // 3. Điểm tương tác (30%): yêu thích, bình luận, đánh giá
            Long favoriteCount = favoriteRepository.countByStoryId(story.getId());
            Long commentCount = commentRepository.countByStoryId(story.getId());
            Long ratingCount = ratingRepository.countByStoryId(story.getId());

            double engagement = (favoriteCount * FAVORITE_SCORE) + (commentCount * COMMENT_SCORE)
                    + (ratingCount * RATING_SCORE);
            double engagementScore = Math.min(engagement / 100.0 * ENGAGEMENT_WEIGHT, ENGAGEMENT_WEIGHT);

            // 4. Điểm độ mới (10%): giảm dần theo thời gian
            long daysSinceUpdate = 0;
            if (story.getUpdatedAt() != null) {
                daysSinceUpdate = ChronoUnit.DAYS.between(story.getUpdatedAt().toLocalDate(), LocalDate.now());
            } else if (story.getCreatedAt() != null) {
                daysSinceUpdate = ChronoUnit.DAYS.between(story.getCreatedAt().toLocalDate(), LocalDate.now());
            }
            double recencyScore = RECENCY_WEIGHT * Math.exp(-0.1 * daysSinceUpdate);

            return viewScore + ratingScore + engagementScore + recencyScore;
        } catch (Exception e) {
            log.error("Error calculating trending score for story ID {}: {}", story.getId(), e.getMessage());
            return 0;
        }
    }

    // Làm mới xu hướng HÀNG NGÀY mỗi 30 phút
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    @Override
    public void refreshDailyTrending() {
        log.info("Starting DAILY trending refresh...");
        refreshTrending(Ranking.RankingType.DAILY, 1, RedisKeyConstants.TRENDING_DAILY);
        log.info("Completed DAILY trending refresh");
    }

    // Làm mới xu hướng HÀNG TUẦN mỗi 2 giờ
    @Scheduled(cron = "0 0 */2 * * *")
    @Transactional
    @Override
    public void refreshWeeklyTrending() {
        log.info("Starting WEEKLY trending refresh...");
        refreshTrending(Ranking.RankingType.WEEKLY, 7, RedisKeyConstants.TRENDING_WEEKLY);
        log.info("Completed WEEKLY trending refresh");
    }

    // Làm mới xu hướng HÀNG THÁNG mỗi 6 giờ
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    @Override
    public void refreshMonthlyTrending() {
        log.info("Starting MONTHLY trending refresh...");
        refreshTrending(Ranking.RankingType.MONTHLY, 30, RedisKeyConstants.TRENDING_MONTHLY);
        log.info("Completed MONTHLY trending refresh");
    }

    private void refreshTrending(Ranking.RankingType rankingType, int days, String redisKey) {
        try {
            var today = LocalDate.now();

            var stories = storyRepository.findByStatusInWithDetails(
                    Arrays.asList(Story.Status.ONGOING, Story.Status.COMPLETED));

            log.info("Calculating {} trending for {} stories", rankingType, stories.size());

            if (stories.isEmpty()) {
                log.warn("No active stories found for trending calculation");
                return;
            }

            var trendingList = stories.parallelStream()
                    .map(story -> {
                        try {
                            double score = calculateTrendingScore(story, days);
                            var avgRating = ratingRepository.getAverageRating(story.getId());
                            var favoriteCount = favoriteRepository.countByStoryId(story.getId());
                            var commentCount = commentRepository.countByStoryId(story.getId());

                            var categoryNames = story.getCategories() != null
                                    ? story.getCategories().stream().map(cat -> cat.getName())
                                            .collect(Collectors.toList())
                                    : new java.util.ArrayList<String>();

                            return StoryTrendingDTO.builder()
                                    .id(story.getId())
                                    .storyId(story.getId())
                                    .title(story.getTitle())
                                    .image(story.getImage())
                                    .totalViews(story.getTotalViews())
                                    .totalChapters(story.getTotalChapters())
                                    .authorName(story.getAuthor() != null ? story.getAuthor().getName() : null)
                                    .authorId(story.getAuthor() != null ? story.getAuthor().getId() : null)
                                    .categories(categoryNames)
                                    .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                                    .favoriteCount(favoriteCount)
                                    .commentCount(commentCount)
                                    .trendingScore(score)
                                    .build();
                        } catch (Exception e) {
                            log.error("Failed to process trending for story ID {}: {}", story.getId(),
                                    e.getMessage());
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .sorted((a, b) -> Double.compare(b.getTrendingScore(), a.getTrendingScore()))
                    .limit(100)
                    .collect(Collectors.toList());

            if (trendingList.isEmpty()) {
                log.warn("Trending calculation resulted in empty list");
                return;
            }

            rankingRepository.deleteByRankingTypeAndDate(rankingType, today);

            int rank = 1;
            for (StoryTrendingDTO dto : trendingList) {
                final int currentRank = rank;
                storyRepository.findById(dto.getId()).ifPresent(story -> {
                    var ranking = Ranking.builder()
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

    // Lưu cache Redis với TTL tương ứng
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

    // Lấy max view trong N ngày để chuẩn hóa điểm số
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

    // Lấy danh sách xu hướng (ưu tiên Redis, fallback DB)
    @Transactional(readOnly = true)
    @Override
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

    // Lấy danh sách xu hướng từ DB
    private List<StoryTrendingDTO> getTrendingFromDB(Ranking.RankingType rankingType, int limit) {
        try {
            var rankings = rankingRepository.findLatestByRankingType(rankingType);

            return rankings.stream()
                    .limit(limit)
                    .map(ranking -> {
                        try {
                            var story = ranking.getStory();
                            var avgRating = ratingRepository.getAverageRating(story.getId());
                            var favoriteCount = favoriteRepository.countByStoryId(story.getId());
                            var commentCount = commentRepository.countByStoryId(story.getId());

                            var categoryNames = story.getCategories() != null
                                    ? story.getCategories().stream().map(cat -> cat.getName())
                                            .collect(Collectors.toList())
                                    : new java.util.ArrayList<String>();

                            return StoryTrendingDTO.builder()
                                    .id(story.getId())
                                    .storyId(story.getId())
                                    .title(story.getTitle())
                                    .image(story.getImage())
                                    .totalViews(story.getTotalViews())
                                    .totalChapters(story.getTotalChapters())
                                    .authorName(story.getAuthor() != null ? story.getAuthor().getName() : null)
                                    .authorId(story.getAuthor() != null ? story.getAuthor().getId() : null)
                                    .categories(categoryNames)
                                    .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
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

    // Trigger làm mới thủ công
    @Transactional
    @Override
    public void manualRefresh(Ranking.RankingType rankingType) {
        switch (rankingType) {
            case DAILY -> refreshTrending(Ranking.RankingType.DAILY, 1, RedisKeyConstants.TRENDING_DAILY);
            case WEEKLY -> refreshTrending(Ranking.RankingType.WEEKLY, 7, RedisKeyConstants.TRENDING_WEEKLY);
            case MONTHLY -> refreshTrending(Ranking.RankingType.MONTHLY, 30, RedisKeyConstants.TRENDING_MONTHLY);
        }
    }

    // Xóa bảng xếp hạng lịch sử cũ hơn 90 ngày
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    @Override
    public void cleanupOldRankings() {
        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        rankingRepository.deleteOlderThan(cutoffDate);
        log.info("Cleaned up old rankings older than {}", cutoffDate);
    }
}
