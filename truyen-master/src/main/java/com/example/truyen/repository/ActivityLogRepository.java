package com.example.truyen.repository;

import com.example.truyen.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // Tìm theo user
    Page<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Tìm theo action
    Page<ActivityLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    // Tìm theo table
    Page<ActivityLog> findByTableNameOrderByCreatedAtDesc(String tableName, Pageable pageable);

    // Tìm theo table và record ID
    Page<ActivityLog> findByTableNameAndRecordIdOrderByCreatedAtDesc(
            String tableName, Integer recordId, Pageable pageable
    );

    // Tìm theo khoảng thời gian
    Page<ActivityLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end, Pageable pageable
    );

    // Tìm theo user và action
    Page<ActivityLog> findByUserIdAndActionOrderByCreatedAtDesc(
            Long userId, String action, Pageable pageable
    );

    // Xóa logs cũ
    @Modifying
    @Query("DELETE FROM ActivityLog a WHERE a.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Thống kê theo action
    @Query("SELECT a.action, COUNT(a) FROM ActivityLog a " +
            "WHERE a.createdAt BETWEEN :start AND :end " +
            "GROUP BY a.action")
    List<Object[]> countByActionBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Thống kê theo user
    @Query("SELECT a.user.username, COUNT(a) FROM ActivityLog a " +
            "WHERE a.createdAt BETWEEN :start AND :end " +
            "GROUP BY a.user.username " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> countByUserBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Đếm total logs
    long countByUserId(Long userId);
}