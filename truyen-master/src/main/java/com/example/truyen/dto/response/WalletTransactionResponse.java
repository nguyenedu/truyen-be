package com.example.truyen.dto.response;

import com.example.truyen.entity.WalletTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransactionResponse {
    private Long id;
    private WalletTransaction.Type type;
    private Integer amount;
    private Integer balanceAfter;
    private String description;
    private LocalDateTime createdAt;
}
