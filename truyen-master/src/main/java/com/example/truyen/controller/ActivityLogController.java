package com.example.truyen.controller;

import com.example.truyen.entity.ActivityLog;
import com.example.truyen.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5174")  // ← Thêm này
@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
// @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")  // ← Comment tạm
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<Page<ActivityLog>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) Long userId
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Nếu có filter, gọi searchLogs
        if (action != null || tableName != null || userId != null) {
            Page<ActivityLog> logs = activityLogService.searchLogs(
                    userId, action, tableName, null, null, pageable
            );
            return ResponseEntity.ok(logs);
        }

        // Không filter thì lấy tất cả
        Page<ActivityLog> logs = activityLogService.getAllLogs(pageable);
        return ResponseEntity.ok(logs);
    }

    //@GetMapping("/user/{userId}")
    //@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")  /
    public ResponseEntity<Page<ActivityLog>> getLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ActivityLog> logs = activityLogService.getLogsByUser(userId, pageable);
        return ResponseEntity.ok(logs);
    }
}