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

    // Đánh giá truyện
    @Transactional
    public RatingResponse rateStory(RatingRequest request) {
        User currentUser = getCurrentUser();

        // Kiểm tra truyện tồn tại
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", request.getStoryId()));

        // Kiểm tra đã đánh giá chưa
        if (ratingRepository.existsByUserIdAndStoryId(currentUser.getId(), request.getStoryId())) {
            throw new BadRequestException("Bạn đã đánh giá truyện này rồi. Vui lòng cập nhật đánh giá.");
        }

        // Tạo rating
        Rating rating = Rating.builder()
                .user(currentUser)
                .story(story)
                .rating(request.getRating())
                .review(request.getReview())
                .build();

        Rating savedRating = ratingRepository.save(rating);
        return convertToResponse(savedRating);
    }

    // Cập nhật đánh giá
    @Transactional
    public RatingResponse updateRating(Long storyId, RatingRequest request) {
        User currentUser = getCurrentUser();

        // Tìm rating của user cho truyện này
        Rating rating = ratingRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa đánh giá truyện này"));

        // Cập nhật
        rating.setRating(request.getRating());
        rating.setReview(request.getReview());

        Rating updatedRating = ratingRepository.save(rating);
        return convertToResponse(updatedRating);
    }

    // Xóa đánh giá
    @Transactional
    public void deleteRating(Long storyId) {
        User currentUser = getCurrentUser();

        Rating rating = ratingRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa đánh giá truyện này"));

        ratingRepository.delete(rating);
    }

    // Lấy đánh giá của user cho truyện
    @Transactional(readOnly = true)
    public RatingResponse getMyRatingForStory(Long storyId) {
        User currentUser = getCurrentUser();

        Rating rating = ratingRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa đánh giá truyện này"));

        return convertToResponse(rating);
    }

    // Lấy thông tin đánh giá trung bình của truyện
    @Transactional(readOnly = true)
    public Map<String, Object> getStoryRatingInfo(Long storyId) {
        // Kiểm tra truyện tồn tại
        storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", storyId));

        Double averageRating = ratingRepository.getAverageRating(storyId);
        Long totalRatings = ratingRepository.countByStoryId(storyId);

        Map<String, Object> result = new HashMap<>();
        result.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
        result.put("totalRatings", totalRatings);

        return result;
    }

    // Lấy user hiện tại
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }

    // Convert Entity sang Response DTO
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