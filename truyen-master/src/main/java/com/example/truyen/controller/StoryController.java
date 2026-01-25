package com.example.truyen.controller;

import com.example.truyen.dto.request.StoryRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.StoryResponse;
import com.example.truyen.service.MinIoService;
import com.example.truyen.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5174")
@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final MinIoService minIoService;

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

    // TẠO TRUYỆN MỚI VỚI ẢNH BÌA (CHỈ ADMIN và SUPER_ADMIN)
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StoryResponse>> createStoryWithImage(
            @RequestParam String title,
            @RequestParam Long authorId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) MultipartFile coverImage,
            @RequestParam(required = false) String categoryIds // Format: "1,2,3"
    ) {
        // Upload ảnh bìa nếu có
        String imageUrl = null;
        if (coverImage != null && !coverImage.isEmpty()) {
            imageUrl = minIoService.uploadFile(coverImage, "story-covers");
        }

        // Tạo request DTO
        StoryRequest request = new StoryRequest();
        request.setTitle(title);
        request.setAuthorId(authorId);
        request.setDescription(description);
        request.setImage(imageUrl);
        request.setStatus(status);

        // Parse categoryIds từ string "1,2,3" thành Set<Long>
        if (categoryIds != null && !categoryIds.isEmpty()) {
            Set<Long> categoryIdSet = Arrays.stream(categoryIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());
            request.setCategoryIds(categoryIdSet);
        }

        StoryResponse story = storyService.createStory(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo truyện thành công", story));
    }

    // TẠO TRUYỆN BẰNG JSON (KHÔNG CÓ ẢNH - giữ lại endpoint cũ)
    @PostMapping(consumes = {"application/json"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StoryResponse>> createStory(
            @Valid @RequestBody StoryRequest request
    ) {
        StoryResponse story = storyService.createStory(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo truyện thành công", story));
    }

    // CẬP NHẬT ẢNH BÌA CHO TRUYỆN ĐÃ CÓ
    @PutMapping("/{id}/cover-image")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StoryResponse>> updateCoverImage(
            @PathVariable Long id,
            @RequestParam MultipartFile coverImage
    ) {
        // Upload ảnh mới lên MinIO
        String imageUrl = minIoService.uploadFile(coverImage, "story-covers");

        // Cập nhật vào database
        StoryRequest request = new StoryRequest();
        request.setImage(imageUrl);

        StoryResponse story = storyService.updateStory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh bìa thành công", story));
    }

    // CẬP NHẬT TRUYỆN VỚI ẢNH MỚI (multipart/form-data)
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StoryResponse>> updateStoryWithImage(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) MultipartFile coverImage,
            @RequestParam(required = false) String categoryIds
    ) {
        // Upload ảnh mới nếu có
        String imageUrl = null;
        if (coverImage != null && !coverImage.isEmpty()) {
            imageUrl = minIoService.uploadFile(coverImage, "story-covers");
        }

        StoryRequest request = new StoryRequest();
        request.setTitle(title);
        request.setAuthorId(authorId);
        request.setDescription(description);
        request.setImage(imageUrl);
        request.setStatus(status);

        if (categoryIds != null && !categoryIds.isEmpty()) {
            Set<Long> categoryIdSet = Arrays.stream(categoryIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());
            request.setCategoryIds(categoryIdSet);
        }

        StoryResponse story = storyService.updateStory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật truyện thành công", story));
    }

    // CẬP NHẬT TRUYỆN BẰNG JSON (giữ lại endpoint cũ)
    @PutMapping(value = "/{id}", consumes = {"application/json"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StoryResponse>> updateStory(
            @PathVariable Long id,
            @Valid @RequestBody StoryRequest request
    ) {
        StoryResponse story = storyService.updateStory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật truyện thành công", story));
    }

    // Xóa truyện (CHỈ ADMIN và SUPER_ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteStory(@PathVariable Long id) {
        storyService.deleteStory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa truyện thành công", null));
    }
}