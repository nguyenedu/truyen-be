package com.example.truyen.repository;

import com.example.truyen.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

        // Dashboard: Đếm user tạo sau thời điểm
        long countByCreatedAtAfter(LocalDateTime since);

        // Dashboard: Đếm user tạo trong khoảng thời gian
        long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

        // Dashboard: Đếm user theo ngày cho biểu đồ
        @Query("SELECT FUNCTION('DATE', u.createdAt) as date, COUNT(u) as cnt " +
                        "FROM User u WHERE u.createdAt BETWEEN :start AND :end " +
                        "GROUP BY FUNCTION('DATE', u.createdAt) ORDER BY date ASC")
        List<Object[]> countUsersByDateRange(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        Optional<User> findByUsername(String username);

        Optional<User> findByEmail(String email);

        Boolean existsByUsername(String username);

        Boolean existsByEmail(String email);

        Optional<User> findByResetPasswordToken(String token);

        @Query("SELECT u FROM User u WHERE " +
                        "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
}