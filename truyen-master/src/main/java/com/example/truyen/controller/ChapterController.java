package com.example.truyen.controller;

import com.example.truyen.dto.request.ChapterRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.ChapterResponse;
import com.example.truyen.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    /**
     * Get all chapters for a specific story.
     */
    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> getChaptersByStoryId(
            @PathVariable Long storyId) {
        List<ChapterResponse> chapters = chapterService.getChaptersByStoryId(storyId);
        return ResponseEntity.ok(ApiResponse.success("Chapters retrieved successfully", chapters));
    }

    /**
     * Get chapter details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChapterResponse>> getChapterById(@PathVariable Long id) {
        ChapterResponse chapter = chapterService.getChapterById(id);
        return ResponseEntity.ok(ApiResponse.success("Chapter retrieved successfully", chapter));
    }

    /**
     * Get chapter by story ID and chapter number.
     */
    @GetMapping("/story/{storyId}/number/{chapterNumber}")
    public ResponseEntity<ApiResponse<ChapterResponse>> getChapterByStoryAndNumber(
            @PathVariable Long storyId,
            @PathVariable Integer chapterNumber) {
        ChapterResponse chapter = chapterService.getChapterByStoryAndNumber(storyId, chapterNumber);
        return ResponseEntity.ok(ApiResponse.success("Chapter retrieved successfully", chapter));
    }

    /**
     * Create a new chapter.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ChapterResponse>> createChapter(
            @Valid @RequestBody ChapterRequest request) {
        ChapterResponse chapter = chapterService.createChapter(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Chapter created successfully", chapter));
    }

    /**
     * Update an existing chapter.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ChapterResponse>> updateChapter(
            @PathVariable Long id,
            @Valid @RequestBody ChapterRequest request) {
        ChapterResponse chapter = chapterService.updateChapter(id, request);
        return ResponseEntity.ok(ApiResponse.success("Chapter updated successfully", chapter));
    }

    /**
     * Delete a chapter.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteChapter(@PathVariable Long id) {
        chapterService.deleteChapter(id);
        return ResponseEntity.ok(ApiResponse.success("Chapter deleted successfully", null));
    }
}