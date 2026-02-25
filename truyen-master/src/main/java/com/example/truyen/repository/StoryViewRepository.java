package com.example.truyen.repository;

import com.example.truyen.entity.StoryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, Long> {

    // Đếm lượt xem theo storyId từ thời điểm cụ thể
    Long countByStoryIdAndViewedAtAfter(Long storyId, LocalDateTime since);

    // Dashboard: Tổng lượt xem sau thời điểm
    long countByViewedAtAfter(LocalDateTime since);

    // Dashboard: Tổng lượt xem trong khoảng thời gian
    long countByViewedAtBetween(LocalDateTime start, LocalDateTime end);
}
