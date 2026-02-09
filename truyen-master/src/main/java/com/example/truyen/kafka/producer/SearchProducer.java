package com.example.truyen.kafka.producer;

import com.example.truyen.config.KafkaTopicConfig;
import com.example.truyen.dto.event.SearchEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Producer gửi search events vào Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Gửi search event vào Kafka
     */
    public void sendSearchEvent(SearchEvent event) {
        try {
            // Sử dụng query làm key để partition
            String key = event.getQuery() != null ? event.getQuery() : "unknown";

            kafkaTemplate.send(KafkaTopicConfig.SEARCH_QUERIES, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Search event sent successfully: query={}", event.getQuery());
                        } else {
                            log.error("Failed to send search event: query={}, error={}",
                                    event.getQuery(), ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            log.error("Error sending search event: {}", e.getMessage(), e);
        }
    }
}
