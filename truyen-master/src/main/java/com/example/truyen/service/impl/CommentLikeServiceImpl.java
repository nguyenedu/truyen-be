package com.example.truyen.service.impl;

import com.example.truyen.entity.Comment;
import com.example.truyen.entity.CommentLike;
import com.example.truyen.entity.User;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.CommentLikeRepository;
import com.example.truyen.repository.CommentRepository;
import com.example.truyen.repository.UserRepository;
import com.example.truyen.service.CommentLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    // Thích hoặc bỏ thích bình luận
    @Transactional
    @Override
    public void toggleLike(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        Optional<CommentLike> existingLike = commentLikeRepository.findByUserIdAndCommentId(currentUser.getId(),
                commentId);

        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
            comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
        } else {
            CommentLike like = CommentLike.builder()
                    .user(currentUser)
                    .comment(comment)
                    .build();
            commentLikeRepository.save(like);
            comment.setLikesCount(comment.getLikesCount() + 1);
        }
        commentRepository.save(comment);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
