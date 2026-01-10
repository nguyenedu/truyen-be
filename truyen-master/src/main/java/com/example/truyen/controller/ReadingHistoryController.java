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

    // Lấy lịch sử đọc của mình
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReadingHistoryResponse>>> getMyReadingHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReadingHistoryResponse> histories = readingHistoryService.getMyReadingHistory(page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đọc thành công", histories));
    }

    // Lấy lịch sử đọc của 1 truyện
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<ReadingHistoryResponse>> getReadingHistoryForStory(
            @PathVariable Long storyId
    ) {
        ReadingHistoryResponse history = readingHistoryService.getReadingHistoryForStory(storyId);
        if (history == null) {
            return ResponseEntity.ok(ApiResponse.success("Chưa có lịch sử đọc truyện này", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đọc thành công", history));
    }

    // Lưu/Cập nhật lịch sử đọc
    @PostMapping("/story/{storyId}/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<ReadingHistoryResponse>> saveReadingHistory(
            @PathVariable Long storyId,
            @PathVariable Long chapterId
    ) {
        ReadingHistoryResponse history = readingHistoryService.saveReadingHistory(storyId, chapterId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lưu lịch sử đọc thành công", history));
    }

    // Xóa lịch sử đọc của 1 truyện
    @DeleteMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<String>> deleteReadingHistory(@PathVariable Long storyId) {
        readingHistoryService.deleteReadingHistory(storyId);
        return ResponseEntity.ok(ApiResponse.success("Xóa lịch sử đọc thành công", null));
    }

    // Xóa toàn bộ lịch sử đọc
    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<String>> deleteAllReadingHistory() {
        readingHistoryService.deleteAllReadingHistory();
        return ResponseEntity.ok(ApiResponse.success("Xóa toàn bộ lịch sử đọc thành công", null));
    }
}