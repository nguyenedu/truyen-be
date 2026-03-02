package com.example.truyen.repository;

import com.example.truyen.entity.UserChapterAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserChapterAccessRepository extends JpaRepository<UserChapterAccess, Long> {

    boolean existsByUserIdAndChapterId(Long userId, Long chapterId);

    Page<UserChapterAccess> findByUserIdOrderByAccessedAtDesc(Long userId, Pageable pageable);
}
