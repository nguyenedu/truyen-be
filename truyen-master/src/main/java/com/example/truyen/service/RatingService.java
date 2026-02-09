package com.example.truyen.service;

import com.example.truyen.dto.request.RatingRequest;
import com.example.truyen.dto.response.RatingResponse;
import com.example.truyen.entity.Rating;
import com.example.truyen.entity.Story;
import com.example.truyen.entity.User;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.RatingRepository;
import com.example.truyen.repository.StoryRepository;
import com.example.truyen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;

    // Gửi đánh giá và nhận xét mới
    @Transactional
    public RatingResponse rateStory(RatingRequest request) {
        User currentUser = getCurrentUser();

        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", request.getStoryId()));

        if (ratingRepository.existsByUserIdAndStoryId(currentUser.getId(), request.getStoryId())) {
            throw new BadRequestException(
                    "You have already rated this story. Please update your existing rating.");
        }

        Rating rating = Rating.builder()
                .user(currentUser)
                .story(story)
                .rating(request.getRating())
                .review(request.getReview())
                .build();

        return convertToResponse(ratingRepository.save(rating));
    }

    // Cập nhật đánh giá và nhận xét
    @Transactional
    public RatingResponse updateRating(Long storyId, RatingRequest request) {
        User currentUser = getCurrentUser();

        Rating rating = ratingRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found for this story"));

        rating.setRating(request.getRating());
        rating.setReview(request.getReview());

        return convertToResponse(ratingRepository.save(rating));
    }

    // Xóa đánh giá của người dùng
    @Transactional
    public void deleteRating(Long storyId) {
        User currentUser = getCurrentUser();

        Rating rating = ratingRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found for this story"));

        ratingRepository.delete(rating);
    }

    // Lấy đánh giá hiện tại của người dùng
    @Transactional(readOnly = true)
    public RatingResponse getMyRatingForStory(Long storyId) {
        User currentUser = getCurrentUser();

        Rating rating = ratingRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found for this story"));

        return convertToResponse(rating);
    }

    // Lấy thông tin tổng hợp đánh giá (trung bình và tổng số)
    @Transactional(readOnly = true)
    public Map<String, Object> getStoryRatingInfo(Long storyId) {
        storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        Double averageRating = ratingRepository.getAverageRating(storyId);
        Long totalRatings = ratingRepository.countByStoryId(storyId);

        Map<String, Object> result = new HashMap<>();
        result.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
        result.put("totalRatings", totalRatings);

        return result;
    }

    // Lấy người dùng hiện tại từ Scecurity Context
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Chuyển đổi entity Rating sang DTO RatingResponse
    private RatingResponse convertToResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .userId(rating.getUser().getId())
                .username(rating.getUser().getUsername())
                .storyId(rating.getStory().getId())
                .storyTitle(rating.getStory().getTitle())
                .rating(rating.getRating())
                .review(rating.getReview())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}