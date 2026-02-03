package com.example.truyen.controller;

import com.example.truyen.dto.request.StoryRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.StoryResponse;
import com.example.truyen.service.MinIoService;
import com.example.truyen.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5174")
@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

        private final StoryService storyService;
        private final MinIoService minIoService;

        /**
         * Lấy danh sách tất cả truyện với phân trang.
         */
        @GetMapping
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> getAllStories(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity
                                .ok(ApiResponse.success("Lấy danh sách truyện thành công",
                                                storyService.getAllStories(page, size)));
        }

        /**
         * Lấy chi tiết truyện theo ID và tăng lượt xem.
         */
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<StoryResponse>> getStoryById(@PathVariable Long id) {
                StoryResponse story = storyService.getStoryById(id);
                storyService.increaseView(id);
                return ResponseEntity.ok(ApiResponse.success("Lấy thông tin truyện thành công", story));
        }

        /**
         * Tìm kiếm truyện theo từ khóa.
         */
        @GetMapping("/search")
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> searchStories(
                        @RequestParam String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(
                                ApiResponse.success("Tìm kiếm hoàn tất",
                                                storyService.searchStories(keyword, page, size)));
        }

        /**
         * Lấy danh sách truyện theo thể loại.
         */
        @GetMapping("/category/{categoryId}")
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> getStoriesByCategory(
                        @PathVariable Long categoryId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(ApiResponse.success("Lấy danh sách truyện thành công",
                                storyService.getStoriesByCategory(categoryId, page, size)));
        }

        /**
         * Lấy danh sách truyện đang thịnh hành (HOT).
         */
        @GetMapping("/hot")
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> getHotStories(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity
                                .ok(ApiResponse.success("Lấy danh sách truyện HOT thành công",
                                                storyService.getHotStories(page, size)));
        }

        /**
         * Lấy danh sách truyện mới cập nhật.
         */
        @GetMapping("/latest")
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> getLatestStories(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(ApiResponse.success("Lấy danh sách truyện mới nhất thành công",
                                storyService.getLatestStories(page, size)));
        }

        /**
         * Tạo truyện mới với ảnh bìa (Multipart).
         */
        @PostMapping(consumes = { "multipart/form-data" })
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<StoryResponse>> createStoryWithImage(
                        @RequestParam String title,
                        @RequestParam Long authorId,
                        @RequestParam(required = false) String description,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) MultipartFile coverImage,
                        @RequestParam(required = false) String categoryIds) {
                String imageUrl = (coverImage != null && !coverImage.isEmpty())
                                ? minIoService.uploadFile(coverImage, "story-covers")
                                : null;

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

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Tạo truyện thành công", storyService.createStory(request)));
        }

        /**
         * Tạo truyện mới (JSON).
         */
        @PostMapping(consumes = { "application/json" })
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<StoryResponse>> createStory(@Valid @RequestBody StoryRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Tạo truyện thành công",
                                                storyService.createStory(request)));
        }

        /**
         * Cập nhật ảnh bìa cho truyện hiện có.
         */
        @PutMapping("/{id}/cover-image")
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<StoryResponse>> updateCoverImage(
                        @PathVariable Long id,
                        @RequestParam MultipartFile coverImage) {
                String imageUrl = minIoService.uploadFile(coverImage, "story-covers");

                StoryRequest request = new StoryRequest();
                request.setImage(imageUrl);

                return ResponseEntity
                                .ok(ApiResponse.success("Cập nhật ảnh bìa thành công",
                                                storyService.updateStory(id, request)));
        }

        /**
         * Cập nhật thông tin truyện với ảnh bìa (Multipart).
         */
        @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<StoryResponse>> updateStoryWithImage(
                        @PathVariable Long id,
                        @RequestParam(required = false) String title,
                        @RequestParam(required = false) Long authorId,
                        @RequestParam(required = false) String description,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) MultipartFile coverImage,
                        @RequestParam(required = false) String categoryIds) {
                String imageUrl = (coverImage != null && !coverImage.isEmpty())
                                ? minIoService.uploadFile(coverImage, "story-covers")
                                : null;

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

                return ResponseEntity
                                .ok(ApiResponse.success("Cập nhật truyện thành công",
                                                storyService.updateStory(id, request)));
        }

        /**
         * Cập nhật thông tin truyện (JSON).
         */
        @PutMapping(value = "/{id}", consumes = { "application/json" })
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<StoryResponse>> updateStory(@PathVariable Long id,
                        @Valid @RequestBody StoryRequest request) {
                return ResponseEntity
                                .ok(ApiResponse.success("Cập nhật truyện thành công",
                                                storyService.updateStory(id, request)));
        }

        /**
         * Xóa một truyện.
         */
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<String>> deleteStory(@PathVariable Long id) {
                storyService.deleteStory(id);
                return ResponseEntity.ok(ApiResponse.success("Xóa truyện thành công", null));
        }

        /**
         * Lấy danh sách truyện theo tác giả.
         */
        @GetMapping("/author/{authorId}")
        public ApiResponse<List<StoryResponse>> getStoriesByAuthor(@PathVariable Long authorId) {
                return ApiResponse.success("Lấy danh sách truyện thành công",
                                storyService.getStoriesByAuthor(authorId));
        }

        /**
         * Lọc truyện dựa trên nhiều tiêu chí.
         */
        @GetMapping("/filter")
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> filterStories(
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long authorId,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) Integer minChapters,
                        @RequestParam(required = false) Integer maxChapters,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id,desc") String sort) {
                Page<StoryResponse> stories = storyService.filterStories(
                                keyword, authorId, status, minChapters, maxChapters,
                                startDate, endDate, page, size, sort);
                return ResponseEntity.ok(ApiResponse.success("Lấy danh sách truyện đã lọc thành công", stories));
        }
}
