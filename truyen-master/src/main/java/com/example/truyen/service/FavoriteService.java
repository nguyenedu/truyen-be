package com.example.truyen.service;

import com.example.truyen.dto.response.FavoriteResponse;
import com.example.truyen.entity.Category;
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

    /**
     * Lấy danh sách truyện yêu thích của người dùng hiện tại với phân trang.
     */
    @Transactional(readOnly = true)
    public Page<FavoriteResponse> getMyFavorites(int page, int size) {
        User currentUser = getCurrentUser();
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), PageRequest.of(page, size))
                .map(this::convertToResponse);
    }

    /**
     * Kiểm tra xem truyện có được người dùng hiện tại yêu thích hay không.
     */
    @Transactional(readOnly = true)
    public Boolean isFavorite(Long storyId) {
        User currentUser = getCurrentUser();
        return favoriteRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId);
    }

    /**
     * Thêm truyện vào danh sách yêu thích của người dùng hiện tại.
     */
    @Transactional
    public FavoriteResponse addFavorite(Long storyId) {
        User currentUser = getCurrentUser();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        if (favoriteRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId)) {
            throw new BadRequestException("Truyện đã có trong danh sách yêu thích của bạn");
        }

        Favorite favorite = Favorite.builder()
                .user(currentUser)
                .story(story)
                .build();

        return convertToResponse(favoriteRepository.save(favorite));
    }

    /**
     * Xóa truyện khỏi danh sách yêu thích của người dùng hiện tại.
     */
    @Transactional
    public void removeFavorite(Long storyId) {
        User currentUser = getCurrentUser();
        Favorite favorite = favoriteRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Không tìm thấy truyện trong danh sách yêu thích của bạn"));

        favoriteRepository.delete(favorite);
    }

    /**
     * Lấy tổng số lượt yêu thích cho một truyện cụ thể.
     */
    @Transactional(readOnly = true)
    public Long countFavoritesByStoryId(Long storyId) {
        return favoriteRepository.countByStoryId(storyId);
    }

    /**
     * Lấy người dùng hiện tại đang đăng nhập từ SecurityContext.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
    }

    /**
     * Chuyển đổi Favorite sang FavoriteResponse DTO.
     */
    private FavoriteResponse convertToResponse(Favorite favorite) {
        Story story = favorite.getStory();
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .userId(favorite.getUser().getId())
                .username(favorite.getUser().getUsername())
                .storyId(story.getId())
                .storyTitle(story.getTitle())
                .storyImage(story.getImage())
                .authorName(story.getAuthor() != null ? story.getAuthor().getName() : null)
                .authorId(story.getAuthor() != null ? story.getAuthor().getId() : null)
                .categories(story.getCategories() != null ? story.getCategories().stream()
                        .map(Category::getName)
                        .toList() : null)
                .totalViews(story.getTotalViews())
                .totalChapters(story.getTotalChapters())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}