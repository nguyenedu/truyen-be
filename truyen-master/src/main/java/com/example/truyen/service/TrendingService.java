package com.example.truyen.service;

import com.example.truyen.dto.response.StoryTrendingDTO;
import com.example.truyen.entity.Ranking;
import com.example.truyen.entity.Story;

import java.util.List;

// Interface TrendingService
public interface TrendingService {

    // Tính toán điểm xu hướng của truyện
    double calculateTrendingScore(Story story, int days);

    // Làm mới xu hướng HÀNG NGÀY
    void refreshDailyTrending();

    // Làm mới xu hướng HÀNG TUẦN
    void refreshWeeklyTrending();

    // Làm mới xu hướng HÀNG THÁNG
    void refreshMonthlyTrending();

    // Lấy danh sách xu hướng (ưu tiên Redis, fallback DB)
    List<StoryTrendingDTO> getTrending(Ranking.RankingType rankingType, int limit);

    // Trigger làm mới thủ công
    void manualRefresh(Ranking.RankingType rankingType);

    // Xóa bảng xếp hạng lịch sử cũ hơn 90 ngày
    void cleanupOldRankings();
}
