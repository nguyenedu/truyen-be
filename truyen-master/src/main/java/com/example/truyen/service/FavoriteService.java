package com.example.truyen.service;

import com.example.truyen.dto.response.FavoriteResponse;
import org.springframework.data.domain.Page;

// Interface FavoriteService
public interface FavoriteService {

    // Lấy danh sách truyện yêu thích của người dùng
    Page<FavoriteResponse> getMyFavorites(int page, int size);

    // Kiểm tra truyện có trong danh sách yêu thích không
    boolean isFavorite(Long storyId);

    // Thêm truyện vào danh sách yêu thích
    FavoriteResponse addFavorite(Long storyId);

    // Xóa truyện khỏi danh sách yêu thích
    void removeFavorite(Long storyId);

    // Đếm số lượt yêu thích của truyện
    Long countFavoritesByStoryId(Long storyId);
}