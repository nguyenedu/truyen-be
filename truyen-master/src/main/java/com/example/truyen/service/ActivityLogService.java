package com.example.truyen.service;

import com.example.truyen.dto.event.ActivityLogEvent;
import com.example.truyen.entity.ActivityLog;
import com.example.truyen.kafka.producer.ActivityLogProducer;
import com.example.truyen.repository.ActivityLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogProducer activityLogProducer;
    private final ObjectMapper objectMapper;

    /**
     * Gửi activity log event vào Kafka (non-blocking).
     * Thay thế cho saveLog async method.
     */
    public void logActivity(String action, String entityType, Long entityId,
            String entityName, Long userId, String username, String details) {
        try {
            ActivityLogEvent event = ActivityLogEvent.create(
                    action, entityType, entityId, entityName, userId, username, details);
            activityLogProducer.sendActivityLog(event);
            log.debug("Activity log event sent to Kafka: {} {}", action, entityType);
        } catch (Exception e) {
            log.error("Failed to send activity log event: {}", e.getMessage());
        }
    }

    /**
     * Chuyển đổi đối tượng sang định dạng chuỗi JSON.
     */
    public String convertObjectToJson(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert object to JSON: {}", e.getMessage());
            return obj.toString();
        }
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> getLogsByUser(Long userId, Pageable pageable) {
        return activityLogRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> getLogsByAction(String action, Pageable pageable) {
        return activityLogRepository.findByAction(action, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> getLogsByTable(String tableName, Pageable pageable) {
        return activityLogRepository.findByTableName(tableName, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end,
            Pageable pageable) {
        return activityLogRepository.findByCreatedAtBetween(start, end, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> searchLogs(Long userId, String action, String tableName,
            LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable) {
        return activityLogRepository.searchLogs(userId, action, tableName,
                startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> getAllLogs(Pageable pageable) {
        return activityLogRepository.findAll(pageable);
    }
}
