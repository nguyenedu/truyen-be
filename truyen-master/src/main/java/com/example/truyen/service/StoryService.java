package com.example.truyen.service;

import com.example.truyen.dto.event.SearchEvent;
import com.example.truyen.dto.request.StoryRequest;
import com.example.truyen.dto.response.StoryResponse;
import com.example.truyen.entity.*;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.kafka.producer.SearchProducer;
import com.example.truyen.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class StoryService {

    private final StoryRepository storyRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final SearchProducer searchProducer;

    /**
     * Lấy danh sách tất cả truyện với phân trang.
     */
    @Transactional(readOnly = true)
    public Page<StoryResponse> getAllStories(int page, int size) {
        return storyRepository.findAll(PageRequest.of(page, size)).map(this::convertToResponse);
    }

    /**
     * Lấy chi tiết truyện theo ID.
     */
    @Transactional(readOnly = true)
    public StoryResponse getStoryById(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", id));
        return convertToResponse(story);
    }

    /**
     * Tìm kiếm truyện theo tiêu đề với phân trang.
     */
    @Transactional(readOnly = true)
    public Page<StoryResponse> searchStories(String keyword, int page, int size) {
        Page<StoryResponse> results = storyRepository.searchByTitle(keyword, PageRequest.of(page, size))
                .map(this::convertToResponse);

        // Gửi search event vào Kafka để analytics
        try {
            SearchEvent searchEvent = SearchEvent.create(
                    keyword,
                    null, // userId - có thể lấy từ SecurityContext nếu cần
                    (int) results.getTotalElements());
            searchProducer.sendSearchEvent(searchEvent);
        } catch (Exception e) {
            log.warn("Failed to send search event: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Lấy danh sách truyện thuộc một thể loại cụ thể.
     */
    @Transactional(readOnly = true)
    public Page<StoryResponse> getStoriesByCategory(Long categoryId, int page, int size) {
        return storyRepository.findByCategoryId(categoryId, PageRequest.of(page, size)).map(this::convertToResponse);
    }

    /**
     * Lấy danh sách truyện được hiển thị là HOT.
     */
    public Page<StoryResponse> getHotStories(int page, int size) {
        return storyRepository.findByIsHotTrue(PageRequest.of(page, size)).map(this::convertToResponse);
    }

    /**
     * Lấy danh sách truyện mới nhất sắp xếp theo ngày tạo.
     */
    @Transactional(readOnly = true)
    public Page<StoryResponse> getLatestStories(int page, int size) {
        return storyRepository.findAllOrderByCreatedAtDesc(PageRequest.of(page, size)).map(this::convertToResponse);
    }

    /**
     * Tạo truyện mới.
     */
    @Transactional
    public StoryResponse createStory(StoryRequest request) {
        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author", "id", request.getAuthorId()));

        Set<Category> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            categories = request.getCategoryIds().stream()
                    .map(categoryId -> categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId)))
                    .collect(Collectors.toSet());
        }

        Story.Status status = Story.Status.ONGOING;
        if (request.getStatus() != null) {
            try {
                status = Story.Status.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái không hợp lệ: " + request.getStatus());
            }
        }

        Story story = Story.builder()
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

    /**
     * Cập nhật thông tin truyện hiện có.
     */
    @Transactional
    public StoryResponse updateStory(Long id, StoryRequest request) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", id));

        if (request.getAuthorId() != null) {
            Author author = authorRepository.findById(request.getAuthorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author", "id", request.getAuthorId()));
            story.setAuthor(author);
        }

        if (request.getCategoryIds() != null) {
            Set<Category> categories = request.getCategoryIds().stream()
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
                throw new BadRequestException("Trạng thái không hợp lệ: " + request.getStatus());
            }
        }

        return convertToResponse(storyRepository.save(story));
    }

    /**
     * Xóa truyện theo ID.
     */
    @Transactional
    public void deleteStory(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", id));
        storyRepository.delete(story);
    }

    /**
     * Tăng tổng số lượt xem của truyện.
     */
    @Transactional
    public void increaseView(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", id));
        story.setTotalViews(story.getTotalViews() + 1);
        storyRepository.save(story);
    }

    /**
     * Lọc truyện nâng cao dựa trên nhiều tiêu chí.
     */
    @Transactional(readOnly = true)
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
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        String sortDir = sortParams.length > 1 ? sortParams[1] : "asc";

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Story.Status storyStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                storyStatus = Story.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {

            }
        }

        Integer categoryCount = (categoryIds != null && !categoryIds.isEmpty())
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

    /**
     * Lấy danh sách tất cả truyện của một tác giả cụ thể.
     */
    @Transactional(readOnly = true)
    public List<StoryResponse> getStoriesByAuthor(Long authorId) {
        return storyRepository.findByAuthorId(authorId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Chuyển đổi Story sang StoryResponse DTO.
     */
    private StoryResponse convertToResponse(Story story) {
        Set<String> categoryNames = story.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

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
                .build();
    }
}
