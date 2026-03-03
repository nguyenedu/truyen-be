package com.example.truyen.controller;

import com.example.truyen.dto.request.ChapterRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.ChapterResponse;
import com.example.truyen.dto.response.UnlockedChapterResponse;
import com.example.truyen.service.ChapterAccessService;
import com.example.truyen.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final ChapterAccessService chapterAccessService;

    // Lấy danh sách chương của truyện
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> getChaptersByStoryId(
            @PathVariable Long storyId) {
        List<ChapterResponse> chapters = chapterService.getChaptersByStoryId(storyId);
        return ResponseEntity.ok(ApiResponse.success("Get chapter list successfully", chapters));
    }

    // Lấy chi tiết chương theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChapterResponse>> getChapterById(@PathVariable Long id) {
        ChapterResponse chapter = chapterService.getChapterById(id);
        return ResponseEntity.ok(ApiResponse.success("Get chapter details successfully", chapter));
    }

    // Lấy chương theo ID truyện và số thứ tự chương
    @GetMapping("/story/{storyId}/number/{chapterNumber}")
    public ResponseEntity<ApiResponse<ChapterResponse>> getChapterByStoryAndNumber(
            @PathVariable Long storyId,
            @PathVariable Integer chapterNumber) {
        ChapterResponse chapter = chapterService.getChapterByStoryAndNumber(storyId, chapterNumber);
        return ResponseEntity.ok(ApiResponse.success("Chapter retrieved successfully", chapter));
    }

    // Kiểm tra quyền đọc chương (có thể gọi trước khi mở chapter)
    @GetMapping("/{id}/access")
    public ResponseEntity<ApiResponse<Boolean>> checkAccess(@PathVariable Long id) {
        boolean hasAccess = chapterAccessService.hasAccess(id);
        return ResponseEntity.ok(ApiResponse.success("Access checked", hasAccess));
    }

    // Mở khóa chương bằng xu (yêu cầu đăng nhập)
    @PostMapping("/{id}/unlock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> unlockChapter(@PathVariable Long id) {
        chapterAccessService.unlockChapter(id);
        return ResponseEntity.ok(ApiResponse.success("Chapter unlocked successfully", null));
    }

    // Xem lịch sử chương đã mở khóa của tôi (yêu cầu đăng nhập)
    @GetMapping("/my-unlocked")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<UnlockedChapterResponse>>> getMyUnlockedChapters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accessedAt").descending());
        Page<UnlockedChapterResponse> result = chapterAccessService.getMyUnlockedChapters(pageable);
        return ResponseEntity.ok(ApiResponse.success("Get unlocked chapters successfully", result));
    }

    @GetMapping("/{id}/unlocked-users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<UnlockedChapterResponse>>> getUnlockedUsersByChapter(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accessedAt").descending());
        Page<UnlockedChapterResponse> result = chapterAccessService.getUnlockedUsersByChapterId(id, pageable);
        return ResponseEntity.ok(ApiResponse.success("Get unlocked users successfully", result));
    }

    // Tạo chương mới (Admin, Super Admin)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ChapterResponse>> createChapter(
            @Valid @RequestBody ChapterRequest request) {
        ChapterResponse chapter = chapterService.createChapter(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create new chapter successfully", chapter));
    }

    // Cập nhật chương (Admin, Super Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ChapterResponse>> updateChapter(
            @PathVariable Long id,
            @Valid @RequestBody ChapterRequest request) {
        ChapterResponse chapter = chapterService.updateChapter(id, request);
        return ResponseEntity.ok(ApiResponse.success("Update chapter successfully", chapter));
    }

    // Xóa chương (Admin, Super Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteChapter(@PathVariable Long id) {
        chapterService.deleteChapter(id);
        return ResponseEntity.ok(ApiResponse.success("Delete chapter successfully", null));
    }

}