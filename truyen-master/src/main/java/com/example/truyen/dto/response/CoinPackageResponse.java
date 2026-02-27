package com.example.truyen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinPackageResponse {
    private Long id;
    private String name;
    private Integer coins;
    private Integer bonusCoins;
    private BigDecimal price;
    private Boolean isActive;
}
