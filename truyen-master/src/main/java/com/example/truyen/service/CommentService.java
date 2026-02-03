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
     * Lấy danh sách bình luận của truyện với phân trang, sắp xếp theo ngày tạo
     * (giảm dần).
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByStoryId(Long storyId, int page, int size) {
        storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        return commentRepository.findByStoryIdOrderByCreatedAtDesc(storyId, PageRequest.of(page, size))
                .map(this::convertToResponse);
    }

    /**
     * Lấy danh sách bình luận của một chương cụ thể với phân trang.
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByChapterId(Long chapterId, int page, int size) {
        chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", chapterId));

        return commentRepository.findByChapterIdOrderByCreatedAtDesc(chapterId, PageRequest.of(page, size))
                .map(this::convertToResponse);
    }

    /**
     * Tạo bình luận mới liên kết với truyện hoặc chương.
     */
    @Transactional
    public CommentResponse createComment(CommentRequest request) {
        User currentUser = getCurrentUser();

        if (request.getStoryId() == null && request.getChapterId() == null) {
            throw new BadRequestException("Phải cung cấp ID truyện hoặc ID chương");
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
     * Cập nhật nội dung bình luận nếu người dùng có quyền sở hữu.
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest request) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa bình luận này");
        }

        comment.setContent(request.getContent());
        return convertToResponse(commentRepository.save(comment));
    }

    /**
     * Xóa bình luận nếu người dùng có quyền sở hữu hoặc là quản trị viên.
     */
    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getUser().getId().equals(currentUser.getId()) && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Bạn không có quyền xóa bình luận này");
        }

        commentRepository.delete(comment);
    }

    /**
     * Lấy tổng số bình luận của một truyện.
     */
    @Transactional(readOnly = true)
    public Long countCommentsByStoryId(Long storyId) {
        return commentRepository.countByStoryId(storyId);
    }

    /**
     * Lấy tổng số bình luận của một chương.
     */
    @Transactional(readOnly = true)
    public Long countCommentsByChapterId(Long chapterId) {
        return commentRepository.countByChapterId(chapterId);
    }

    /**
     * Lấy người dùng hiện tại đang đăng nhập từ SecurityContext.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tìm thấy"));
    }

    /**
     * Chuyển đổi Comment sang CommentResponse DTO.
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