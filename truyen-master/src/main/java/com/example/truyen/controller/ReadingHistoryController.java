package com.example.truyen.controller;

import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.ReadingHistoryResponse;
import com.example.truyen.service.ReadingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reading-history")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReadingHistoryController {

    private final ReadingHistoryService readingHistoryService;

    /**
     * Get the current user's reading history.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReadingHistoryResponse>>> getMyReadingHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Reading history retrieved successfully",
                readingHistoryService.getMyReadingHistory(page, size)));
    }

    /**
     * Get reading history for a specific story.
     */
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<ReadingHistoryResponse>> getReadingHistoryForStory(@PathVariable Long storyId) {
        ReadingHistoryResponse history = readingHistoryService.getReadingHistoryForStory(storyId);
        String message = (history == null) ? "No reading history for this story"
                : "Reading history retrieved successfully";
        return ResponseEntity.ok(ApiResponse.success(message, history));
    }

    /**
     * Save or update reading progress.
     */
    @PostMapping("/story/{storyId}/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<ReadingHistoryResponse>> saveReadingHistory(
            @PathVariable Long storyId,
            @PathVariable Long chapterId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reading progress saved successfully",
                        readingHistoryService.saveReadingHistory(storyId, chapterId)));
    }

    /**
     * Delete reading history for a specific story.
     */
    @DeleteMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<String>> deleteReadingHistory(@PathVariable Long storyId) {
        readingHistoryService.deleteReadingHistory(storyId);
        return ResponseEntity.ok(ApiResponse.success("Story reading history deleted successfully", null));
    }

    /**
     * Clear entire reading history.
     */
    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<String>> deleteAllReadingHistory() {
        readingHistoryService.deleteAllReadingHistory();
        return ResponseEntity.ok(ApiResponse.success("All reading history cleared successfully", null));
    }
}