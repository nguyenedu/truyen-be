package com.example.truyen.service;

import com.example.truyen.dto.request.CommentRequest;
import com.example.truyen.dto.response.CommentResponse;
import com.example.truyen.entity.Chapter;
import com.example.truyen.entity.Comment;
import com.example.truyen.entity.Story;
import com.example.truyen.entity.User;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final CommentLikeRepository commentLikeRepository;

    // Lấy danh sách bình luận truyện (phân trang, mới nhất trước)
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByStoryId(Long storyId, int page, int size) {
        storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        return commentRepository.findByStoryIdOrderByCreatedAtDesc(storyId, PageRequest.of(page, size))
                .map(this::convertToResponse);
    }

    // Lấy danh sách bình luận chương (phân trang)
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByChapterId(Long chapterId, int page, int size) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", chapterId));

        return commentRepository.findByChapterIdOrderByCreatedAtDesc(chapterId, PageRequest.of(page, size))
                .map(this::convertToResponse);
    }

    // Tạo bình luận mới (gắn với truyện hoặc chương)
    @Transactional
    public CommentResponse createComment(CommentRequest request) {
        User currentUser = getCurrentUser();

        if (request.getStoryId() == null && request.getChapterId() == null) {
            throw new BadRequestException("Story ID or Chapter ID must be provided");
        }

        Story story = null;
        Chapter chapter = null;

        if (request.getStoryId() != null) {
            story = storyRepository.findById(request.getStoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Story", "id", request.getStoryId()));
        }

        if (request.getChapterId() != null) {
            chapter = chapterRepository.findById(request.getChapterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", request.getChapterId()));
            if (story == null) {
                story = chapter.getStory();
            }
        }

        Comment comment = Comment.builder()
                .user(currentUser)
                .story(story)
                .chapter(chapter)
                .content(request.getContent())
                .likesCount(0)
                .build();

        return convertToResponse(commentRepository.save(comment));
    }

    // Cập nhật nội dung bình luận (kiểm tra quyền sở hữu)
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest request) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You do not have permission to edit this comment");
        }

        comment.setContent(request.getContent());
        return convertToResponse(commentRepository.save(comment));
    }

    // Xóa bình luận (kiểm tra quyền sở hữu hoặc Admin)
    @Transactional
    public void deleteComment(Long commentId) {
        log.debug("Attempting to delete comment with ID: {}", commentId);
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        boolean isAdmin = currentUser.getRole().equals(User.Role.ADMIN)
                || currentUser.getRole().equals(User.Role.SUPER_ADMIN);

        log.debug("User {} (role: {}) is attempting to delete comment by {}",
                currentUser.getUsername(), currentUser.getRole(), comment.getUser().getUsername());

        if (!comment.getUser().getId().equals(currentUser.getId()) && !isAdmin) {
            log.warn("User {} does not have permission to delete comment {}", currentUser.getUsername(), commentId);
            throw new BadRequestException("You do not have permission to delete this comment");
        }

        // Delete all likes related to this comment before deleting comment to avoid FK
        // error
        commentLikeRepository.deleteByCommentId(commentId);
        log.debug("Deleted likes for comment {}", commentId);

        commentRepository.delete(comment);
        log.debug("Successfully deleted comment {}", commentId);
    }

    // Đếm tổng số bình luận của truyện
    @Transactional(readOnly = true)
    public Long countCommentsByStoryId(Long storyId) {
        return commentRepository.countByStoryId(storyId);
    }

    // Đếm tổng số bình luận của chương
    @Transactional(readOnly = true)
    public Long countCommentsByChapterId(Long chapterId) {
        return commentRepository.countByChapterId(chapterId);
    }

    /**
     * Get current logged-in user from SecurityContext.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Convert Comment to CommentResponse DTO.
     */
    private CommentResponse convertToResponse(Comment comment) {
        boolean isLiked = false;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
                if (currentUser != null) {
                    isLiked = commentLikeRepository.existsByUserIdAndCommentId(currentUser.getId(), comment.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error checking like status: {}", e.getMessage());
        }

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
                .isLiked(isLiked)
                .build();
    }
}