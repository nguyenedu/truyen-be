package com.example.truyen.repository;

import com.example.truyen.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Page<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Favorite> findByUserIdAndStoryId(Long userId, Long storyId);

    Boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    void deleteByUserIdAndStoryId(Long userId, Long storyId);

    Long countByStoryId(Long storyId);
}