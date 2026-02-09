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

    // Lấy danh sách chương của truyện (sắp xếp tăng dần theo số chương)
    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersByStoryId(Long storyId) {
        storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        return chapterRepository.findByStoryIdOrderByChapterNumberAsc(storyId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Lấy chi tiết chương và tăng lượt xem
    @Transactional
    public ChapterResponse getChapterById(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));

        chapter.setViews(chapter.getViews() + 1);
        chapterRepository.save(chapter);

        return convertToResponse(chapter);
    }

    // Lấy chi tiết chương theo số thứ tự và tăng lượt xem
    @Transactional
    public ChapterResponse getChapterByStoryAndNumber(Long storyId, Integer chapterNumber) {
        Chapter chapter = chapterRepository.findByStoryIdAndChapterNumber(storyId, chapterNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chapter " + chapterNumber + " not found for story ID " + storyId));

        chapter.setViews(chapter.getViews() + 1);
        chapterRepository.save(chapter);

        return convertToResponse(chapter);
    }

    // Tạo chương mới và cập nhật tổng số chương của truyện
    @Transactional
    public ChapterResponse createChapter(ChapterRequest request) {
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", request.getStoryId()));

        if (chapterRepository.existsByStoryIdAndChapterNumber(request.getStoryId(), request.getChapterNumber())) {
            throw new BadRequestException("Chapter " + request.getChapterNumber() + " already exists");
        }

        Chapter chapter = Chapter.builder()
                .story(story)
                .chapterNumber(request.getChapterNumber())
                .title(request.getTitle())
                .content(request.getContent())
                .views(0)
                .build();

        Chapter savedChapter = chapterRepository.save(chapter);

        Long totalChapters = chapterRepository.countByStoryId(request.getStoryId());
        story.setTotalChapters(totalChapters.intValue());
        storyRepository.save(story);

        return convertToResponse(savedChapter);
    }

    // Cập nhật nội dung chương
    @Transactional
    public ChapterResponse updateChapter(Long id, ChapterRequest request) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));

        if (request.getChapterNumber() != null && !chapter.getChapterNumber().equals(request.getChapterNumber())) {
            if (chapterRepository.existsByStoryIdAndChapterNumber(
                    chapter.getStory().getId(), request.getChapterNumber())) {
                throw new BadRequestException("Chapter " + request.getChapterNumber() + " already exists");
            }
            chapter.setChapterNumber(request.getChapterNumber());
        }

        if (request.getTitle() != null)
            chapter.setTitle(request.getTitle());
        if (request.getContent() != null)
            chapter.setContent(request.getContent());

        return convertToResponse(chapterRepository.save(chapter));
    }

    /**
     * Delete chapter and update total chapters of the story.
     */
    @Transactional
    public void deleteChapter(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));

        Long storyId = chapter.getStory().getId();
        chapterRepository.delete(chapter);

        Long totalChapters = chapterRepository.countByStoryId(storyId);
        storyRepository.findById(storyId).ifPresent(story -> {
            story.setTotalChapters(totalChapters.intValue());
            storyRepository.save(story);
        });
    }

    /**
     * Convert Chapter to ChapterResponse DTO.
     */
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