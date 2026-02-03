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
     * Retrieve the current user's favorite stories with pagination.
     */
    @Transactional(readOnly = true)
    public Page<FavoriteResponse> getMyFavorites(int page, int size) {
        User currentUser = getCurrentUser();
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), PageRequest.of(page, size))
                .map(this::convertToResponse);
    }

    /**
     * Check if a story is favorited by the current user.
     */
    @Transactional(readOnly = true)
    public Boolean isFavorite(Long storyId) {
        User currentUser = getCurrentUser();
        return favoriteRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId);
    }

    /**
     * Add a story to the current user's favorite list.
     */
    @Transactional
    public FavoriteResponse addFavorite(Long storyId) {
        User currentUser = getCurrentUser();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        if (favoriteRepository.existsByUserIdAndStoryId(currentUser.getId(), storyId)) {
            throw new BadRequestException("Story is already in your favorites list");
        }

        Favorite favorite = Favorite.builder()
                .user(currentUser)
                .story(story)
                .build();

        return convertToResponse(favoriteRepository.save(favorite));
    }

    /**
     * Remove a story from the current user's favorite list.
     */
    @Transactional
    public void removeFavorite(Long storyId) {
        User currentUser = getCurrentUser();
        Favorite favorite = favoriteRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found in your favorites list"));

        favoriteRepository.delete(favorite);
    }

    /**
     * Get the total favorite count for a given story.
     */
    @Transactional(readOnly = true)
    public Long countFavoritesByStoryId(Long storyId) {
        return favoriteRepository.countByStoryId(storyId);
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
     * Map Favorite entity to FavoriteResponse DTO.
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
                .categories(story.getCategories() != null ? story.getCategories().stream()
                        .map(Category::getName)
                        .toList() : null)
                .totalViews(story.getTotalViews())
                .totalChapters(story.getTotalChapters())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}