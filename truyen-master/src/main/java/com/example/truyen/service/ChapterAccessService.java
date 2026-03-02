package com.example.truyen.service;

import com.example.truyen.dto.response.UnlockedChapterResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChapterAccessService {

    boolean hasAccess(Long chapterId);

    void unlockChapter(Long chapterId);

    Page<UnlockedChapterResponse> getMyUnlockedChapters(Pageable pageable);
}
