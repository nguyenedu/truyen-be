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

    /**
     * Lưu activity log bất đồng bộ
     * Sử dụng REQUIRES_NEW để đảm bảo log vẫn được lưu khi transaction chính rollback
     */
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

    /**
     * Lưu log đồng bộ (dùng khi cần chắc chắn log được lưu)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLogSync(ActivityLog activityLog) {
        try {
            activityLogRepository.save(activityLog);
        } catch (Exception e) {
            log.error("Lỗi khi lưu activity log: {}", e.getMessage(), e);
        }
    }

    /**
     * Tạo log từ thông tin cơ bản
     */
    public ActivityLog createLog(Integer userId, String action, String tableName,
                                 Integer recordId, Object data, String ipAddress) {
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

    /**
     * Convert object sang JSON string
     */
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

    /**
     * Lấy logs theo user
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getLogsByUser(Integer userId, Pageable pageable) {
        return activityLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Lấy logs theo action
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getLogsByAction(String action, Pageable pageable) {
        return activityLogRepository.findByAction(action, pageable);
    }

    /**
     * Lấy logs theo table
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getLogsByTable(String tableName, Pageable pageable) {
        return activityLogRepository.findByTableName(tableName, pageable);
    }

    /**
     * Lấy logs theo khoảng thời gian
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end,
                                                Pageable pageable) {
        return activityLogRepository.findByCreatedAtBetween(start, end, pageable);
    }

    /**
     * Search logs với nhiều điều kiện
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> searchLogs(Integer userId, String action, String tableName,
                                        LocalDateTime startDate, LocalDateTime endDate,
                                        Pageable pageable) {
        return activityLogRepository.searchLogs(userId, action, tableName,
                startDate, endDate, pageable);
    }

    /**
     * Lấy tất cả logs
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getAllLogs(Pageable pageable) {
        return activityLogRepository.findAll(pageable);
    }
}