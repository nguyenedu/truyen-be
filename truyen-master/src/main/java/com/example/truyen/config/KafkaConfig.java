package com.example.truyen.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình Kafka Producer và Consumer
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Cấu hình Producer Factory với JSON serialization
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        // Bootstrap servers
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serialization
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Performance & Reliability
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Nén dữ liệu
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // Batch size 16KB
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Đợi 10ms để batch
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB buffer

        // Idempotence - đảm bảo không gửi trùng
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all"); // Đợi tất cả replicas acknowledge
        config.put(ProducerConfig.RETRIES_CONFIG, 3); // Retry 3 lần nếu fail

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Kafka Template để gửi messages
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Cấu hình Consumer Factory với JSON deserialization
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

        // Bootstrap servers
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Deserialization
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // JSON Deserializer settings
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.truyen.dto.event");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.example.truyen.dto.event.ViewEvent");

        // Consumer settings
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Đọc từ đầu nếu chưa có offset
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit để đảm bảo xử lý thành công
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100); // Lấy tối đa 100 records mỗi lần poll

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Container Factory cho Kafka Listener
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // 3 consumer threads
        factory.getContainerProperties().setPollTimeout(3000); // Poll timeout 3 giây

        return factory;
    }
}
