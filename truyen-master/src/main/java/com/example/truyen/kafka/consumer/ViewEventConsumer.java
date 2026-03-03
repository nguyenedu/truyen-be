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

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewEventConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StoryRepository storyRepository;

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
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            log.debug("Successfully processed {} view events", events.size());
        } catch (Exception e) {
            log.error("Error processing view events batch: {}", e.getMessage(), e);
        }
    }

    private void processViewEvent(ViewEvent event) {
        try {
            String today = LocalDate.now().toString();
            Long storyId = event.getStoryId();
            Long userId = event.getUserId();

            // 1. Tăng tổng lượt xem tích lũy (dùng chung với StoryViewServiceImpl)
            String totalKey = RedisKeyConstants.STORY_TOTAL_VIEWS + storyId;
            Long totalViews = redisTemplate.opsForValue().increment(totalKey);

            // 2. Tăng lượt xem hôm nay (TTL: 1 ngày)
            String todayKey = RedisKeyConstants.STORY_VIEWS_TODAY + storyId;
            redisTemplate.opsForValue().increment(todayKey);
            redisTemplate.expire(todayKey, Duration.ofDays(1));

            // 3. Tăng lượt xem theo ngày cụ thể (cho trending, TTL: 35 ngày)
            String dateKey = RedisKeyConstants.STORY_VIEWS_DATE + today + ":" + storyId;
            redisTemplate.opsForValue().increment(dateKey);
            redisTemplate.expire(dateKey, Duration.ofDays(35));

            // 4. Track unique viewer hôm nay nếu có userId (TTL: 1 ngày)
            if (userId != null) {
                String uniqueKey = RedisKeyConstants.STORY_UNIQUE_VIEWERS_TODAY + storyId;
                redisTemplate.opsForSet().add(uniqueKey, userId);
                redisTemplate.expire(uniqueKey, Duration.ofDays(1));
            }

            // 5. Fast-path sync: cộng dồn vào MySQL mỗi 100 lượt xem từ Kafka
            // Đồng thời cập nhật STORY_DB_SYNCED_VIEWS để scheduled job không sync lại
            if (totalViews != null && totalViews % 100 == 0) {
                storyRepository.incrementTotalViews(storyId, 100);

                // Cập nhật mốc đã sync để scheduled job tính delta chính xác
                String syncedKey = RedisKeyConstants.STORY_DB_SYNCED_VIEWS + storyId;
                redisTemplate.opsForValue().set(syncedKey, totalViews);

                log.debug("Fast-path: synced 100 views to DB for story {}, total={}", storyId, totalViews);
            }

            log.trace("View event processed for story {}, user {}", storyId, userId);

        } catch (Exception e) {
            log.error("Failed to process view event for story {}: {}", event.getStoryId(), e.getMessage());
            throw e;
        }
    }
}
