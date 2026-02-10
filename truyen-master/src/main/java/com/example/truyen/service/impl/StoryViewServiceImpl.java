package com.example.truyen.service.impl;

import com.example.truyen.config.RedisKeyConstants;
import com.example.truyen.entity.StoryView;
import com.example.truyen.repository.StoryRepository;
import com.example.truyen.repository.StoryViewRepository;
import com.example.truyen.service.StoryViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryViewServiceImpl implements StoryViewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StoryViewRepository storyViewRepository;
    private final StoryRepository storyRepository;

    // Theo dõi lượt xem truyện
    @Override
    public void trackView(Long storyId, String visitorId) {
        String viewCountKey = RedisKeyConstants.VIEW_COUNT_PREFIX + storyId;
        String uniqueViewerKey = RedisKeyConstants.UNIQUE_VIEWERS_PREFIX + storyId + ":" + LocalDate.now();
        String dailyViewKey = RedisKeyConstants.DAILY_VIEW_PREFIX + storyId + ":" + LocalDate.now();

        try {
            redisTemplate.opsForValue().increment(viewCountKey);
            redisTemplate.expire(viewCountKey, Duration.ofDays(7));

            String visitorKey = visitorId != null ? visitorId : "anonymous_" + System.currentTimeMillis();
            redisTemplate.opsForSet().add(uniqueViewerKey, visitorKey);
            redisTemplate.expire(uniqueViewerKey, Duration.ofDays(1));

            redisTemplate.opsForValue().increment(dailyViewKey);
            redisTemplate.expire(dailyViewKey, Duration.ofDays(1));

            StoryView view = StoryView.builder()
                    .storyId(storyId)
                    .visitorId(visitorId)
                    .viewedAt(LocalDateTime.now())
                    .build();
            storyViewRepository.save(view);
        } catch (Exception e) {
            log.error("Error tracking view for story {}: {}", storyId, e.getMessage());
        }
    }

    // Theo dõi lượt xem truyện với userId
    @Override
    public void trackView(Long storyId, Long userId, String visitorId) {
        String visitor = userId != null ? "user_" + userId : visitorId;
        trackView(storyId, visitor);
    }

    // Lấy lượt xem gần đây trong N ngày
    @Override
    public long getRecentViews(Long storyId, int days) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            Long count = storyViewRepository.countByStoryIdAndViewedAtAfter(storyId, since);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Error getting recent views for story {}: {}", storyId, e.getMessage());
            return 0L;
        }
    }

    // Đếm số người xem duy nhất hôm nay
    @Override
    public long getUniqueViewersToday(Long storyId) {
        try {
            String key = RedisKeyConstants.UNIQUE_VIEWERS_PREFIX + storyId + ":" + LocalDate.now();
            Long count = redisTemplate.opsForSet().size(key);
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    // Đếm lượt xem hôm nay
    @Override
    public long getViewsToday(Long storyId) {
        try {
            String key = RedisKeyConstants.DAILY_VIEW_PREFIX + storyId + ":" + LocalDate.now();
            Object count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count.toString()) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    // Đồng bộ lượt xem từ Redis vào database
    @Scheduled(fixedRate = 300000)
    @Transactional
    @Override
    public void syncAllViewsToDatabase() {
        try {
            var stories = storyRepository.findAll();
            for (var story : stories) {
                String key = RedisKeyConstants.VIEW_COUNT_PREFIX + story.getId();
                Object redisViews = redisTemplate.opsForValue().get(key);
                if (redisViews != null) {
                    long views = Long.parseLong(redisViews.toString());
                    story.setTotalViews((int) views);
                    storyRepository.save(story);
                }
            }
        } catch (Exception e) {
            log.error("Error syncing views: {}", e.getMessage());
        }
    }
}
