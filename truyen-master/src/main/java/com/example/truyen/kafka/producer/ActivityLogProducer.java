package com.example.truyen.kafka.producer;

import com.example.truyen.config.KafkaTopicConfig;
import com.example.truyen.dto.event.ActivityLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Producer gửi Activity Log Events vào Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLogProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Gửi activity log event vào Kafka
     */
    public void sendActivityLog(ActivityLogEvent event) {
        try {
            // Sử dụng userId làm key để các logs của cùng user vào cùng partition
            String key = event.getUserId() != null ? String.valueOf(event.getUserId()) : "system";

            kafkaTemplate.send(KafkaTopicConfig.ACTIVITY_LOGS, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Activity log sent: {} {} by user {}",
                                    event.getAction(), event.getEntityType(), event.getUsername());
                        } else {
                            log.error("Failed to send activity log: {}", ex.getMessage());
                        }
                    });

        } catch (Exception e) {
            log.error("Error sending activity log event: {}", e.getMessage());
        }
    }
}
