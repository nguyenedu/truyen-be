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
     * Get comments for a specific story.
     */
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentsByStoryId(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully",
                commentService.getCommentsByStoryId(storyId, page, size)));
    }

    /**
     * Get comments for a specific chapter.
     */
    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentsByChapterId(
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully",
                commentService.getCommentsByChapterId(chapterId, page, size)));
    }

    /**
     * Create a new comment.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(@Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment posted successfully", commentService.createComment(request)));
    }

    /**
     * Update an existing comment.
     */
    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Comment updated successfully",
                commentService.updateComment(commentId, request)));
    }

    /**
     * Delete a comment.
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
    }

    /**
     * Count total comments for a story.
     */
    @GetMapping("/count/story/{storyId}")
    public ResponseEntity<ApiResponse<Long>> countCommentsByStoryId(@PathVariable Long storyId) {
        return ResponseEntity.ok(ApiResponse.success("Count retrieved successfully",
                commentService.countCommentsByStoryId(storyId)));
    }

    /**
     * Count total comments for a chapter.
     */
    @GetMapping("/count/chapter/{chapterId}")
    public ResponseEntity<ApiResponse<Long>> countCommentsByChapterId(@PathVariable Long chapterId) {
        return ResponseEntity.ok(ApiResponse.success("Count retrieved successfully",
                commentService.countCommentsByChapterId(chapterId)));
    }
}