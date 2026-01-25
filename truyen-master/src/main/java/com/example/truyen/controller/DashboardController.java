package com.example.truyen.controller;

import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.DashboardStatsResponse;
import com.example.truyen.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Lấy thống kê dashboard
     * @param period "week" hoặc "month" (mặc định: month)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(
            @RequestParam(defaultValue = "month") String period
    ) {
        DashboardStatsResponse stats = dashboardService.getDashboardStats(period);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê dashboard thành công", stats));
    }
}