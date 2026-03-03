package com.example.truyen.repository;

import com.example.truyen.entity.UserChapterAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserChapterAccessRepository extends JpaRepository<UserChapterAccess, Long> {

    boolean existsByUserIdAndChapterId(Long userId, Long chapterId);

    Page<UserChapterAccess> findByUserIdOrderByAccessedAtDesc(Long userId, Pageable pageable);

    // Admin: xem danh sách user đã mở khóa 1 chương cụ thể
    Page<UserChapterAccess> findByChapterIdOrderByAccessedAtDesc(Long chapterId, Pageable pageable);

    // VIP stats: tổng xu đã tiêu thụ từ tất cả user
    @Query("SELECT COALESCE(SUM(a.coinsSpent), 0) FROM UserChapterAccess a")
    long sumTotalCoinsSpent();

    // VIP stats: tổng số lượt mở khóa
    long count();
}
