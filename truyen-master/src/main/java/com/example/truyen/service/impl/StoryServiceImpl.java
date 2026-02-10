package com.example.truyen.service.impl;

import com.example.truyen.dto.event.SearchEvent;
import com.example.truyen.dto.request.StoryRequest;
import com.example.truyen.dto.response.StoryResponse;
import com.example.truyen.entity.*;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.kafka.producer.SearchProducer;
import com.example.truyen.repository.*;
import com.example.truyen.service.StoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final SearchProducer searchProducer;
    private final RatingRepository ratingRepository;

    // Lấy danh sách truyện
    @Transactional(readOnly = true)
    @Override
    public Page<StoryResponse> getAllStories(int page, int size) {
        return storyRepository.findAll(PageRequest.of(page, size)).map(this::convertToResponse);
    }

    // Lấy chi tiết truyện theo ID
    @Transactional(readOnly = true)
    @Override
    public StoryResponse getStoryById(Long id) {
        var story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", id));
        return convertToResponse(story);
    }

    // Tìm kiếm truyện và gửi event tracking
    @Transactional(readOnly = true)
    @Override
    public Page<StoryResponse> searchStories(String keyword, int page, int size) {
        var results = storyRepository.searchByTitle(keyword, PageRequest.of(page, size))
                .map(this::convertToResponse);

        // Gửi event search vào Kafka
        try {
            var searchEvent = SearchEvent.create(
                    keyword,
                    null,
                    (int) results.getTotalElements());
            searchProducer.sendSearchEvent(searchEvent);
        } catch (Exception e) {
            log.warn("Failed to send search event: {}", e.getMessage());
        }

        return results;
    }

    // Lấy truyện theo danh mục
    @Transactional(readOnly = true)
    @Override
    public Page<StoryResponse> getStoriesByCategory(Long categoryId, int page, int size) {
        return storyRepository.findByCategoryId(categoryId, PageRequest.of(page, size)).map(this::convertToResponse);
    }

    // Lấy danh sách truyện HOT
    @Override
    public Page<StoryResponse> getHotStories(int page, int size) {
        return storyRepository.findByIsHotTrue(PageRequest.of(page, size)).map(this::convertToResponse);
    }

    // Lấy danh sách truyện mới nhất
    @Transactional(readOnly = true)
    @Override
    public Page<StoryResponse> getLatestStories(int page, int size) {
        return storyRepository.findAllOrderByCreatedAtDesc(PageRequest.of(page, size)).map(this::convertToResponse);
    }

    // Tạo truyện mới
    @Transactional
    @Override
    public StoryResponse createStory(StoryRequest request) {
        var author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author", "id", request.getAuthorId()));

        Set<Category> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            categories = request.getCategoryIds().stream()
                    .map(categoryId -> categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId)))
                    .collect(Collectors.toSet());
        }

        var status = Story.Status.ONGOING;
        if (request.getStatus() != null) {
            try {
                status = Story.Status.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + request.getStatus());
            }
        }

        var story = Story.builder()
                .title(request.getTitle())
                .author(author)
                .description(request.getDescription())
                .image(request.getImage())
                .status(status)
                .categories(categories)
                .totalChapters(0)
                .totalViews(0)
                .isHot(false)
                .build();

        return convertToResponse(storyRepository.save(story));
    }

    // Cập nhật thông tin truyện
    @Transactional
    @Override
    public StoryResponse updateStory(Long id, StoryRequest request) {
        var story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", id));

        if (request.getAuthorId() != null) {
            var author = authorRepository.findById(request.getAuthorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author", "id", request.getAuthorId()));
            story.setAuthor(author);
        }

        if (request.getCategoryIds() != null) {
            var categories = request.getCategoryIds().stream()
                    .map(categoryId -> categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId)))
                    .collect(Collectors.toSet());
            story.setCategories(categories);
        }

        if (request.getTitle() != null)
            story.setTitle(request.getTitle());
        if (request.getDescription() != null)
            story.setDescription(request.getDescription());
        if (request.getImage() != null)
            story.setImage(request.getImage());
        if (request.getStatus() != null) {
            try {
                story.setStatus(Story.Status.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + request.getStatus());
            }
        }

        return convertToResponse(storyRepository.save(story));
    }

    // Xóa truyện
    @Transactional
    @Override
    public void deleteStory(Long id) {
        var story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", id));
        storyRepository.delete(story);
    }

    // Tăng lượt xem truyện
    @Transactional
    @Override
    public void increaseView(Long id) {
        var story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", id));
        story.setTotalViews(story.getTotalViews() + 1);
        storyRepository.save(story);
    }

    // Lọc truyện nâng cao
    @Transactional(readOnly = true)
    @Override
    public Page<StoryResponse> filterStories(
            String keyword,
            Long authorId,
            String status,
            Integer minChapters,
            Integer maxChapters,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<Long> categoryIds,
            int page,
            int size,
            String sort) {
        var sortParams = sort.split(",");
        var sortField = sortParams[0];
        var sortDir = sortParams.length > 1 ? sortParams[1] : "asc";

        var direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Story.Status storyStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                storyStatus = Story.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {

            }
        }

        var categoryCount = (categoryIds != null && !categoryIds.isEmpty())
                ? categoryIds.size()
                : null;

        return storyRepository.filterStories(
                keyword,
                authorId,
                storyStatus,
                minChapters,
                maxChapters,
                startDate,
                endDate,
                categoryIds,
                categoryCount,
                pageable).map(this::convertToResponse);
    }

    // Lấy danh sách truyện của tác giả
    @Transactional(readOnly = true)
    @Override
    public List<StoryResponse> getStoriesByAuthor(Long authorId) {
        return storyRepository.findByAuthorId(authorId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    private StoryResponse convertToResponse(Story story) {
        var categoryNames = story.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

        // Lấy đánh giá trung bình và tổng số đánh giá
        var averageRating = ratingRepository.getAverageRating(story.getId());
        var totalRatingsCount = ratingRepository.countByStoryId(story.getId());

        return StoryResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .authorName(story.getAuthor() != null ? story.getAuthor().getName() : null)
                .authorId(story.getAuthor() != null ? story.getAuthor().getId() : null)
                .description(story.getDescription())
                .image(story.getImage())
                .status(story.getStatus().name())
                .totalChapters(story.getTotalChapters())
                .totalViews(story.getTotalViews())
                .isHot(story.getIsHot())
                .categories(categoryNames)
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .averageRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0)
                .totalRatings(totalRatingsCount != null ? totalRatingsCount.intValue() : 0)
                .build();
    }
}
