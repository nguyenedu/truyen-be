package com.example.truyen.service;

// Interface StoryViewService
public interface StoryViewService {

    // Theo dõi lượt xem truyện
    void trackView(Long storyId, String visitorId);

    // Theo dõi lượt xem truyện với userId
    void trackView(Long storyId, Long userId, String visitorId);

    // Lấy lượt xem gần đây trong N ngày
    long getRecentViews(Long storyId, int days);

    // Đếm số người xem duy nhất hôm nay
    long getUniqueViewersToday(Long storyId);

    // Đếm lượt xem hôm nay
    long getViewsToday(Long storyId);

    // Đồng bộ lượt xem từ Redis vào database
    void syncAllViewsToDatabase();
}
