package com.example.truyen.service;

import com.example.truyen.dto.request.StoryRequest;
import com.example.truyen.dto.response.StoryResponse;
import com.example.truyen.entity.Author;
import com.example.truyen.entity.Category;
import com.example.truyen.entity.Story;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.AuthorRepository;
import com.example.truyen.repository.CategoryRepository;
import com.example.truyen.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;

    // Lấy tất cả truyện (có phân trang)
    @Transactional(readOnly = true)
    public Page<StoryResponse> getAllStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> stories = storyRepository.findAll(pageable);
        return stories.map(this::convertToResponse);
    }

    // Lấy chi tiết 1 truyện
    @Transactional(readOnly = true)
    public StoryResponse getStoryById(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", id));
        return convertToResponse(story);
    }

    // Tìm kiếm truyện theo tiêu đề
    @Transactional(readOnly = true)
    public Page<StoryResponse> searchStories(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> stories = storyRepository.searchByTitle(keyword, pageable);
        return stories.map(this::convertToResponse);
    }

    // Lấy truyện theo thể loại
    @Transactional(readOnly = true)
    public Page<StoryResponse> getStoriesByCategory(Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> stories = storyRepository.findByCategoryId(categoryId, pageable);
        return stories.map(this::convertToResponse);
    }

    // Lấy truyện HOT
    public Page<StoryResponse> getHotStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> stories = storyRepository.findByIsHotTrue(pageable);
        return stories.map(this::convertToResponse);
    }

    // Lấy truyện mới nhất
    @Transactional(readOnly = true)
    public Page<StoryResponse> getLatestStories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> stories = storyRepository.findAllOrderByCreatedAtDesc(pageable);
        return stories.map(this::convertToResponse);
    }

    // Tạo truyện mới
    @Transactional
    public StoryResponse createStory(StoryRequest request) {
        // Kiểm tra author tồn tại
        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Tác giả", "id", request.getAuthorId()));

        // Lấy categories
        Set<Category> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            categories = request.getCategoryIds().stream()
                    .map(categoryId -> categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new ResourceNotFoundException("Thể loại", "id", categoryId)))
                    .collect(Collectors.toSet());
        }

        // Parse status
        Story.Status status = Story.Status.ONGOING;
        if (request.getStatus() != null) {
            try {
                status = Story.Status.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Status không hợp lệ: " + request.getStatus());
            }
        }

        // Tạo story
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

        Story savedStory = storyRepository.save(story);
        return convertToResponse(savedStory);
    }

    // Cập nhật truyện
    @Transactional
    public StoryResponse updateStory(Long id, StoryRequest request) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", id));

        // Cập nhật author
        if (request.getAuthorId() != null) {
            Author author = authorRepository.findById(request.getAuthorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tác giả", "id", request.getAuthorId()));
            story.setAuthor(author);
        }

        // Cập nhật categories
        if (request.getCategoryIds() != null) {
            Set<Category> categories = request.getCategoryIds().stream()
                    .map(categoryId -> categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new ResourceNotFoundException("Thể loại", "id", categoryId)))
                    .collect(Collectors.toSet());
            story.setCategories(categories);
        }

        // Cập nhật thông tin khác
        if (request.getTitle() != null) story.setTitle(request.getTitle());
        if (request.getDescription() != null) story.setDescription(request.getDescription());
        if (request.getImage() != null) story.setImage(request.getImage());
        if (request.getStatus() != null) {
            try {
                story.setStatus(Story.Status.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Status không hợp lệ: " + request.getStatus());
            }
        }

        Story updatedStory = storyRepository.save(story);
        return convertToResponse(updatedStory);
    }

    // Xóa truyện
    @Transactional
    public void deleteStory(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", id));
        storyRepository.delete(story);
    }

    // Tăng lượt xem
    @Transactional
    public void increaseView(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", id));
        story.setTotalViews(story.getTotalViews() + 1);
        storyRepository.save(story);
    }

    @Transactional(readOnly = true)
    public Page<StoryResponse> filterStories(
            String keyword,
            Long authorId,
            String status,
            Integer minChapters,
            Integer maxChapters,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size,
            String sort
    ) {
        // Parse sort
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        String sortDir = sortParams.length > 1 ? sortParams[1] : "asc";

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        // Parse status
        Story.Status storyStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                storyStatus = Story.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        // Call repository
        Page<Story> stories = storyRepository.filterStories(
                keyword,
                authorId,
                storyStatus,
                minChapters,
                maxChapters,
                startDate,
                endDate,
                pageable
        );

        return stories.map(this::convertToResponse);
    }

    public List<StoryResponse> getStoriesByAuthor(Long authorId) {
        return storyRepository.findByAuthorId(authorId)
                .stream()
                .map(story -> {
                    StoryResponse res = new StoryResponse();
                    res.setId(story.getId());
                    res.setTitle(story.getTitle());

                    Author author = story.getAuthor();
                    if (author != null) {
                        res.setAuthorId(author.getId());
                        res.setAuthorName(author.getName());
                    }

                    return res;
                })
                .toList();
    }

    // Convert Entity sang Response DTO
    private StoryResponse convertToResponse(Story story) {
        Set<String> categoryNames = story.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

        return StoryResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .authorName(story.getAuthor() != null ? story.getAuthor().getName() : null)
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