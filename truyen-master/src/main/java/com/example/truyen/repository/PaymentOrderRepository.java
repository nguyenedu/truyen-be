package com.example.truyen.repository;

import com.example.truyen.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderCode(String orderCode);

    boolean existsByVnpTransactionNo(String vnpTransactionNo);

    Page<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<PaymentOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.status = 'SUCCESS' AND p.completedAt BETWEEN :start AND :end")
    long countSuccessOrdersByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.status = 'SUCCESS' AND p.completedAt BETWEEN :start AND :end")
    long sumRevenueByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
