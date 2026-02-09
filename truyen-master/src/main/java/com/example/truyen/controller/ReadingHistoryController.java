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

    // Lấy lịch sử đọc truyện của người dùng
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReadingHistoryResponse>>> getMyReadingHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Get reading history successfully",
                readingHistoryService.getMyReadingHistory(page, size)));
    }

    // Lấy lịch sử đọc của một truyện cụ thể
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<ReadingHistoryResponse>> getReadingHistoryForStory(@PathVariable Long storyId) {
        ReadingHistoryResponse history = readingHistoryService.getReadingHistoryForStory(storyId);
        String message = (history == null) ? "No reading history for this story"
                : "Get reading history successfully";
        return ResponseEntity.ok(ApiResponse.success(message, history));
    }

    // Lưu hoặc cập nhật tiến độ đọc
    @PostMapping("/story/{storyId}/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<ReadingHistoryResponse>> saveReadingHistory(
            @PathVariable Long storyId,
            @PathVariable Long chapterId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Save reading progress successfully",
                        readingHistoryService.saveReadingHistory(storyId, chapterId)));
    }

    // Xóa lịch sử đọc của một truyện
    @DeleteMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<String>> deleteReadingHistory(@PathVariable Long storyId) {
        readingHistoryService.deleteReadingHistory(storyId);
        return ResponseEntity.ok(ApiResponse.success("Delete reading history successfully", null));
    }

    // Xóa tất cả lịch sử đọc
    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<String>> deleteAllReadingHistory() {
        readingHistoryService.deleteAllReadingHistory();
        return ResponseEntity.ok(ApiResponse.success("Delete all reading history successfully", null));
    }
}