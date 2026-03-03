package com.example.truyen.repository;

import com.example.truyen.entity.StoryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, Long> {

    Long countByStoryIdAndViewedAtAfter(Long storyId, LocalDateTime since);

    long countByViewedAtAfter(LocalDateTime since);

    long countByViewedAtBetween(LocalDateTime start, LocalDateTime end);

    // Lấy số view cao nhất của bất kỳ story nào trong khoảng thời gian (1 query duy
    // nhất)
    @Query("SELECT COALESCE(MAX(cnt), 1) FROM " +
            "(SELECT COUNT(v) as cnt FROM StoryView v WHERE v.viewedAt > :since GROUP BY v.storyId) sub")
    long findMaxViewCountSince(@Param("since") LocalDateTime since);
}
