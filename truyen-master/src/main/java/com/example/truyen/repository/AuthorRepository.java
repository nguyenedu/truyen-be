package com.example.truyen.repository;

import com.example.truyen.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    // Dashboard: Đếm author tạo sau thời điểm
    long countByCreatedAtAfter(LocalDateTime since);

    // Dashboard: Đếm author tạo trong khoảng thời gian
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<Author> findByName(String name);

    Boolean existsByName(String name);

    List<Author> findByNameContainingIgnoreCase(String name);
}