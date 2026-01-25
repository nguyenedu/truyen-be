package com.example.truyen.service;

import com.example.truyen.dto.request.ChapterRequest;
import com.example.truyen.dto.response.ChapterResponse;
import com.example.truyen.entity.Chapter;
import com.example.truyen.entity.Story;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.ChapterRepository;
import com.example.truyen.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final StoryRepository storyRepository;

    // Lấy tất cả chương của 1 truyện
    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersByStoryId(Long storyId) {
        // Kiểm tra truyện tồn tại
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", storyId));

        List<Chapter> chapters = chapterRepository.findByStoryIdOrderByChapterNumberAsc(storyId);
        return chapters.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Lấy chi tiết 1 chương
    @Transactional
    public ChapterResponse getChapterById(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chương", "id", id));

        // Tăng lượt xem chương
        chapter.setViews(chapter.getViews() + 1);
        chapterRepository.save(chapter);

        return convertToResponse(chapter);
    }

    // Lấy chương theo Idstory và chapter number
    @Transactional
    public ChapterResponse getChapterByStoryAndNumber(Long storyId, Integer chapterNumber) {
        Chapter chapter = chapterRepository.findByStoryIdAndChapterNumber(storyId, chapterNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chương " + chapterNumber + " của truyện id " + storyId + " không tồn tại"));

        // Tăng lượt xem chương
        chapter.setViews(chapter.getViews() + 1);
        chapterRepository.save(chapter);

        return convertToResponse(chapter);
    }

    // Tạo chương mới
    @Transactional
    public ChapterResponse createChapter(ChapterRequest request) {
        // Kiểm tra truyện tồn tại
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Truyện", "id", request.getStoryId()));

        // Kiểm tra chapter number đã tồn tại chưa
        if (chapterRepository.existsByStoryIdAndChapterNumber(request.getStoryId(), request.getChapterNumber())) {
            throw new BadRequestException("Chương " + request.getChapterNumber() + " đã tồn tại");
        }

        // Tạo chapter
        Chapter chapter = Chapter.builder()
                .story(story)
                .chapterNumber(request.getChapterNumber())
                .title(request.getTitle())
                .content(request.getContent())
                .views(0)
                .build();

        Chapter savedChapter = chapterRepository.save(chapter);

        // Cập nhật total chapters của story
        Long totalChapters = chapterRepository.countByStoryId(request.getStoryId());
        story.setTotalChapters(totalChapters.intValue());
        storyRepository.save(story);

        return convertToResponse(savedChapter);
    }

    // Cập nhật chương
    @Transactional
    public ChapterResponse updateChapter(Long id, ChapterRequest request) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chương", "id", id));

        // Cập nhật thông tin
        if (request.getChapterNumber() != null) {
            // Kiểm tra chapter number mới có bị trùng không
            if (!chapter.getChapterNumber().equals(request.getChapterNumber())) {
                if (chapterRepository.existsByStoryIdAndChapterNumber(
                        chapter.getStory().getId(), request.getChapterNumber())) {
                    throw new BadRequestException("Chương " + request.getChapterNumber() + " đã tồn tại");
                }
                chapter.setChapterNumber(request.getChapterNumber());
            }
        }

        if (request.getTitle() != null) chapter.setTitle(request.getTitle());
        if (request.getContent() != null) chapter.setContent(request.getContent());

        Chapter updatedChapter = chapterRepository.save(chapter);
        return convertToResponse(updatedChapter);
    }

    // Xóa chương
    @Transactional
    public void deleteChapter(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chương", "id", id));

        Long storyId = chapter.getStory().getId();
        chapterRepository.delete(chapter);

        // Cập nhật lại total chapters của story
        Long totalChapters = chapterRepository.countByStoryId(storyId);
        Story story = storyRepository.findById(storyId).orElse(null);
        if (story != null) {
            story.setTotalChapters(totalChapters.intValue());
            storyRepository.save(story);
        }
    }

    // Convert Entity sang Response DTO
    private ChapterResponse convertToResponse(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .storyId(chapter.getStory().getId())
                .storyTitle(chapter.getStory().getTitle())
                .chapterNumber(chapter.getChapterNumber())
                .title(chapter.getTitle())
                .content(chapter.getContent())
                .views(chapter.getViews())
                .createdAt(chapter.getCreatedAt())
                .build();
    }
}