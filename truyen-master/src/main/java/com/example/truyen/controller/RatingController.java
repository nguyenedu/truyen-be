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

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // Đánh giá truyện
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingResponse>> rateStory(@Valid @RequestBody RatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rate story successfully", ratingService.rateStory(request)));
    }

    // Cập nhật đánh giá
    @PutMapping("/{storyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingResponse>> updateRating(
            @PathVariable Long storyId,
            @Valid @RequestBody RatingRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Update rating successfully", ratingService.updateRating(storyId, request)));
    }

    // Xóa đánh giá
    @DeleteMapping("/{storyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteRating(@PathVariable Long storyId) {
        ratingService.deleteRating(storyId);
        return ResponseEntity.ok(ApiResponse.success("Delete rating successfully", null));
    }

    // Lấy đánh giá của tôi về truyện
    @GetMapping("/my/{storyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingResponse>> getMyRatingForStory(@PathVariable Long storyId) {
        return ResponseEntity
                .ok(ApiResponse.success("Get rating info successfully",
                        ratingService.getMyRatingForStory(storyId)));
    }

    // Lấy thống kê đánh giá của truyện
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<RatingResponse>> getStoryRatingInfo(@PathVariable Long storyId) {
        return ResponseEntity.ok(
                ApiResponse.success("Get rating info successfully", ratingService.getStoryRatingInfo(storyId)));
    }
}