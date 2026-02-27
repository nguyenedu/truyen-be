package com.example.truyen.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CoinPackageRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Coins is required")
    @Min(value = 1, message = "Coins must be at least 1")
    private Integer coins;

    @Min(value = 0, message = "Bonus coins must be >= 0")
    private Integer bonusCoins = 0;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private Boolean isActive = true;
}
