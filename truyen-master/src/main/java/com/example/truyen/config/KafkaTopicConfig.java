package com.example.truyen.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Cấu hình các Kafka Topics
 */
@Configuration
public class KafkaTopicConfig {

    // Topic names
    public static final String STORY_VIEW_EVENTS = "story.view.events";
    public static final String ACTIVITY_LOGS = "activity.logs";
    public static final String ANALYTICS_EVENTS = "analytics.events";

    /**
     * Topic cho view tracking events
     * - Partitions: 3 để tăng throughput
     * - Replication: 2 để đảm bảo high availability
     */
    @Bean
    public NewTopic storyViewEventsTopic() {
        return TopicBuilder.name(STORY_VIEW_EVENTS)
                .partitions(3)
                .replicas(2)
                .compact() // Cleanup policy: compact để giữ latest state
                .build();
    }

    /**
     * Topic cho activity logs
     * - Partitions: 2
     * - Replication: 2
     */
    @Bean
    public NewTopic activityLogsTopic() {
        return TopicBuilder.name(ACTIVITY_LOGS)
                .partitions(2)
                .replicas(2)
                .build();
    }

    /**
     * Topic cho analytics events
     * - Partitions: 3 để xử lý song song
     * - Replication: 2
     */
    @Bean
    public NewTopic analyticsEventsTopic() {
        return TopicBuilder.name(ANALYTICS_EVENTS)
                .partitions(3)
                .replicas(2)
                .build();
    }
}
