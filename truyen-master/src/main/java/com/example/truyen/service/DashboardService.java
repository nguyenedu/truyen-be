package com.example.truyen.service;

import com.example.truyen.dto.response.DashboardStatsResponse;

// Interface DashboardService
public interface DashboardService {

    // Lấy toàn bộ thống kê dashboard
    DashboardStatsResponse getDashboardStats(String period);
}
