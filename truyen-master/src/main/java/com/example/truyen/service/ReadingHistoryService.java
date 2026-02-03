package com.example.truyen.service;

import com.example.truyen.dto.response.ReadingHistoryResponse;
import com.example.truyen.entity.Chapter;
import com.example.truyen.entity.ReadingHistory;
import com.example.truyen.entity.User;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.ChapterRepository;
import com.example.truyen.repository.ReadingHistoryRepository;
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
public class ReadingHistoryService {

    private final ReadingHistoryRepository readingHistoryRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;

    /**
     * Retrieve the current user's reading history with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ReadingHistoryResponse> getMyReadingHistory(int page, int size) {
        User currentUser = getCurrentUser();
        return readingHistoryRepository.findByUserIdOrderByReadAtDesc(currentUser.getId(), PageRequest.of(page, size))
                .map(this::convertToResponse);
    }

    /**
     * Retrieve reading status for a specific story.
     */
    @Transactional(readOnly = true)
    public ReadingHistoryResponse getReadingHistoryForStory(Long storyId) {
        User currentUser = getCurrentUser();
        return readingHistoryRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .map(this::convertToResponse)
                .orElse(null);
    }

    /**
     * Save or update reading progress for a specific story and chapter.
     */
    @Transactional
    public ReadingHistoryResponse saveReadingHistory(Long storyId, Long chapterId) {
        User currentUser = getCurrentUser();

        storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", chapterId));

        if (!chapter.getStory().getId().equals(storyId)) {
            throw new BadRequestException("Chapter does not belong to this story");
        }

        ReadingHistory history = readingHistoryRepository
                .findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElse(ReadingHistory.builder()
                        .user(currentUser)
                        .story(chapter.getStory())
                        .build());

        history.setChapter(chapter);
        return convertToResponse(readingHistoryRepository.save(history));
    }

    /**
     * Delete reading history for a specific story.
     */
    @Transactional
    public void deleteReadingHistory(Long storyId) {
        User currentUser = getCurrentUser();
        ReadingHistory history = readingHistoryRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reading history not found for this story"));

        readingHistoryRepository.delete(history);
    }

    /**
     * Delete all reading history for the current user.
     */
    @Transactional
    public void deleteAllReadingHistory() {
        User currentUser = getCurrentUser();
        Page<ReadingHistory> histories = readingHistoryRepository.findByUserIdOrderByReadAtDesc(
                currentUser.getId(), PageRequest.of(0, Integer.MAX_VALUE));

        readingHistoryRepository.deleteAll(histories.getContent());
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
     * Map ReadingHistory entity to ReadingHistoryResponse DTO.
     */
    private ReadingHistoryResponse convertToResponse(ReadingHistory history) {
        return ReadingHistoryResponse.builder()
                .id(history.getId())
                .userId(history.getUser().getId())
                .username(history.getUser().getUsername())
                .storyId(history.getStory().getId())
                .storyTitle(history.getStory().getTitle())
                .storyImage(history.getStory().getImage())
                .chapterId(history.getChapter().getId())
                .chapterNumber(history.getChapter().getChapterNumber())
                .chapterTitle(history.getChapter().getTitle())
                .readAt(history.getReadAt())
                .build();
    }
}