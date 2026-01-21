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
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Integer> {

    // Tìm theo user_id
    Page<ActivityLog> findByUserId(Integer userId, Pageable pageable);

    // Tìm theo action
    Page<ActivityLog> findByAction(String action, Pageable pageable);

    // Tìm theo table_name
    Page<ActivityLog> findByTableName(String tableName, Pageable pageable);

    // Tìm theo khoảng thời gian
    Page<ActivityLog> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    // Tìm theo user và thời gian
    Page<ActivityLog> findByUserIdAndCreatedAtBetween(
            Integer userId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    // Tìm theo user và action
    Page<ActivityLog> findByUserIdAndAction(Integer userId, String action, Pageable pageable);

    // Search với nhiều điều kiện
    @Query("SELECT al FROM ActivityLog al WHERE " +
            "(:userId IS NULL OR al.userId = :userId) AND " +
            "(:action IS NULL OR al.action LIKE %:action%) AND " +
            "(:tableName IS NULL OR al.tableName = :tableName) AND " +
            "(:startDate IS NULL OR al.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR al.createdAt <= :endDate)")
    Page<ActivityLog> searchLogs(
            @Param("userId") Integer userId,
            @Param("action") String action,
            @Param("tableName") String tableName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}