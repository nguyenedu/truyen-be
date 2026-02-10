package com.example.truyen.service.impl;

import com.example.truyen.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface ActivityLogServiceImpl {
    // Ghi lại hoạt động vào Kafka
    // Ghi lại hoạt động vào Kafka (có fallback save DB)
    void logActivity(String action, String entityType, Long entityId,
                     String entityName, Long userId, String username, String details);

    // Chuyển đổi đối tượng sang chuỗi JSON
    String convertObjectToJson(Object obj);

    // Lấy nhật ký theo người dùng
    @Transactional(readOnly = true)
    Page<ActivityLog> getLogsByUser(Long userId, Pageable pageable);

    // Lấy nhật ký theo hành động
    @Transactional(readOnly = true)
    Page<ActivityLog> getLogsByAction(String action, Pageable pageable);

    // Lấy nhật ký theo bảng
    @Transactional(readOnly = true)
    Page<ActivityLog> getLogsByTable(String tableName, Pageable pageable);

    // Lấy nhật ký theo khoảng thời gian
    @Transactional(readOnly = true)
    Page<ActivityLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end,
                                         Pageable pageable);

    // Tìm kiếm nhật ký nâng cao
    @Transactional(readOnly = true)
    Page<ActivityLog> searchLogs(Long userId, String action, String tableName,
                                 LocalDateTime startDate, LocalDateTime endDate,
                                 Pageable pageable);

    // Lấy tất cả nhật ký
    @Transactional(readOnly = true)
    Page<ActivityLog> getAllLogs(Pageable pageable);
}
