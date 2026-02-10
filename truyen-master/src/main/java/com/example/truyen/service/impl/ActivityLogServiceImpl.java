package com.example.truyen.service.impl;

import com.example.truyen.dto.event.ActivityLogEvent;
import com.example.truyen.entity.ActivityLog;
import com.example.truyen.kafka.producer.ActivityLogProducer;
import com.example.truyen.repository.ActivityLogRepository;
import com.example.truyen.service.ActivityLogService;

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
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogProducer activityLogProducer;

    // Ghi lại hoạt động vào Kafka
    @Override
    public void logActivity(String action, String entityType, Long entityId,
            String entityName, Long userId, String username, String details) {

        ActivityLogEvent event = ActivityLogEvent.create(
                action, entityType, entityId, entityName, userId, username, details);

        try {
            activityLogProducer.sendActivityLog(event);
            log.debug("Activity log event sent to Kafka: {} {}", action, entityType);
        } catch (Exception e) {
            log.error("Failed to send activity log event to Kafka: {}. Fallback to direct DB save.", e.getMessage());
            saveLogDirectly(event);
        }
    }

    private void saveLogDirectly(ActivityLogEvent event) {
        try {
            ActivityLog activityLog = ActivityLog.builder()
                    .userId(event.getUserId())
                    .action(event.getAction())
                    .tableName(event.getEntityType())
                    .recordId(event.getEntityId())
                    .description(buildDescription(event))
                    .ipAddress(null)
                    .build();

            activityLogRepository.save(activityLog);
            log.info("Activity log saved directly to DB (Fallback): {}", event.getAction());
        } catch (Exception e) {
            log.error("Failed to save activity log fallback: {}", e.getMessage());
        }
    }

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

    // Lấy nhật ký theo người dùng
    @Transactional(readOnly = true)
    @Override
    public Page<ActivityLog> getLogsByUser(Long userId, Pageable pageable) {
        return activityLogRepository.findByUserId(userId, pageable);
    }

    // Lấy nhật ký theo hành động
    @Transactional(readOnly = true)
    @Override
    public Page<ActivityLog> getLogsByAction(String action, Pageable pageable) {
        return activityLogRepository.findByAction(action, pageable);
    }

    // Lấy nhật ký theo bảng
    @Transactional(readOnly = true)
    @Override
    public Page<ActivityLog> getLogsByTable(String tableName, Pageable pageable) {
        return activityLogRepository.findByTableName(tableName, pageable);
    }

    // Lấy nhật ký theo khoảng thời gian
    @Transactional(readOnly = true)
    @Override
    public Page<ActivityLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end,
            Pageable pageable) {
        return activityLogRepository.findByCreatedAtBetween(start, end, pageable);
    }

    // Tìm kiếm nhật ký nâng cao
    @Transactional(readOnly = true)
    @Override
    public Page<ActivityLog> searchLogs(Long userId, String action, String tableName,
            LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable) {
        return activityLogRepository.searchLogs(userId, action, tableName,
                startDate, endDate, pageable);
    }

    // Lấy tất cả nhật ký
    @Transactional(readOnly = true)
    @Override
    public Page<ActivityLog> getAllLogs(Pageable pageable) {
        return activityLogRepository.findAll(pageable);
    }
}
