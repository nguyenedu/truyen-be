package com.example.truyen.kafka.producer;

import com.example.truyen.config.KafkaTopicConfig;
import com.example.truyen.dto.event.ViewEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer gửi View Events vào Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ViewEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Gửi view event vào Kafka topic
     * 
     * @param viewEvent Event cần gửi
     */
    public void sendViewEvent(ViewEvent viewEvent) {
        try {
            // Sử dụng storyId làm key để đảm bảo các events của cùng 1 story vào cùng
            // partition
            String key = String.valueOf(viewEvent.getStoryId());

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate
                    .send(KafkaTopicConfig.STORY_VIEW_EVENTS, key, viewEvent);

            // Callback xử lý success/failure
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("View event sent successfully for story ID: {} to partition: {}",
                            viewEvent.getStoryId(),
                            result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to send view event for story ID: {}: {}",
                            viewEvent.getStoryId(),
                            ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Error sending view event for story ID: {}: {}",
                    viewEvent.getStoryId(),
                    e.getMessage());
        }
    }
}
