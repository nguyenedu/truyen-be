package com.example.truyen.service;

public interface ChapterAccessService {

    boolean hasAccess(Long chapterId);

    void unlockChapter(Long chapterId);
}
