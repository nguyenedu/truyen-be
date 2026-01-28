package com.example.truyen.repository;

import com.example.truyen.entity.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    Page<Story> findByStatus(Story.Status status, Pageable pageable);

    Page<Story> findByIsHotTrue(Pageable pageable);

    @Query("SELECT s FROM Story s WHERE LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Story> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT s FROM Story s JOIN s.categories c WHERE c.id = :categoryId")
    Page<Story> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT s FROM Story s WHERE s.author.id = :authorId")
    Page<Story> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    List<Story> findTop10ByOrderByTotalViewsDesc();

    @Query("SELECT s FROM Story s ORDER BY s.createdAt DESC")
    Page<Story> findAllOrderByCreatedAtDesc(Pageable pageable);

    List<Story> findByAuthorId(Long authorId);

    @Query("SELECT s FROM Story s WHERE " +
            "(:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:authorId IS NULL OR s.author.id = :authorId) AND " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:minChapters IS NULL OR s.totalChapters >= :minChapters) AND " +
            "(:maxChapters IS NULL OR s.totalChapters <= :maxChapters) AND " +
            "(:startDate IS NULL OR s.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR s.createdAt <= :endDate)")
    Page<Story> filterStories(
            @Param("keyword") String keyword,
            @Param("authorId") Long authorId,
            @Param("status") Story.Status status,
            @Param("minChapters") Integer minChapters,
            @Param("maxChapters") Integer maxChapters,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    List<Story> findByStatusIn (List<Story.Status> statuses);

    @Modifying
    @Query("UPDATE Story s SET s.totalViews = COALESCE(s.totalViews, 0) + :increment WHERE s.id = :storyId")
    void incrementTotalViews(@Param("storyId") Long storyId, @Param("increment") int increment);
}