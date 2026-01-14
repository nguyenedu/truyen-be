package com.example.truyen.controller;

import com.example.truyen.dto.request.StoryRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.StoryResponse;
import com.example.truyen.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    // Lấy tất cả truyện
    @GetMapping
    public ResponseEntity<ApiResponse<Page<StoryResponse>>> getAllStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<StoryResponse> stories = storyService.getAllStories(page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách truyện thành công", stories));
    }

    // Lấy chi tiết 1 truyện
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoryResponse>> getStoryById(@PathVariable Long id) {
        StoryResponse story = storyService.getStoryById(id);
        // Tăng lượt xem
        storyService.increaseView(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin truyện thành công", story));
    }

    // Tìm kiếm truyện theo tiêu đề
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<StoryResponse>>> searchStories(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<StoryResponse> stories = storyService.searchStories(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm thành công", stories));
    }

    // Lấy truyện theo thể loại
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<StoryResponse>>> getStoriesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<StoryResponse> stories = storyService.getStoriesByCategory(categoryId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy truyện theo thể loại thành công", stories));
    }

    // Lấy truyện HOT
    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<Page<StoryResponse>>> getHotStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<StoryResponse> stories = storyService.getHotStories(page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy truyện HOT thành công", stories));
    }

    // Lấy truyện mới nhất
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<Page<StoryResponse>>> getLatestStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<StoryResponse> stories = storyService.getLatestStories(page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy truyện mới nhất thành công", stories));
    }

    // Tạo truyện mới
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoryResponse>> createStory(
            @Valid @RequestBody StoryRequest request
    ) {
        StoryResponse story = storyService.createStory(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo truyện thành công", story));
    }

    // Cập nhật truyện
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoryResponse>> updateStory(
            @PathVariable Long id,
            @Valid @RequestBody StoryRequest request
    ) {
        StoryResponse story = storyService.updateStory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật truyện thành công", story));
    }

    // Xóa truyện
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteStory(@PathVariable Long id) {
        storyService.deleteStory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa truyện thành công", null));
    }
}