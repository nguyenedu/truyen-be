package com.example.truyen.repository;

import com.example.truyen.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndStoryId(Long userId, Long storyId);

    Boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.story.id = :storyId")
    Double getAverageRating(@Param("storyId") Long storyId);

    Long countByStoryId(Long storyId);

    // Batch: Lấy điểm trung bình theo danh sách storyIds (tránh N+1)
    @Query("SELECT r.story.id, AVG(r.rating) FROM Rating r WHERE r.story.id IN :storyIds GROUP BY r.story.id")
    List<Object[]> getAverageRatingsByStoryIds(@Param("storyIds") List<Long> storyIds);

    // Batch: Đếm số đánh giá theo danh sách storyIds (tránh N+1)
    @Query("SELECT r.story.id, COUNT(r) FROM Rating r WHERE r.story.id IN :storyIds GROUP BY r.story.id")
    List<Object[]> countByStoryIds(@Param("storyIds") List<Long> storyIds);
}