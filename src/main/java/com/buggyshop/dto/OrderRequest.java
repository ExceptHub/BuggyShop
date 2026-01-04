package com.buggyshop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Cart ID is required")
    private Long cartId;

    @NotNull(message = "Shipping address ID is required")
    private Long shippingAddressId;

    private String couponCode;
}
