package com.example.truyen.repository;

import com.example.truyen.entity.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}