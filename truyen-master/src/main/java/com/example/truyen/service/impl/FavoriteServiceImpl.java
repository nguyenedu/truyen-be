package com.example.truyen.service.impl;

import com.example.truyen.dto.response.FavoriteResponse;
import com.example.truyen.entity.Favorite;
import com.example.truyen.entity.Story;
import com.example.truyen.entity.User;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.FavoriteRepository;
import com.example.truyen.repository.StoryRepository;
import com.example.truyen.repository.UserRepository;
import com.example.truyen.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;

    // Lấy danh sách truyện yêu thích của người dùng
    @Transactional(readOnly = true)
    @Override
    public Page<FavoriteResponse> getMyFavorites(int page, int size) {
        User currentUser = getCurrentUser();
        return favoriteRepository
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId(), PageRequest.of(page, size))
                .map(this::convertToResponse);
    }

    // Kiểm tra truyện có trong danh sách yêu thích không
    @Transactional(readOnly = true)
    @Override
    public boolean isFavorite(Long storyId) {
        User currentUser = getCurrentUser();
        return favoriteRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId);
    }

    // Thêm truyện vào danh sách yêu thích
    @Transactional
    @Override
    public FavoriteResponse addFavorite(Long storyId) {
        User currentUser = getCurrentUser();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        if (favoriteRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId)) {
            throw new BadRequestException("Story already in favorites");
        }

        Favorite favorite = Favorite.builder()
                .user(currentUser)
                .story(story)
                .build();

        return convertToResponse(favoriteRepository.save(favorite));
    }

    // Xóa truyện khỏi danh sách yêu thích
    @Transactional
    @Override
    public void removeFavorite(Long storyId) {
        User currentUser = getCurrentUser();
        Favorite favorite = favoriteRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found"));
        favoriteRepository.delete(favorite);
    }

    // Đếm số lượt yêu thích của truyện
    @Transactional(readOnly = true)
    @Override
    public Long countFavoritesByStoryId(Long storyId) {
        return favoriteRepository.countByStoryId(storyId);
    }

    // Lấy thông tin người dùng hiện tại
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Chuyển đổi entity sang DTO
    private FavoriteResponse convertToResponse(Favorite favorite) {
        var categoryNames = favorite.getStory().getCategories().stream()
                .map(cat -> cat.getName())
                .collect(Collectors.toList());

        return FavoriteResponse.builder()
                .id(favorite.getId())
                .userId(favorite.getUser().getId())
                .username(favorite.getUser().getUsername())
                .storyId(favorite.getStory().getId())
                .storyTitle(favorite.getStory().getTitle())
                .storyImage(favorite.getStory().getImage())
                .authorName(favorite.getStory().getAuthor() != null ? favorite.getStory().getAuthor().getName() : null)
                .authorId(favorite.getStory().getAuthor() != null ? favorite.getStory().getAuthor().getId() : null)
                .categories(categoryNames)
                .totalViews(favorite.getStory().getTotalViews())
                .totalChapters(favorite.getStory().getTotalChapters())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
