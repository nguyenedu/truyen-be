package com.example.truyen.service;

import com.example.truyen.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

// Interface ActivityLogService
public interface ActivityLogService {

        // Ghi lại hoạt động vào Kafka (có fallback save DB)
        void logActivity(String action, String entityType, Long entityId,
                        String entityName, Long userId, String username, String details);

        // Lấy nhật ký theo người dùng
        Page<ActivityLog> getLogsByUser(Long userId, Pageable pageable);

        // Lấy nhật ký theo hành động
        Page<ActivityLog> getLogsByAction(String action, Pageable pageable);

        // Lấy nhật ký theo bảng
        Page<ActivityLog> getLogsByTable(String tableName, Pageable pageable);

        // Lấy nhật ký theo khoảng thời gian
        Page<ActivityLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable);

        // Tìm kiếm nhật ký nâng cao
        Page<ActivityLog> searchLogs(Long userId, String action, String tableName,
                        LocalDateTime startDate, LocalDateTime endDate,
                        Pageable pageable);

        // Lấy tất cả nhật ký
        Page<ActivityLog> getAllLogs(Pageable pageable);
}
