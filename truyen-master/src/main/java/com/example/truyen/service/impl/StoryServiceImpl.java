package com.example.truyen.service.impl;

import com.example.truyen.dto.event.SearchEvent;
import com.example.truyen.dto.request.StoryFilterCriteria;
import com.example.truyen.dto.request.StoryRequest;
import com.example.truyen.dto.response.StoryResponse;
import com.example.truyen.entity.*;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.kafka.producer.SearchProducer;
import com.example.truyen.repository.*;
import com.example.truyen.service.MinIoService;
import com.example.truyen.service.StoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final SearchProducer searchProducer;
    private final MinIoService minIoService;
    private final RatingRepository ratingRepository;

    // Lấy danh sách truyện (batch rating query)
    @Transactional(readOnly = true)
    @Override
    public Page<StoryResponse> getAllStories(int page, int size) {
        var storiesPage = storyRepository.findAll(PageRequest.of(page, size));
        return convertToResponsePage(storiesPage);
    }

    // Lấy chi tiết truyện theo ID (single query vẫn OK)
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
        var storiesPage = storyRepository.searchByTitle(keyword, PageRequest.of(page, size));
        var results = convertToResponsePage(storiesPage);

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
        var storiesPage = storyRepository.findByCategoryId(categoryId, PageRequest.of(page, size));
        return convertToResponsePage(storiesPage);
    }

    // Lấy danh sách truyện HOT
    @Override
    public Page<StoryResponse> getHotStories(int page, int size) {
        var storiesPage = storyRepository.findByIsHotTrue(PageRequest.of(page, size));
        return convertToResponsePage(storiesPage);
    }

    // Lấy danh sách truyện mới nhất
    @Transactional(readOnly = true)
    @Override
    public Page<StoryResponse> getLatestStories(int page, int size) {
        var storiesPage = storyRepository.findAllOrderByCreatedAtDesc(PageRequest.of(page, size));
        return convertToResponsePage(storiesPage);
    }

    // Tạo truyện mới
    @Transactional
    @Override
    public StoryResponse createStory(StoryRequest request) {
        if (request.getCoverImage() != null && !request.getCoverImage().isEmpty()) {
            String imageUrl = minIoService.uploadFile(request.getCoverImage(), "story-covers");
            request.setImage(imageUrl);
        }

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

        if (request.getCoverImage() != null && !request.getCoverImage().isEmpty()) {
            String imageUrl = minIoService.uploadFile(request.getCoverImage(), "story-covers");
            request.setImage(imageUrl);
        }

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
    public Page<StoryResponse> filterStories(StoryFilterCriteria criteria) {
        var sortParams = criteria.getSort().split(",");
        var sortField = sortParams[0];
        var sortDir = sortParams.length > 1 ? sortParams[1] : "asc";

        var direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), Sort.by(direction, sortField));

        Story.Status storyStatus = null;
        if (criteria.getStatus() != null && !criteria.getStatus().trim().isEmpty()) {
            try {
                storyStatus = Story.Status.valueOf(criteria.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {

            }
        }

        var categoryCount = (criteria.getCategoryIds() != null && !criteria.getCategoryIds().isEmpty())
                ? criteria.getCategoryIds().size()
                : null;

        var storiesPage = storyRepository.filterStories(
                criteria.getKeyword(),
                criteria.getAuthorId(),
                storyStatus,
                criteria.getMinChapters(),
                criteria.getMaxChapters(),
                criteria.getStartDate(),
                criteria.getEndDate(),
                criteria.getCategoryIds(),
                categoryCount,
                pageable);
        return convertToResponsePage(storiesPage);
    }

    // Lấy danh sách truyện của tác giả
    @Transactional(readOnly = true)
    @Override
    public List<StoryResponse> getStoriesByAuthor(Long authorId) {
        var stories = storyRepository.findByAuthorId(authorId);
        return convertToResponseList(stories);
    }

    // ===== BATCH CONVERSION (tránh N+1 query) =====

    // Chuyển đổi Page<Story> sang Page<StoryResponse> với batch rating query
    private Page<StoryResponse> convertToResponsePage(Page<Story> storiesPage) {
        var stories = storiesPage.getContent();
        var responses = convertToResponseList(stories);
        return new PageImpl<>(responses, storiesPage.getPageable(), storiesPage.getTotalElements());
    }

    // Chuyển đổi List<Story> sang List<StoryResponse> — chỉ 2 queries cho rating
    private List<StoryResponse> convertToResponseList(List<Story> stories) {
        if (stories.isEmpty()) {
            return Collections.emptyList();
        }

        // 1 query lấy toàn bộ averageRating cho tất cả stories
        var storyIds = stories.stream().map(Story::getId).toList();
        Map<Long, Double> avgRatingMap = buildAvgRatingMap(storyIds);
        Map<Long, Long> countRatingMap = buildCountRatingMap(storyIds);

        // Map kết quả cho từng story (không cần query thêm)
        return stories.stream()
                .map(story -> buildStoryResponse(story, avgRatingMap, countRatingMap))
                .toList();
    }

    // Chuyển đổi 1 Story (dùng cho getById, create, update — chỉ 1 story nên N+1
    // không ảnh hưởng)
    private StoryResponse convertToResponse(Story story) {
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
                .categories(story.getCategories().stream().map(Category::getName).collect(Collectors.toSet()))
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .averageRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0)
                .totalRatings(totalRatingsCount != null ? totalRatingsCount.intValue() : 0)
                .build();
    }

    // Build StoryResponse từ pre-fetched rating maps
    private StoryResponse buildStoryResponse(Story story, Map<Long, Double> avgRatingMap,
            Map<Long, Long> countRatingMap) {
        var avgRating = avgRatingMap.getOrDefault(story.getId(), 0.0);
        var totalRatings = countRatingMap.getOrDefault(story.getId(), 0L);

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
                .categories(story.getCategories().stream().map(Category::getName).collect(Collectors.toSet()))
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .totalRatings(totalRatings.intValue())
                .build();
    }

    // Build map storyId → averageRating từ batch query
    private Map<Long, Double> buildAvgRatingMap(List<Long> storyIds) {
        return ratingRepository.getAverageRatingsByStoryIds(storyIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]));
    }

    // Build map storyId → totalRatings từ batch query
    private Map<Long, Long> buildCountRatingMap(List<Long> storyIds) {
        return ratingRepository.countByStoryIds(storyIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));
    }
}
