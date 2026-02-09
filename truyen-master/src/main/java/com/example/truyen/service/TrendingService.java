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
     * Tính toán điểm xu hướng cho truyện dựa trên nhiều tiêu chí:
     * - Điểm lượt xem (40%)
     * - Điểm đánh giá (20%)
     * - Điểm tương tác (30%)
     * - Điểm độ mới (10%)
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
            log.error("Lỗi khi tính điểm xu hướng cho truyện có ID {}: {}", story.getId(), e.getMessage());
            return 0;
        }
    }

    /**
     * Làm mới bảng xếp hạng xu hướng HÀNG NGÀY mỗi 30 phút.
     */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void refreshDailyTrending() {
        log.info("Bắt đầu làm mới xu hướng HÀNG NGÀY...");
        refreshTrending(Ranking.RankingType.DAILY, 1, RedisKeyConstants.TRENDING_DAILY);
        log.info("Hoàn tất làm mới xu hướng HÀNG NGÀY");
    }

    /**
     * Làm mới bảng xếp hạng xu hướng HÀNG TUẦN mỗi 2 giờ.
     */
    @Scheduled(cron = "0 0 */2 * * *")
    @Transactional
    public void refreshWeeklyTrending() {
        log.info("Bắt đầu làm mới xu hướng HÀNG TUẦN...");
        refreshTrending(Ranking.RankingType.WEEKLY, 7, RedisKeyConstants.TRENDING_WEEKLY);
        log.info("Hoàn tất làm mới xu hướng HÀNG TUẦN");
    }

    /**
     * Làm mới bảng xếp hạng xu hướng HÀNG THÁNG mỗi 6 giờ.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void refreshMonthlyTrending() {
        log.info("Bắt đầu làm mới xu hướng HÀNG THÁNG...");
        refreshTrending(Ranking.RankingType.MONTHLY, 30, RedisKeyConstants.TRENDING_MONTHLY);
        log.info("Hoàn tất làm mới xu hướng HÀNG THÁNG");
    }

    /**
     * Logic cốt lõi để tính điểm xu hướng và cập nhật cả cơ sở dữ liệu và bộ nhớ
     * đệm Redis.
     */
    private void refreshTrending(Ranking.RankingType rankingType, int days, String redisKey) {
        try {
            LocalDate today = LocalDate.now();

            List<Story> stories = storyRepository.findByStatusInWithDetails(
                    Arrays.asList(Story.Status.ONGOING, Story.Status.COMPLETED));

            log.info("Đang tính toán xu hướng {} cho {} truyện", rankingType, stories.size());

            if (stories.isEmpty()) {
                log.warn("Không tìm thấy truyện đang hoạt động nào để tính toán xu hướng");
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
                                    .authorId(story.getAuthor() != null ? story.getAuthor().getId() : null)
                                    .categories(categoryNames)
                                    .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                                    .favoriteCount(favoriteCount)
                                    .commentCount(commentCount)
                                    .trendingScore(score)
                                    .build();
                        } catch (Exception e) {
                            log.error("Không thể xử lý xu hướng cho truyện có ID {}: {}", story.getId(),
                                    e.getMessage());
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .sorted((a, b) -> Double.compare(b.getTrendingScore(), a.getTrendingScore()))
                    .limit(100)
                    .collect(Collectors.toList());

            if (trendingList.isEmpty()) {
                log.warn("Tính toán xu hướng cho kết quả là danh sách trống");
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
            log.info("Làm mới thành công xu hướng {} với {} mục", rankingType, trendingList.size());

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng trong quá trình làm mới xu hướng: {}", e.getMessage(), e);
        }
    }

    /**
     * Lưu dữ liệu xu hướng vào Redis với thời gian sống (TTL) phù hợp.
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
     * Lấy tổng số lượt xem tối đa của tất cả các truyện trong N ngày qua để chuẩn
     * hóa điểm số.
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
     * Lấy danh sách truyện xu hướng cho một loại xếp hạng cụ thể.
     * Cố gắng lấy từ Redis trước, nếu không có sẽ lấy từ cơ sở dữ liệu.
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
     * Phương pháp dự phòng để lấy bảng xếp hạng xu hướng từ cơ sở dữ liệu.
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

    /**
     * Kích hoạt làm mới thủ công cho một khoảng thời gian xu hướng cụ thể.
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
     * Xóa sạch các bảng xếp hạng lịch sử cũ hơn 90 ngày.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldRankings() {
        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        rankingRepository.deleteOlderThan(cutoffDate);
        log.info("Đã xóa sạch các bảng xếp hạng lịch sử cũ hơn {}", cutoffDate);
    }
}
