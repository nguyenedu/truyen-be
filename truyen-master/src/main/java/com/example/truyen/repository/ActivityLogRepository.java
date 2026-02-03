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


    Page<ActivityLog> findByUserId(Long userId, Pageable pageable);


    Page<ActivityLog> findByAction(String action, Pageable pageable);


    Page<ActivityLog> findByTableName(String tableName, Pageable pageable);


    Page<ActivityLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);


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