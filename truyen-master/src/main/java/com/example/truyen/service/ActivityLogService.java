package com.example.truyen.service;

import com.example.truyen.entity.ActivityLog;
import com.example.truyen.repository.ActivityLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(ActivityLog activityLog) {
        try {
            activityLogRepository.save(activityLog);
            log.debug("Đã lưu activity log: {}", activityLog.getAction());
        } catch (Exception e) {
            log.error("Lỗi khi lưu activity log: {}", e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLogSync(ActivityLog activityLog) {
        try {
            activityLogRepository.save(activityLog);
        } catch (Exception e) {
            log.error("Lỗi khi lưu activity log: {}", e.getMessage(), e);
        }
    }

    // ĐÃ SỬA: Integer -> Long
    public ActivityLog createLog(Long userId, String action, String tableName,
                                 Long recordId, Object data, String ipAddress) {
        String description = convertObjectToJson(data);

        return ActivityLog.builder()
                .userId(userId)
                .action(action)
                .tableName(tableName)
                .recordId(recordId)
                .description(description)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public String convertObjectToJson(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Không thể convert object sang JSON: {}", e.getMessage());
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