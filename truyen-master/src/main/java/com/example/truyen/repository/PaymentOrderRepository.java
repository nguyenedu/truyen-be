package com.example.truyen.repository;

import com.example.truyen.entity.PaymentOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderCode(String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentOrder p WHERE p.orderCode = :orderCode")
    Optional<PaymentOrder> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    boolean existsByVnpTransactionNo(String vnpTransactionNo);

    Page<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<PaymentOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Tìm đơn PENDING quá thời hạn (để scheduled job hủy)
    @Query("SELECT p FROM PaymentOrder p WHERE p.status = 'PENDING' AND p.createdAt < :cutoff")
    List<PaymentOrder> findPendingOrdersBefore(@Param("cutoff") LocalDateTime cutoff);

    // Hủy hàng loạt đơn PENDING quá hạn bằng bulk UPDATE
    @Modifying
    @Query("UPDATE PaymentOrder p SET p.status = 'CANCELLED' WHERE p.status = 'PENDING' AND p.createdAt < :cutoff")
    int cancelPendingOrdersBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.status = 'SUCCESS' AND p.completedAt BETWEEN :start AND :end")
    long countSuccessOrdersByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentOrder p WHERE p.status = 'SUCCESS' AND p.completedAt BETWEEN :start AND :end")
    long sumRevenueByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
