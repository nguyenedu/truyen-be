package com.example.truyen.service;

import com.example.truyen.dto.request.ChapterRequest;
import com.example.truyen.dto.response.ChapterResponse;

import java.util.List;

// Interface ChapterService
public interface ChapterService {

    // Lấy danh sách chương của truyện
    List<ChapterResponse> getChaptersByStoryId(Long storyId);

    // Lấy chi tiết chương và tăng lượt xem
    ChapterResponse getChapterById(Long id);

    // Lấy chi tiết chương theo số thứ tự và tăng lượt xem
    ChapterResponse getChapterByStoryAndNumber(Long storyId, Integer chapterNumber);

    // Tạo chương mới và cập nhật tổng số chương của truyện
    ChapterResponse createChapter(ChapterRequest request);

    // Cập nhật nội dung chương
    ChapterResponse updateChapter(Long id, ChapterRequest request);

    // Xóa chương và cập nhật tổng số chương của truyện
    void deleteChapter(Long id);
}