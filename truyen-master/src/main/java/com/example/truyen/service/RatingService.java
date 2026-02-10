package com.example.truyen.service;

import com.example.truyen.dto.request.RatingRequest;
import com.example.truyen.dto.response.RatingResponse;

// Interface RatingService
public interface RatingService {

    // Đánh giá truyện
    RatingResponse rateStory(RatingRequest request);

    // Cập nhật đánh giá
    RatingResponse updateRating(Long storyId, RatingRequest request);

    // Xóa đánh giá
    void deleteRating(Long storyId);

    // Lấy đánh giá của người dùng hiện tại cho truyện
    RatingResponse getMyRatingForStory(Long storyId);

    // Lấy thông tin đánh giá tổng hợp của truyện
    RatingResponse getStoryRatingInfo(Long storyId);
}