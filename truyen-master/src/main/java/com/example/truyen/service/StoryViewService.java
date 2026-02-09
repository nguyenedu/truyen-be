package com.example.truyen.service;

import com.example.truyen.config.RedisKeyConstants;
import com.example.truyen.dto.event.AnalyticsEvent;
import com.example.truyen.dto.event.ViewEvent;
import com.example.truyen.entity.Story;
import com.example.truyen.kafka.producer.AnalyticsProducer;
import com.example.truyen.kafka.producer.ViewEventProducer;
import com.example.truyen.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryViewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StoryRepository storyRepository;
    private final ViewEventProducer viewEventProducer;
    private final AnalyticsProducer analyticsProducer;

    // Gửi sự kiện xem tới Kafka để xử lý
    public void trackView(Long storyId, Long userId, String ipAddress) {
        try {
            // Tạo session ID
            String sessionId = UUID.randomUUID().toString();

            // Tạo sự kiện lượt xem
            ViewEvent viewEvent = ViewEvent.create(storyId, userId, ipAddress, sessionId);

            // Send to Kafka
            viewEventProducer.sendViewEvent(viewEvent);

            log.debug("View event sent to Kafka for story ID: {} by user ID: {}", storyId, userId);

        } catch (Exception e) {
            log.error("Failed to send view event to Kafka for story ID: {}: {}", storyId, e.getMessage());
        }
    }

    // Đếm lượt xem trong N ngày gần đây
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

    // Đếm số người xem duy nhất trong ngày
    public Long getUniqueViewersToday(Long storyId) {
        String key = RedisKeyConstants.STORY_UNIQUE_VIEWERS_TODAY + storyId;
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0L;
    }

    // Lấy tổng lượt xem trong ngày
    public Long getViewsToday(Long storyId) {
        String key = RedisKeyConstants.STORY_VIEWS_TODAY + storyId;
        Object views = redisTemplate.opsForValue().get(key);
        return views != null ? Long.parseLong(views.toString()) : 0L;
    }

    // Đồng bộ lượt xem từ Redis vào cơ sở dữ liệu
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
