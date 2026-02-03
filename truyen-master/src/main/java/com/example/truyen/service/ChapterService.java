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

    /**
     * Retrieve all chapters for a specific story, ordered by chapter number.
     */
    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersByStoryId(Long storyId) {
        storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));

        return chapterRepository.findByStoryIdOrderByChapterNumberAsc(storyId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve chapter details by ID and increment view count.
     */
    @Transactional
    public ChapterResponse getChapterById(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", id));

        chapter.setViews(chapter.getViews() + 1);
        chapterRepository.save(chapter);

        return convertToResponse(chapter);
    }

    /**
     * Retrieve chapter details by story ID and chapter number, incrementing view
     * count.
     */
    @Transactional
    public ChapterResponse getChapterByStoryAndNumber(Long storyId, Integer chapterNumber) {
        Chapter chapter = chapterRepository.findByStoryIdAndChapterNumber(storyId, chapterNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chapter " + chapterNumber + " for story ID " + storyId + " not found"));

        chapter.setViews(chapter.getViews() + 1);
        chapterRepository.save(chapter);

        return convertToResponse(chapter);
    }

    /**
     * Create a new chapter and update the story's total chapter count.
     */
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

    /**
     * Update an existing chapter's details.
     */
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
     * Delete a chapter and update the story's total chapter count.
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
     * Map Chapter entity to ChapterResponse DTO.
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