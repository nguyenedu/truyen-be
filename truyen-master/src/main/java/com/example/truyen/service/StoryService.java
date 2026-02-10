package com.example.truyen.service;

import com.example.truyen.dto.request.StoryFilterCriteria;
import com.example.truyen.dto.request.StoryRequest;
import com.example.truyen.dto.response.StoryResponse;
import org.springframework.data.domain.Page;

import java.util.List;

// Interface StoryService
public interface StoryService {

    // Lấy danh sách truyện
    Page<StoryResponse> getAllStories(int page, int size);

    // Lấy chi tiết truyện theo ID
    StoryResponse getStoryById(Long id);

    // Tìm kiếm truyện và gửi event tracking
    Page<StoryResponse> searchStories(String keyword, int page, int size);

    // Lấy truyện theo danh mục
    Page<StoryResponse> getStoriesByCategory(Long categoryId, int page, int size);

    // Lấy danh sách truyện HOT
    Page<StoryResponse> getHotStories(int page, int size);

    // Lấy danh sách truyện mới nhất
    Page<StoryResponse> getLatestStories(int page, int size);

    // Tạo truyện mới
    StoryResponse createStory(StoryRequest request);

    // Cập nhật thông tin truyện
    StoryResponse updateStory(Long id, StoryRequest request);

    // Xóa truyện
    void deleteStory(Long id);

    // Tăng lượt xem truyện
    void increaseView(Long id);

    // Lọc truyện nâng cao
    Page<StoryResponse> filterStories(StoryFilterCriteria criteria);

    // Lấy danh sách truyện của tác giả
    List<StoryResponse> getStoriesByAuthor(Long authorId);
}
