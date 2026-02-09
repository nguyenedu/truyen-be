package com.example.truyen.kafka.consumer;

import com.example.truyen.config.KafkaTopicConfig;
import com.example.truyen.config.RedisKeyConstants;
import com.example.truyen.dto.event.AnalyticsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Consumer xử lý Analytics Events từ Kafka
 * Cập nhật trending scores và analytics data trong Redis
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

    /**
     * Xử lý analytics events để cập nhật trending scores
     */
    @KafkaListener(topics = KafkaTopicConfig.ANALYTICS_EVENTS, groupId = "analytics-group", containerFactory = "kafkaListenerContainerFactory", batch = "true")
    public void consumeAnalyticsEvents(
            @Payload List<AnalyticsEvent> events,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Processing {} analytics events", events.size());

            for (AnalyticsEvent event : events) {
                processAnalyticsEvent(event);
            }

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.debug("Successfully processed {} analytics events", events.size());

        } catch (Exception e) {
            log.error("Error processing analytics events: {}", e.getMessage(), e);
        }
    }

    /**
     * Xử lý từng analytics event
     */
    private void processAnalyticsEvent(AnalyticsEvent event) {
        try {
            Long storyId = event.getStoryId();
            if (storyId == null)
                return;

            String eventType = event.getEventType();
            String hour = LocalDateTime.now().format(HOUR_FORMATTER);

            // Tăng counter cho từng loại event theo giờ
            String hourlyKey = "analytics:hourly:" + hour + ":" + eventType + ":" + storyId;
            redisTemplate.opsForValue().increment(hourlyKey, 1);
            redisTemplate.expire(hourlyKey, Duration.ofDays(7));

            // Cập nhật trending score (weighted scoring)
            updateTrendingScore(storyId, eventType);

            // Track category analytics nếu có
            if (event.getCategoryId() != null) {
                String categoryKey = "analytics:category:" + event.getCategoryId() + ":" + eventType;
                redisTemplate.opsForValue().increment(categoryKey, 1);
                redisTemplate.expire(categoryKey, Duration.ofDays(30));
            }

        } catch (Exception e) {
            log.error("Failed to process analytics event: {}", e.getMessage());
        }
    }

    /**
     * Cập nhật trending score với weighted scoring
     * VIEW: 1 điểm, FAVORITE: 5 điểm, RATING: 3 điểm, COMMENT: 4 điểm
     */
    private void updateTrendingScore(Long storyId, String eventType) {
        try {
            double score = switch (eventType) {
                case "STORY_VIEW" -> 1.0;
                case "FAVORITE" -> 5.0;
                case "RATING" -> 3.0;
                case "COMMENT" -> 4.0;
                default -> 0.0;
            };

            if (score > 0) {
                // Cập nhật trending score trong Redis Sorted Set
                String trendingKey = RedisKeyConstants.TRENDING_STORIES;
                redisTemplate.opsForZSet().incrementScore(trendingKey, storyId, score);

                // Set expiry cho trending data (7 ngày)
                redisTemplate.expire(trendingKey, Duration.ofDays(7));

                log.trace("Updated trending score for story {}: +{} points", storyId, score);
            }

        } catch (Exception e) {
            log.error("Failed to update trending score for story {}: {}", storyId, e.getMessage());
        }
    }
}
