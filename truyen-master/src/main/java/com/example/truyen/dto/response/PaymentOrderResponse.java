package com.example.truyen.dto.response;

import com.example.truyen.entity.PaymentOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrderResponse {
    private Long id;
    private String orderCode;
    private String packageName;
    private BigDecimal amount;
    private Integer coinsToAdd;
    private PaymentOrder.Status status;
    private String paymentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
