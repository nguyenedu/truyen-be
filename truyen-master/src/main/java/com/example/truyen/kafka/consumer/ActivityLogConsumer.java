package com.example.truyen.kafka.consumer;

import com.example.truyen.config.KafkaTopicConfig;
import com.example.truyen.dto.event.ActivityLogEvent;
import com.example.truyen.entity.ActivityLog;
import com.example.truyen.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumer xử lý Activity Log Events từ Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLogConsumer {

    private final ActivityLogRepository activityLogRepository;

    /**
     * Xử lý activity log events với batch processing
     */
    @KafkaListener(topics = KafkaTopicConfig.ACTIVITY_LOGS, groupId = "activity-log-group", containerFactory = "kafkaListenerContainerFactory", batch = "true")
    @Transactional
    public void consumeActivityLogs(
            @Payload List<ActivityLogEvent> events,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Processing {} activity log events", events.size());

            List<ActivityLog> activityLogs = new ArrayList<>();

            for (ActivityLogEvent event : events) {
                // Map ActivityLogEvent -> ActivityLog entity
                // ActivityLog fields: userId, action, tableName, recordId, description,
                // ipAddress, createdAt
                ActivityLog activityLog = ActivityLog.builder()
                        .userId(event.getUserId())
                        .action(event.getAction())
                        .tableName(event.getEntityType()) // entityType -> tableName
                        .recordId(event.getEntityId()) // entityId -> recordId
                        .description(buildDescription(event))
                        .ipAddress(null) // IP address không có trong event, có thể thêm sau
                        .build();

                activityLogs.add(activityLog);
            }

            // Batch insert vào database
            activityLogRepository.saveAll(activityLogs);

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.debug("Successfully saved {} activity logs", activityLogs.size());

        } catch (Exception e) {
            log.error("Error processing activity log events: {}", e.getMessage(), e);
        }
    }

    /**
     * Build description từ event details
     */
    private String buildDescription(ActivityLogEvent event) {
        StringBuilder desc = new StringBuilder();
        if (event.getUsername() != null) {
            desc.append("User: ").append(event.getUsername()).append(", ");
        }
        if (event.getEntityName() != null) {
            desc.append("Entity: ").append(event.getEntityName()).append(", ");
        }
        if (event.getDetails() != null) {
            desc.append("Details: ").append(event.getDetails());
        }
        return desc.toString();
    }
}
