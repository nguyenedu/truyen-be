package com.example.truyen.controller;

import com.example.truyen.dto.request.StoryFilterCriteria;
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

import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

        private final StoryService storyService;
        private final MinIoService minIoService;

        // Lấy danh sách truyện (có phân trang)
        @GetMapping
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> getAllStories(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(ApiResponse.success("Get story list successfully",
                                storyService.getAllStories(page, size)));
        }

        // Lấy chi tiết truyện theo ID và tăng lượt xem
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<StoryResponse>> getStoryById(@PathVariable Long id) {
                var story = storyService.getStoryById(id);
                storyService.increaseView(id);
                return ResponseEntity.ok(ApiResponse.success("Get story details successfully", story));
        }

        // Tìm kiếm truyện theo từ khóa
        @GetMapping("/search")
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> searchStories(
                        @RequestParam String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(ApiResponse.success("Search completed",
                                storyService.searchStories(keyword, page, size)));
        }

        // Lấy truyện theo danh mục
        @GetMapping("/category/{categoryId}")
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> getStoriesByCategory(
                        @PathVariable Long categoryId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(ApiResponse.success("Get story list successfully",
                                storyService.getStoriesByCategory(categoryId, page, size)));
        }

        // Lấy danh sách truyện HOT
        @GetMapping("/hot")
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> getHotStories(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(ApiResponse.success("Get hot stories successfully",
                                storyService.getHotStories(page, size)));
        }

        // Lấy danh sách truyện mới nhất
        @GetMapping("/latest")
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> getLatestStories(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(ApiResponse.success("Get latest stories successfully",
                                storyService.getLatestStories(page, size)));
        }

        // Tạo truyện mới kèm ảnh bìa
        @PostMapping(consumes = { "multipart/form-data" })
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<StoryResponse>> createStoryWithImage(@ModelAttribute StoryRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Create story successfully",
                                                storyService.createStory(request)));
        }

        // Tạo truyện mới (JSON)
        @PostMapping(consumes = { "application/json" })
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<StoryResponse>> createStory(@Valid @RequestBody StoryRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Create story successfully",
                                                storyService.createStory(request)));
        }

        // Cập nhật ảnh bìa truyện
        @PutMapping("/{id}/cover-image")
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<StoryResponse>> updateCoverImage(
                        @PathVariable Long id,
                        @RequestParam MultipartFile coverImage) {
                String imageUrl = minIoService.uploadFile(coverImage, "story-covers");

                var request = new StoryRequest();
                request.setImage(imageUrl);

                return ResponseEntity
                                .ok(ApiResponse.success("Update cover image successfully",
                                                storyService.updateStory(id, request)));
        }

        // Cập nhật thông tin truyện kèm ảnh bìa
        @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<StoryResponse>> updateStoryWithImage(
                        @PathVariable Long id,
                        @ModelAttribute StoryRequest request) {

                return ResponseEntity.ok(ApiResponse.success("Update story successfully",
                                storyService.updateStory(id, request)));
        }

        // Cập nhật thông tin truyện (JSON)
        @PutMapping(value = "/{id}", consumes = { "application/json" })
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<StoryResponse>> updateStory(@PathVariable Long id,
                        @Valid @RequestBody StoryRequest request) {
                return ResponseEntity.ok(ApiResponse.success("Update story successfully",
                                storyService.updateStory(id, request)));
        }

        // Xóa truyện
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<String>> deleteStory(@PathVariable Long id) {
                storyService.deleteStory(id);
                return ResponseEntity.ok(ApiResponse.success("Delete story successfully", null));
        }

        // Lấy danh sách truyện của một tác giả
        @GetMapping("/author/{authorId}")
        public ApiResponse<List<StoryResponse>> getStoriesByAuthor(@PathVariable Long authorId) {
                return ApiResponse.success("Get story list successfully",
                                storyService.getStoriesByAuthor(authorId));
        }

        // Lọc truyện nâng cao
        @GetMapping("/filter")
        public ResponseEntity<ApiResponse<Page<StoryResponse>>> filterStories(
                        @ModelAttribute StoryFilterCriteria criteria) {
                var stories = storyService.filterStories(criteria);
                return ResponseEntity.ok(ApiResponse.success("Get filtered stories successfully", stories));
        }
}
