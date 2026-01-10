package com.example.truyen.service;

import com.example.truyen.dto.response.ReadingHistoryResponse;
import com.example.truyen.entity.Chapter;
import com.example.truyen.entity.ReadingHistory;
import com.example.truyen.entity.Story;
import com.example.truyen.entity.User;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.ChapterRepository;
import com.example.truyen.repository.ReadingHistoryRepository;
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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReadingHistoryService {

    private final ReadingHistoryRepository readingHistoryRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;

    // Lấy lịch sử đọc của user
    @Transactional(readOnly = true)
    public Page<ReadingHistoryResponse> getMyReadingHistory(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadingHistory> histories = readingHistoryRepository.findByUserIdOrderByReadAtDesc(
                currentUser.getId(), pageable);
        return histories.map(this::convertToResponse);
    }

    // Lấy lịch sử đọc của 1 truyện
    @Transactional(readOnly = true)
    public ReadingHistoryResponse getReadingHistoryForStory(Long storyId) {
        User currentUser = getCurrentUser();

        Optional<ReadingHistory> history = readingHistoryRepository.findByUserIdAndStoryId(
                currentUser.getId(), storyId);

        return history.map(this::convertToResponse).orElse(null);
    }

    // Lưu/Cập nhật lịch sử đọc
    @Transactional
    public ReadingHistoryResponse saveReadingHistory(Long storyId, Long chapterId) {
        User currentUser = getCurrentUser();

        // Kiểm tra truyện tồn tại
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", storyId));

        // Kiểm tra chương tồn tại
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chương", "id", chapterId));

        // Kiểm tra chương có thuộc truyện không
        if (!chapter.getStory().getId().equals(storyId)) {
            throw new ResourceNotFoundException("Chương không thuộc truyện này");
        }

        // Tìm hoặc tạo mới reading history
        ReadingHistory history = readingHistoryRepository
                .findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElse(ReadingHistory.builder()
                        .user(currentUser)
                        .story(story)
                        .build());

        // Cập nhật chương đang đọc
        history.setChapter(chapter);

        ReadingHistory savedHistory = readingHistoryRepository.save(history);
        return convertToResponse(savedHistory);
    }

    // Xóa lịch sử đọc của 1 truyện
    @Transactional
    public void deleteReadingHistory(Long storyId) {
        User currentUser = getCurrentUser();

        ReadingHistory history = readingHistoryRepository.findByUserIdAndStoryId(
                        currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch sử đọc không tồn tại"));

        readingHistoryRepository.delete(history);
    }

    // Xóa toàn bộ lịch sử đọc
    @Transactional
    public void deleteAllReadingHistory() {
        User currentUser = getCurrentUser();
        Page<ReadingHistory> histories = readingHistoryRepository.findByUserIdOrderByReadAtDesc(
                currentUser.getId(), PageRequest.of(0, Integer.MAX_VALUE));

        readingHistoryRepository.deleteAll(histories.getContent());
    }

    // Lấy user hiện tại
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }

    // Convert Entity sang Response DTO
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