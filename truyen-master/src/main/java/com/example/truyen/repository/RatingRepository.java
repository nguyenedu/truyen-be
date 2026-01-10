package com.example.truyen.repository;

import com.example.truyen.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndStoryId(Long userId, Long storyId);

    Boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.story.id = :storyId")
    Double getAverageRatingByStoryId(@Param("storyId") Long storyId);

    Long countByStoryId(Long storyId);
}