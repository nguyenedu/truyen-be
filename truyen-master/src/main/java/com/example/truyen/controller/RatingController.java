package com.example.truyen.controller;

import com.example.truyen.dto.request.RatingRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.RatingResponse;
import com.example.truyen.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    /**
     * Đánh giá truyện.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingResponse>> rateStory(@Valid @RequestBody RatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá truyện thành công", ratingService.rateStory(request)));
    }

    /**
     * Cập nhật đánh giá hiện có.
     */
    @PutMapping("/{storyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingResponse>> updateRating(
            @PathVariable Long storyId,
            @Valid @RequestBody RatingRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Cập nhật đánh giá thành công", ratingService.updateRating(storyId, request)));
    }

    /**
     * Xóa đánh giá.
     */
    @DeleteMapping("/{storyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteRating(@PathVariable Long storyId) {
        ratingService.deleteRating(storyId);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công", null));
    }

    /**
     * Lấy đánh giá của người dùng hiện tại cho một truyện.
     */
    @GetMapping("/my/{storyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingResponse>> getMyRatingForStory(@PathVariable Long storyId) {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin đánh giá thành công",
                        ratingService.getMyRatingForStory(storyId)));
    }

    /**
     * Lấy thông tin đánh giá trung bình của truyện.
     */
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStoryRatingInfo(@PathVariable Long storyId) {
        return ResponseEntity.ok(
                ApiResponse.success("Lấy thông tin đánh giá thành công", ratingService.getStoryRatingInfo(storyId)));
    }
}