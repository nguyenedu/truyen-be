package com.example.truyen.controller;

import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.FavoriteResponse;
import com.example.truyen.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // Tất cả các API yêu cầu xác thực
public class FavoriteController {

    private final FavoriteService favoriteService;

    // Lấy danh sách truyện yêu thích của tôi
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FavoriteResponse>>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<FavoriteResponse> favorites = favoriteService.getMyFavorites(page, size);
        return ResponseEntity.ok(ApiResponse.success("Get favorite list successfully", favorites));
    }

    // Kiểm tra xem truyện có nằm trong danh sách yêu thích không
    @GetMapping("/check/{storyId}")
    public ResponseEntity<ApiResponse<Boolean>> isFavorite(@PathVariable Long storyId) {
        Boolean isFavorite = favoriteService.isFavorite(storyId);
        return ResponseEntity.ok(ApiResponse.success("Check successfully", isFavorite));
    }

    // Thêm truyện vào danh sách yêu thích
    @PostMapping("/{storyId}")
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(@PathVariable Long storyId) {
        FavoriteResponse favorite = favoriteService.addFavorite(storyId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Added to favorites successfully", favorite));
    }

    // Xóa truyện khỏi danh sách yêu thích
    @DeleteMapping("/{storyId}")
    public ResponseEntity<ApiResponse<String>> removeFavorite(@PathVariable Long storyId) {
        favoriteService.removeFavorite(storyId);
        return ResponseEntity.ok(ApiResponse.success("Removed from favorites successfully", null));
    }

    // Đếm tổng số lượt yêu thích của truyện
    @GetMapping("/count/{storyId}")
    public ResponseEntity<ApiResponse<Long>> countFavorites(@PathVariable Long storyId) {
        Long count = favoriteService.countFavoritesByStoryId(storyId);
        return ResponseEntity.ok(ApiResponse.success("Count favorites successfully", count));
    }
}