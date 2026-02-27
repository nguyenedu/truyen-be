package com.example.truyen.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentOrderRequest {

    @NotNull(message = "Package ID is required")
    private Long packageId;
}
