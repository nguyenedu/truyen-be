package com.example.truyen.service;

import com.example.truyen.dto.request.CommentRequest;
import com.example.truyen.dto.response.CommentResponse;
import com.example.truyen.entity.Chapter;
import com.example.truyen.entity.Comment;
import com.example.truyen.entity.Story;
import com.example.truyen.entity.User;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.ChapterRepository;
import com.example.truyen.repository.CommentRepository;
import com.example.truyen.repository.StoryRepository;
import com.example.truyen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;

    // Lấy bình luận của truyện
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByStoryId(Long storyId, int page, int size) {
        // Kiểm tra truyện tồn tại
        storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", storyId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByStoryIdOrderByCreatedAtDesc(storyId, pageable);
        return comments.map(this::convertToResponse);
    }

    // Lấy bình luận của chương
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByChapterId(Long chapterId, int page, int size) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chương", "id", chapterId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByChapterIdOrderByCreatedAtDesc(chapterId, pageable);
        return comments.map(this::convertToResponse);
    }

    // Tạo bình luận mới
    @Transactional
    public CommentResponse createComment(CommentRequest request) {
        User currentUser = getCurrentUser();

        // Phải có ít nhất storyId hoặc chapterId
        if (request.getStoryId() == null && request.getChapterId() == null) {
            throw new BadRequestException("Phải cung cấp Story ID hoặc Chapter ID");
        }

        Story story = null;
        Chapter chapter = null;

        // Kiểm tra storyId
        if (request.getStoryId() != null) {
            story = storyRepository.findById(request.getStoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", request.getStoryId()));
        }

        // Kiểm tra chapterId
        if (request.getChapterId() != null) {
            chapter = chapterRepository.findById(request.getChapterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chương", "id", request.getChapterId()));

            // Nếu có chapterId thì lấy story từ chapter
            if (story == null) {
                story = chapter.getStory();
            }
        }

        // Tạo comment
        Comment comment = Comment.builder()
                .user(currentUser)
                .story(story)
                .chapter(chapter)
                .content(request.getContent())
                .likesCount(0)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return convertToResponse(savedComment);
    }

    // Cập nhật bình luận
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest request) {
        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bình luận", "id", commentId));

        // Kiểm tra quyền sở hữu
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền sửa bình luận này");
        }

        // Cập nhật nội dung
        comment.setContent(request.getContent());

        Comment updatedComment = commentRepository.save(comment);
        return convertToResponse(updatedComment);
    }

    // Xóa bình luận
    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Bình luận", "id", commentId));

        // Kiểm tra quyền sở hữu hoặc là ADMIN
        if (!comment.getUser().getId().equals(currentUser.getId())
                && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Bạn không có quyền xóa bình luận này");
        }

        commentRepository.delete(comment);
    }

    // Đếm số bình luận của truyện
    @Transactional(readOnly = true)
    public Long countCommentsByStoryId(Long storyId) {
        return commentRepository.countByStoryId(storyId);
    }

    // Đếm số bình luận của chương
    @Transactional(readOnly = true)
    public Long countCommentsByChapterId(Long chapterId) {
        return commentRepository.countByChapterId(chapterId);
    }

    // Lấy user hiện tại
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }

    // Convert Entity sang Response DTO
    private CommentResponse convertToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .userAvatar(comment.getUser().getAvatar())
                .storyId(comment.getStory() != null ? comment.getStory().getId() : null)
                .storyTitle(comment.getStory() != null ? comment.getStory().getTitle() : null)
                .chapterId(comment.getChapter() != null ? comment.getChapter().getId() : null)
                .chapterNumber(comment.getChapter() != null ? comment.getChapter().getChapterNumber() : null)
                .content(comment.getContent())
                .likesCount(comment.getLikesCount())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}