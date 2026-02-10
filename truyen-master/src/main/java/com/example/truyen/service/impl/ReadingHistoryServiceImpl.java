package com.example.truyen.service.impl;

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
import com.example.truyen.service.ReadingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReadingHistoryServiceImpl implements ReadingHistoryService {

    private final ReadingHistoryRepository readingHistoryRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;

    // Lấy lịch sử đọc của người dùng
    @Transactional(readOnly = true)
    @Override
    public Page<ReadingHistoryResponse> getMyReadingHistory(int page, int size) {
        User currentUser = getCurrentUser();
        return readingHistoryRepository
                .findByUserIdOrderByReadAtDesc(currentUser.getId(), PageRequest.of(page, size))
                .map(this::convertToResponse);
    }

    // Lấy lịch sử đọc của người dùng cho truyện cụ thể
    @Transactional(readOnly = true)
    @Override
    public ReadingHistoryResponse getReadingHistoryForStory(Long storyId) {
        User currentUser = getCurrentUser();
        ReadingHistory history = readingHistoryRepository
                .findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElse(null);
        return history != null ? convertToResponse(history) : null;
    }

    // Lưu tiến trình đọc
    @Transactional
    @Override
    public ReadingHistoryResponse saveReadingHistory(Long storyId, Long chapterId, Integer chapterNumber) {
        User currentUser = getCurrentUser();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        Chapter chapter = null;
        if (chapterId != null) {
            chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", chapterId));
        }

        ReadingHistory history = readingHistoryRepository
                .findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElse(ReadingHistory.builder()
                        .user(currentUser)
                        .story(story)
                        .build());

        history.setChapter(chapter);

        return convertToResponse(readingHistoryRepository.save(history));
    }

    // Lưu tiến trình đọc (không có chapterNumber)
    @Transactional
    @Override
    public ReadingHistoryResponse saveReadingHistory(Long storyId, Long chapterId) {
        return saveReadingHistory(storyId, chapterId, null);
    }

    // Xóa lịch sử đọc một truyện
    @Transactional
    @Override
    public void deleteReadingHistory(Long storyId) {
        User currentUser = getCurrentUser();
        ReadingHistory history = readingHistoryRepository
                .findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reading history not found"));
        readingHistoryRepository.delete(history);
    }

    // Xóa toàn bộ lịch sử đọc
    @Transactional
    @Override
    public void deleteAllReadingHistory() {
        User currentUser = getCurrentUser();
        readingHistoryRepository.deleteAll(
                readingHistoryRepository.findByUserIdOrderByReadAtDesc(currentUser.getId(),
                        PageRequest.of(0, Integer.MAX_VALUE)).getContent());
    }

    // Lấy thông tin người dùng hiện tại
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Chuyển đổi entity sang DTO
    private ReadingHistoryResponse convertToResponse(ReadingHistory history) {
        return ReadingHistoryResponse.builder()
                .id(history.getId())
                .userId(history.getUser().getId())
                .username(history.getUser().getUsername())
                .storyId(history.getStory().getId())
                .storyTitle(history.getStory().getTitle())
                .storyImage(history.getStory().getImage())
                .chapterId(history.getChapter() != null ? history.getChapter().getId() : null)
                .chapterNumber(history.getChapter() != null ? history.getChapter().getChapterNumber() : null)
                .chapterTitle(history.getChapter() != null ? history.getChapter().getTitle() : null)
                .readAt(history.getReadAt())
                .build();
    }
}
