package com.example.truyen.kafka.consumer;

import com.example.truyen.config.KafkaTopicConfig;
import com.example.truyen.config.RedisKeyConstants;
import com.example.truyen.dto.event.ViewEvent;
import com.example.truyen.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Consumer xử lý View Events từ Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ViewEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StoryRepository storyRepository;

    /**
     * Xử lý view events từ Kafka topic
     * Sử dụng batch processing để tối ưu performance
     */
    @KafkaListener(topics = KafkaTopicConfig.STORY_VIEW_EVENTS, groupId = "view-tracking-group", containerFactory = "kafkaListenerContainerFactory", batch = "true")
    @Transactional
    public void consumeViewEvents(
            @Payload List<ViewEvent> events,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Processing {} view events from partitions: {}", events.size(), partitions);

            for (ViewEvent event : events) {
                processViewEvent(event);
            }

            // Manual commit sau khi xử lý thành công
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.debug("Successfully processed {} view events", events.size());

        } catch (Exception e) {
            log.error("Error processing view events batch: {}", e.getMessage(), e);
            // Không acknowledge để Kafka retry
        }
    }

    /**
     * Xử lý từng view event
     */
    private void processViewEvent(ViewEvent event) {
        try {
            String today = LocalDate.now().toString();
            Long storyId = event.getStoryId();
            Long userId = event.getUserId();

            // 1. Tăng lượng views trong ngày
            String viewKey = RedisKeyConstants.STORY_VIEWS_TODAY + storyId;
            Long currentViews = redisTemplate.opsForValue().increment(viewKey, 1);
            redisTemplate.expire(viewKey, Duration.ofDays(1));

            // 2. Đếm số lượng user đã xem trong ngày (nếu có userId)
            if (userId != null) {
                String viewerKey = RedisKeyConstants.STORY_UNIQUE_VIEWERS_TODAY + storyId;
                redisTemplate.opsForSet().add(viewerKey, userId);
                redisTemplate.expire(viewerKey, Duration.ofDays(1));
            }

            // 3. Tăng lượt view theo ngày
            String dateViewKey = RedisKeyConstants.STORY_VIEWS_DATE + today + ":" + storyId;
            redisTemplate.opsForValue().increment(dateViewKey, 1);
            redisTemplate.expire(dateViewKey, Duration.ofDays(35));

            // 4. Đồng bộ hóa với database sau mỗi 100 lượt xem (tối ưu hơn 10)
            if (currentViews != null && currentViews % 100 == 0) {
                storyRepository.incrementTotalViews(storyId, 100);
                log.debug("Synced 100 views to database for story ID: {}", storyId);
            }

            log.trace("View event processed for story ID: {} by user ID: {}", storyId, userId);

        } catch (Exception e) {
            log.error("Failed to process view event for story ID: {}: {}",
                    event.getStoryId(), e.getMessage());
            throw e;
        }
    }
}
