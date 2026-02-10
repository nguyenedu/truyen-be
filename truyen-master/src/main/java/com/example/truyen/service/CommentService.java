package com.example.truyen.service;

import com.example.truyen.dto.request.CommentRequest;
import com.example.truyen.dto.response.CommentResponse;
import org.springframework.data.domain.Page;

// Interface CommentService
public interface CommentService {

    // Lấy danh sách bình luận truyện
    Page<CommentResponse> getCommentsByStoryId(Long storyId, int page, int size);

    // Lấy danh sách bình luận chương
    Page<CommentResponse> getCommentsByChapterId(Long chapterId, int page, int size);

    // Tạo bình luận mới
    CommentResponse createComment(CommentRequest request);

    // Cập nhật nội dung bình luận
    CommentResponse updateComment(Long commentId, CommentRequest request);

    // Xóa bình luận
    void deleteComment(Long commentId);

    // Đếm tổng số bình luận của truyện
    Long countCommentsByStoryId(Long storyId);

    // Đếm tổng số bình luận của chương
    Long countCommentsByChapterId(Long chapterId);
}