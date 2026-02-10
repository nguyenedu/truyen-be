package com.example.truyen.service;

import com.example.truyen.dto.response.ReadingHistoryResponse;
import org.springframework.data.domain.Page;

// Interface ReadingHistoryService
public interface ReadingHistoryService {

    // Lấy lịch sử đọc của người dùng
    Page<ReadingHistoryResponse> getMyReadingHistory(int page, int size);

    // Lấy lịch sử đọc của người dùng cho truyện cụ thể
    ReadingHistoryResponse getReadingHistoryForStory(Long storyId);

    // Lưu tiến trình đọc
    ReadingHistoryResponse saveReadingHistory(Long storyId, Long chapterId, Integer chapterNumber);

    // Lưu tiến trình đọc (không có chapterNumber)
    ReadingHistoryResponse saveReadingHistory(Long storyId, Long chapterId);

    // Xóa lịch sử đọc một truyện
    void deleteReadingHistory(Long storyId);

    // Xóa toàn bộ lịch sử đọc
    void deleteAllReadingHistory();
}