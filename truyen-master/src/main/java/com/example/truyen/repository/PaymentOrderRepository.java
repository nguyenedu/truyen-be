package com.example.truyen.repository;

import com.example.truyen.entity.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderCode(String orderCode);

    boolean existsByVnpTransactionNo(String vnpTransactionNo);

    Page<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
