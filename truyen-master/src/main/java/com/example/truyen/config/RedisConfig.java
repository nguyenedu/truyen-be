package com.example.truyen.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);

                // Bộ tuần tự hóa cho khóa
                StringRedisSerializer stringSerializer = new StringRedisSerializer();
                template.setKeySerializer(stringSerializer);
                template.setHashKeySerializer(stringSerializer);

                // Custom ObjectMapper for better serialization
                ObjectMapper objectMapper = new ObjectMapper();

                // Register JavaTimeModule for LocalDateTime support
                objectMapper.registerModule(new JavaTimeModule());

                // Disable writing dates as timestamps
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                // Enable default typing for polymorphic deserialization
                BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .build();

                objectMapper.activateDefaultTyping(
                                ptv,
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                JsonTypeInfo.As.PROPERTY);

                // Bộ tuần tự hóa cho giá trị với ObjectMapper tùy chỉnh
                GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
                                objectMapper);

                template.setValueSerializer(jsonSerializer);
                template.setHashValueSerializer(jsonSerializer);

                template.afterPropertiesSet();
                return template;
        }
}