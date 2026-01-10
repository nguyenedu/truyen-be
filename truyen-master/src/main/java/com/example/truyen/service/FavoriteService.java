package com.example.truyen.service;

import com.example.truyen.dto.response.FavoriteResponse;
import com.example.truyen.entity.Favorite;
import com.example.truyen.entity.Story;
import com.example.truyen.entity.User;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.FavoriteRepository;
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
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;

    // Lấy danh sách truyện yêu thích của user đang đăng nhập
    @Transactional(readOnly = true)
    public Page<FavoriteResponse> getMyFavorites(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        return favorites.map(this::convertToResponse);
    }

    // Kiểm tra truyện đã được yêu thích chưa
    @Transactional(readOnly = true)
    public Boolean isFavorite(Long storyId) {
        User currentUser = getCurrentUser();
        return favoriteRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId);
    }

    // Thêm truyện vào danh sách yêu thích
    @Transactional
    public FavoriteResponse addFavorite(Long storyId) {
        User currentUser = getCurrentUser();

        // Kiểm tra truyện tồn tại
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", storyId));

        // Kiểm tra đã yêu thích chưa
        if (favoriteRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId)) {
            throw new BadRequestException("Truyện đã có trong danh sách yêu thích");
        }

        // Thêm vào yêu thích
        Favorite favorite = Favorite.builder()
                .user(currentUser)
                .story(story)
                .build();

        Favorite savedFavorite = favoriteRepository.save(favorite);
        return convertToResponse(savedFavorite);
    }

    // Xóa truyện khỏi danh sách yêu thích
    @Transactional
    public void removeFavorite(Long storyId) {
        User currentUser = getCurrentUser();

        // Kiểm tra truyện có trong danh sách yêu thích không
        Favorite favorite = favoriteRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Truyện không có trong danh sách yêu thích"));

        favoriteRepository.delete(favorite);
    }

    // Đếm số lượt yêu thích của truyện
    @Transactional(readOnly = true)
    public Long countFavoritesByStoryId(Long storyId) {
        return favoriteRepository.countByStoryId(storyId);
    }

    // Lấy user hiện tại từ Security Context
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }

    // Convert Entity sang Response DTO
    private FavoriteResponse convertToResponse(Favorite favorite) {
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .userId(favorite.getUser().getId())
                .username(favorite.getUser().getUsername())
                .storyId(favorite.getStory().getId())
                .storyTitle(favorite.getStory().getTitle())
                .storyImage(favorite.getStory().getImage())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}