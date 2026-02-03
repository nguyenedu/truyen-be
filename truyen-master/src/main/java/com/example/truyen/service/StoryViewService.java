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
     * trackView increments view counts and tracks unique viewers for a given story.
     * Executed asynchronously to prevent blocking the response.
     */
    @Async
    @Transactional
    public void trackView(Long storyId, Long userId, String ipAddress) {
        try {
            String today = LocalDate.now().toString();

            // 1. Increment today's view count
            String viewKey = RedisKeyConstants.STORY_VIEWS_TODAY + storyId;
            Long currentViews = redisTemplate.opsForValue().increment(viewKey, 1);
            redisTemplate.expire(viewKey, Duration.ofDays(1));

            // 2. Track unique viewers
            if (userId != null) {
                String viewerKey = RedisKeyConstants.STORY_UNIQUE_VIEWERS_TODAY + storyId;
                redisTemplate.opsForSet().add(viewerKey, userId);
                redisTemplate.expire(viewerKey, Duration.ofDays(1));
            }

            // 3. Increment historical view count by date
            String dateViewKey = RedisKeyConstants.STORY_VIEWS_DATE + today + ":" + storyId;
            redisTemplate.opsForValue().increment(dateViewKey, 1);
            redisTemplate.expire(dateViewKey, Duration.ofDays(35));

            // 4. Batch sync to database for every 10 views
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
     * Retrieve aggregate view count for the last N days.
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
     * Retrieve the number of unique viewers for today.
     */
    public Long getUniqueViewersToday(Long storyId) {
        String key = RedisKeyConstants.STORY_UNIQUE_VIEWERS_TODAY + storyId;
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0L;
    }

    /**
     * Retrieve total views for today.
     */
    public Long getViewsToday(Long storyId) {
        String key = RedisKeyConstants.STORY_VIEWS_TODAY + storyId;
        Object views = redisTemplate.opsForValue().get(key);
        return views != null ? Long.parseLong(views.toString()) : 0L;
    }

    /**
     * Synchronize all remaining view counts from Redis to the database.
     * Intended for end-of-day synchronization.
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
