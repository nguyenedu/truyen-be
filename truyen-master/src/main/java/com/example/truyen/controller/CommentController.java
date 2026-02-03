package com.example.truyen.controller;

import com.example.truyen.dto.request.CommentRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.CommentResponse;
import com.example.truyen.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * Lấy bình luận của một truyện cụ thể.
     */
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentsByStoryId(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bình luận thành công",
                commentService.getCommentsByStoryId(storyId, page, size)));
    }

    /**
     * Lấy bình luận của một chương cụ thể.
     */
    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentsByChapterId(
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bình luận thành công",
                commentService.getCommentsByChapterId(chapterId, page, size)));
    }

    /**
     * Tạo bình luận mới.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(@Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng bình luận thành công", commentService.createComment(request)));
    }

    /**
     * Cập nhật bình luận hiện có.
     */
    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bình luận thành công",
                commentService.updateComment(commentId, request)));
    }

    /**
     * Xóa một bình luận.
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("Xóa bình luận thành công", null));
    }

    /**
     * Đếm tổng số bình luận của một truyện.
     */
    @GetMapping("/count/story/{storyId}")
    public ResponseEntity<ApiResponse<Long>> countCommentsByStoryId(@PathVariable Long storyId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thành công",
                commentService.countCommentsByStoryId(storyId)));
    }

    /**
     * Đếm tổng số bình luận của một chương.
     */
    @GetMapping("/count/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<Long>> countCommentsByChapterId(@PathVariable Long chapterId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thành công",
                commentService.countCommentsByChapterId(chapterId)));
    }
}