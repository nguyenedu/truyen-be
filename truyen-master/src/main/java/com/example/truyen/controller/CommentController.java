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

    // Lấy bình luận của truyện
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentsByStoryId(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<CommentResponse> comments = commentService.getCommentsByStoryId(storyId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy bình luận thành công", comments));
    }

    // Lấy bình luận của chương
    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentsByChapterId(
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<CommentResponse> comments = commentService.getCommentsByChapterId(chapterId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy bình luận thành công", comments));
    }

    // Tạo bình luận
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Valid @RequestBody CommentRequest request
    ) {
        CommentResponse comment = commentService.createComment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bình luận thành công", comment));
    }

    // Cập nhật bình luận
    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request
    ) {
        CommentResponse comment = commentService.updateComment(commentId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bình luận thành công", comment));
    }

    // Xóa bình luận
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("Xóa bình luận thành công", null));
    }

    // Đếm số bình luận của truyện
    @GetMapping("/count/story/{storyId}")
    public ResponseEntity<ApiResponse<Long>> countCommentsByStoryId(@PathVariable Long storyId) {
        Long count = commentService.countCommentsByStoryId(storyId);
        return ResponseEntity.ok(ApiResponse.success("Đếm bình luận thành công", count));
    }

    // Đếm số bình luận của chương
    @GetMapping("/count/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<Long>> countCommentsByChapterId(@PathVariable Long chapterId) {
        Long count = commentService.countCommentsByChapterId(chapterId);
        return ResponseEntity.ok(ApiResponse.success("Đếm bình luận thành công", count));
    }
}