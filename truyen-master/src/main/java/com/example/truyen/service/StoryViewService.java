package com.example.truyen.service;

import com.example.truyen.config.RedisKeyConstants;
import com.example.truyen.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryViewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StoryRepository storyRepository;

    /**
     * trackView tăng lượt view và số người dùng xem Story ấy.
     */
    @Async
    @Transactional
    public void trackView(Long storyId, Long userId, String ipAddress) {
        try {
            String today = LocalDate.now().toString();

            // 1. Tăng lượng views trong ngày
            String viewKey = RedisKeyConstants.STORY_VIEWS_TODAY + storyId;
            Long currentViews = redisTemplate.opsForValue().increment(viewKey, 1);
            redisTemplate.expire(viewKey, Duration.ofDays(1));

            // 2. Đếm số lượng user đã xem trong ngày
            if (userId != null) {
                String viewerKey = RedisKeyConstants.STORY_UNIQUE_VIEWERS_TODAY + storyId;
                redisTemplate.opsForSet().add(viewerKey, userId);
                redisTemplate.expire(viewerKey, Duration.ofDays(1));
            }

            // 3. tăng lượt view theo ngày
            String dateViewKey = RedisKeyConstants.STORY_VIEWS_DATE + today + ":" + storyId;
            redisTemplate.opsForValue().increment(dateViewKey, 1);
            redisTemplate.expire(dateViewKey, Duration.ofDays(35));

            // 4. Đồng bộ hóa với cơ sở dữ liệu sau mỗi 10 lượt xem
            if (currentViews != null && currentViews % 10 == 0) {
                storyRepository.incrementTotalViews(storyId, 10);
                log.debug("Synced 10 views to database for story ID: {}", storyId);
            }

            log.debug("View tracked for story ID: {} by user ID: {} from IP: {}", storyId, userId, ipAddress);

        } catch (Exception e) {
            log.error("Failed to track view for story ID: {}: {}", storyId, e.getMessage());
        }
    }

    /**
     * Đếm số lượng view trung bình trong N ngày
     */
    public Long getRecentViews(Long storyId, int days) {
        long totalViews = 0;
        LocalDate today = LocalDate.now();

        for (int i = 0; i < days; i++) {
            String date = today.minusDays(i).toString();
            String key = RedisKeyConstants.STORY_VIEWS_DATE + date + ":" + storyId;

            Object views = redisTemplate.opsForValue().get(key);
            if (views != null) {
                totalViews += Long.parseLong(views.toString());
            }
        }

        return totalViews;
    }

    /**
     * Dếm số người dùng duy nhất đã xem truyện trong ngày hôm nay.
     */
    public Long getUniqueViewersToday(Long storyId) {
        String key = RedisKeyConstants.STORY_UNIQUE_VIEWERS_TODAY + storyId;
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0L;
    }

    /**
     *  Tính tổng số view trong ngày
     */
    public Long getViewsToday(Long storyId) {
        String key = RedisKeyConstants.STORY_VIEWS_TODAY + storyId;
        Object views = redisTemplate.opsForValue().get(key);
        return views != null ? Long.parseLong(views.toString()) : 0L;
    }

    /**
     * Đồng bộ lượt xem từ redis về cơ sở dữ liệu.
     */
    @Transactional
    public void syncAllViewsToDatabase() {
        try {
            var keys = redisTemplate.keys(RedisKeyConstants.STORY_VIEWS_TODAY + "*");

            if (keys != null) {
                for (String key : keys) {
                    try {
                        String storyIdStr = key.replace(RedisKeyConstants.STORY_VIEWS_TODAY, "");
                        Long storyId = Long.parseLong(storyIdStr);

                        Object views = redisTemplate.opsForValue().get(key);
                        if (views != null) {
                            int viewCount = Integer.parseInt(views.toString());
                            int remainder = viewCount % 10;

                            if (remainder > 0) {
                                storyRepository.incrementTotalViews(storyId, remainder);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to sync views for key {}: {}", key, e.getMessage());
                    }
                }
                log.info("Successfully synchronized all views from Redis to database");
            }
        } catch (Exception e) {
            log.error("Error during syncAllViewsToDatabase: {}", e.getMessage());
        }
    }
}
