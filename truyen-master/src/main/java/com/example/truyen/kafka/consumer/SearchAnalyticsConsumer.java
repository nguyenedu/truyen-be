package com.example.truyen.kafka.consumer;

import com.example.truyen.config.KafkaTopicConfig;
import com.example.truyen.config.RedisKeyConstants;
import com.example.truyen.dto.event.SearchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Consumer xử lý Search Analytics Events từ Kafka
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    // Xử lý search event
    @KafkaListener(topics = KafkaTopicConfig.SEARCH_QUERIES, groupId = "search-analytics-group", containerFactory = "kafkaListenerContainerFactory", batch = "true")
    public void consumeSearchEvents(
            @Payload List<SearchEvent> events,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Processing {} search events", events.size());

            for (SearchEvent event : events) {
                String query = normalizeQuery(event.getQuery());

                if (query == null || query.trim().isEmpty()) {
                    continue;
                }

                // 1. Cập nhật tìm kiếm phổ biến (toàn thời gian)
                redisTemplate.opsForZSet()
                        .incrementScore(RedisKeyConstants.SEARCH_POPULAR, query, 1.0);

                // 2. Cập nhật tìm kiếm xu hướng (hàng ngày)
                String trendingKey = RedisKeyConstants.SEARCH_TRENDING + LocalDate.now();
                redisTemplate.opsForZSet()
                        .incrementScore(trendingKey, query, 1.0);
                redisTemplate.expire(trendingKey, 7, TimeUnit.DAYS);

                // 3. Cập nhật lịch sử tìm kiếm người dùng
                if (event.getUserId() != null) {
                    String userKey = RedisKeyConstants.SEARCH_USER_HISTORY + event.getUserId();
                    double timestamp = System.currentTimeMillis() / 1000.0;
                    redisTemplate.opsForZSet()
                            .add(userKey, query, timestamp);
                    redisTemplate.expire(userKey, 90, TimeUnit.DAYS);

                    // Giữ tối đa 50 tìm kiếm gần nhất
                    Long size = redisTemplate.opsForZSet().size(userKey);
                    if (size != null && size > 50) {
                        redisTemplate.opsForZSet().removeRange(userKey, 0, size - 51);
                    }
                }

                // 4. Theo dõi tỷ lệ nhấp (nếu có click)
                if (event.getClickedStoryId() != null) {
                    String ctrKey = "search:ctr:" + query;
                    redisTemplate.opsForValue().increment(ctrKey);
                    redisTemplate.expire(ctrKey, 30, TimeUnit.DAYS);
                }
            }

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.debug("Successfully processed {} search events", events.size());

        } catch (Exception e) {
            log.error("Error processing search events: {}", e.getMessage(), e);
        }
    }

    // Chuẩn hóa truy vấn: chữ thường, cắt khoảng trắng thừa
    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        return query.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ");
    }
}
