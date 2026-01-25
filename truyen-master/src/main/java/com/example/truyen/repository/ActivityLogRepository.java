package com.example.truyen.repository;

import com.example.truyen.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // Tìm theo userId - ĐÃ SỬA: Integer -> Long
    Page<ActivityLog> findByUserId(Long userId, Pageable pageable);

    // Tìm theo action
    Page<ActivityLog> findByAction(String action, Pageable pageable);

    // Tìm theo tableName
    Page<ActivityLog> findByTableName(String tableName, Pageable pageable);

    // Tìm theo khoảng thời gian
    Page<ActivityLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Search với nhiều điều kiện
    @Query("SELECT a FROM ActivityLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:tableName IS NULL OR a.tableName = :tableName) AND " +
            "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR a.createdAt <= :endDate)")
    Page<ActivityLog> searchLogs(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("tableName") String tableName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}