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
     * Rate a story.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingResponse>> rateStory(@Valid @RequestBody RatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Story rated successfully", ratingService.rateStory(request)));
    }

    /**
     * Update an existing rating.
     */
    @PutMapping("/{storyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingResponse>> updateRating(
            @PathVariable Long storyId,
            @Valid @RequestBody RatingRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Rating updated successfully", ratingService.updateRating(storyId, request)));
    }

    /**
     * Delete a rating.
     */
    @DeleteMapping("/{storyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteRating(@PathVariable Long storyId) {
        ratingService.deleteRating(storyId);
        return ResponseEntity.ok(ApiResponse.success("Rating deleted successfully", null));
    }

    /**
     * Get the current user's rating for a story.
     */
    @GetMapping("/my/{storyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RatingResponse>> getMyRatingForStory(@PathVariable Long storyId) {
        return ResponseEntity
                .ok(ApiResponse.success("Rating retrieved successfully", ratingService.getMyRatingForStory(storyId)));
    }

    /**
     * Get average rating information for a story.
     */
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStoryRatingInfo(@PathVariable Long storyId) {
        return ResponseEntity.ok(
                ApiResponse.success("Rating info retrieved successfully", ratingService.getStoryRatingInfo(storyId)));
    }
}