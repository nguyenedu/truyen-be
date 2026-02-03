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

    /**
     * Retrieve comments for a story with pagination, ordered by creation date
     * (descending).
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByStoryId(Long storyId, int page, int size) {
        storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        return commentRepository.findByStoryIdOrderByCreatedAtDesc(storyId, PageRequest.of(page, size))
                .map(this::convertToResponse);
    }

    /**
     * Retrieve comments for a specific chapter with pagination.
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByChapterId(Long chapterId, int page, int size) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", chapterId));

        return commentRepository.findByChapterIdOrderByCreatedAtDesc(chapterId, PageRequest.of(page, size))
                .map(this::convertToResponse);
    }

    /**
     * Create a new comment linked to either a story or a chapter.
     */
    @Transactional
    public CommentResponse createComment(CommentRequest request) {
        User currentUser = getCurrentUser();

        if (request.getStoryId() == null && request.getChapterId() == null) {
            throw new BadRequestException("Either Story ID or Chapter ID must be provided");
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

    /**
     * Update comment content if user has ownership.
     */
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

    /**
     * Delete a comment if user has ownership or is an administrator.
     */
    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getUser().getId().equals(currentUser.getId()) && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("You do not have permission to delete this comment");
        }

        commentRepository.delete(comment);
    }

    /**
     * Get total comment count for a story.
     */
    @Transactional(readOnly = true)
    public Long countCommentsByStoryId(Long storyId) {
        return commentRepository.countByStoryId(storyId);
    }

    /**
     * Get total comment count for a chapter.
     */
    @Transactional(readOnly = true)
    public Long countCommentsByChapterId(Long chapterId) {
        return commentRepository.countByChapterId(chapterId);
    }

    /**
     * Get current authenticated user from SecurityContext.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Map Comment entity to CommentResponse DTO.
     */
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