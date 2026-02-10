package com.example.truyen.service.impl;

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
import com.example.truyen.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;

    // Đánh giá truyện
    @Transactional
    @Override
    public RatingResponse rateStory(RatingRequest request) {
        User currentUser = getCurrentUser();
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", request.getStoryId()));

        if (ratingRepository.existsByUserIdAndStoryId(currentUser.getId(), request.getStoryId())) {
            throw new BadRequestException("You already rated this story");
        }

        int ratingValue = request.getRating();
        if (ratingValue < 1 || ratingValue > 5) {
            throw new BadRequestException("Rating score must be between 1 and 5");
        }

        Rating rating = Rating.builder()
                .user(currentUser)
                .story(story)
                .rating(ratingValue)
                .build();

        return convertToResponse(ratingRepository.save(rating));
    }

    // Cập nhật đánh giá
    @Transactional
    @Override
    public RatingResponse updateRating(Long storyId, RatingRequest request) {
        User currentUser = getCurrentUser();
        Rating rating = ratingRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found for this story"));

        int ratingValue = request.getRating();
        if (ratingValue < 1 || ratingValue > 5) {
            throw new BadRequestException("Rating score must be between 1 and 5");
        }

        rating.setRating(ratingValue);
        return convertToResponse(ratingRepository.save(rating));
    }

    // Xóa đánh giá
    @Transactional
    @Override
    public void deleteRating(Long storyId) {
        User currentUser = getCurrentUser();
        Rating rating = ratingRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found for this story"));
        ratingRepository.delete(rating);
    }

    // Lấy đánh giá của người dùng hiện tại cho truyện
    @Transactional(readOnly = true)
    @Override
    public RatingResponse getMyRatingForStory(Long storyId) {
        User currentUser = getCurrentUser();
        Rating rating = ratingRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found for this story"));
        return convertToResponse(rating);
    }

    // Lấy thông tin đánh giá tổng hợp của truyện
    @Transactional(readOnly = true)
    @Override
    public RatingResponse getStoryRatingInfo(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        Double averageRating = ratingRepository.getAverageRating(storyId);

        return RatingResponse.builder()
                .storyId(storyId)
                .storyTitle(story.getTitle())
                .rating(averageRating != null ? (int) Math.round(averageRating) : 0)
                .build();
    }

    // Lấy thông tin người dùng hiện tại
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Chuyển đổi entity sang DTO
    private RatingResponse convertToResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .userId(rating.getUser().getId())
                .username(rating.getUser().getUsername())
                .storyId(rating.getStory().getId())
                .storyTitle(rating.getStory().getTitle())
                .rating(rating.getRating())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}
