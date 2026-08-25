package com.capricorn.order_management_and_inventory_system_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    @NotNull
    private Long orderId;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String paymentMethod; // e.g., "CREDIT_CARD", "PAYPAL"
}
