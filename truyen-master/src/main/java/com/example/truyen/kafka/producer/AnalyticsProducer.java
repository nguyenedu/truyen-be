package com.example.truyen.kafka.producer;

import com.example.truyen.config.KafkaTopicConfig;
import com.example.truyen.dto.event.AnalyticsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// Producer gửi Analytics Events vào Kafka
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Gửi analytics event vào Kafka
    public void sendAnalyticsEvent(AnalyticsEvent event) {
        try {
            // Sử dụng storyId làm key để analytics của cùng story vào cùng partition
            String key = event.getStoryId() != null ? String.valueOf(event.getStoryId()) : "general";

            kafkaTemplate.send(KafkaTopicConfig.ANALYTICS_EVENTS, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Analytics event sent: {} for story {}",
                                    event.getEventType(), event.getStoryId());
                        } else {
                            log.error("Failed to send analytics event: {}", ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            log.error("Error sending analytics event: {}", e.getMessage());
        }
    }
}
