package com.example.truyen.repository;

import com.example.truyen.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByStoryIdOrderByChapterNumberAsc(Long storyId);

    Optional<Chapter> findByStoryIdAndChapterNumber(Long storyId, Integer chapterNumber);

    Long countByStoryId(Long storyId);

    @Query("SELECT MAX(c.chapterNumber) FROM Chapter c WHERE c.story.id = :storyId")
    Optional<Integer> findMaxChapterNumberByStoryId(@Param("storyId") Long storyId);

    Boolean existsByStoryIdAndChapterNumber(Long storyId, Integer chapterNumber);
}