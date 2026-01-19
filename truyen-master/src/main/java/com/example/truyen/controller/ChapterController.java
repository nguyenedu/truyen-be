package com.example.truyen.controller;

import com.example.truyen.dto.request.ChapterRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.ChapterResponse;
import com.example.truyen.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    // Lấy tất cả chương của 1 truyện
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> getChaptersByStoryId(
            @PathVariable Long storyId
    ) {
        List<ChapterResponse> chapters = chapterService.getChaptersByStoryId(storyId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chương thành công", chapters));
    }

    // Đọc chương theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChapterResponse>> getChapterById(@PathVariable Long id) {
        ChapterResponse chapter = chapterService.getChapterById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chương thành công", chapter));
    }

    // Đọc chương theo Story ID và Chapter Number
    @GetMapping("/story/{storyId}/number/{chapterNumber}")
    public ResponseEntity<ApiResponse<ChapterResponse>> getChapterByStoryAndNumber(
            @PathVariable Long storyId,
            @PathVariable Integer chapterNumber
    ) {
        ChapterResponse chapter = chapterService.getChapterByStoryAndNumber(storyId, chapterNumber);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chương thành công", chapter));
    }

    // Tạo chương mới (CHỈ ADMIN)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ChapterResponse>> createChapter(
            @Valid @RequestBody ChapterRequest request
    ) {
        ChapterResponse chapter = chapterService.createChapter(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo chương thành công", chapter));
    }

    // Cập nhật chương (CHỈ ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ChapterResponse>> updateChapter(
            @PathVariable Long id,
            @Valid @RequestBody ChapterRequest request
    ) {
        ChapterResponse chapter = chapterService.updateChapter(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chương thành công", chapter));
    }

    // Xóa chương (CHỈ ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteChapter(@PathVariable Long id) {
        chapterService.deleteChapter(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa chương thành công", null));
    }
}