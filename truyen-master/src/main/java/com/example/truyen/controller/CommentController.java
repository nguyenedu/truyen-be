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

    // Lấy danh sách bình luận của truyện
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentsByStoryId(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Get comment list successfully",
                commentService.getCommentsByStoryId(storyId, page, size)));
    }

    // Lấy danh sách bình luận của chương
    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentsByChapterId(
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Get comment list successfully",
                commentService.getCommentsByChapterId(chapterId, page, size)));
    }

    // Đăng bình luận mới
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(@Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post comment successfully", commentService.createComment(request)));
    }

    // Cập nhật bình luận
    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable("commentId") Long commentId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Update comment successfully",
                commentService.updateComment(commentId, request)));
    }

    // Xóa bình luận
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteComment(@PathVariable("commentId") Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("Delete comment successfully", null));
    }

    // Đếm số bình luận của truyện
    @GetMapping("/count/story/{storyId}")
    public ResponseEntity<ApiResponse<Long>> countCommentsByStoryId(@PathVariable Long storyId) {
        return ResponseEntity.ok(ApiResponse.success("Get count successfully",
                commentService.countCommentsByStoryId(storyId)));
    }

    // Đếm số bình luận của chương
    @GetMapping("/count/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<Long>> countCommentsByChapterId(@PathVariable Long chapterId) {
        return ResponseEntity.ok(ApiResponse.success("Get count successfully",
                commentService.countCommentsByChapterId(chapterId)));
    }
}