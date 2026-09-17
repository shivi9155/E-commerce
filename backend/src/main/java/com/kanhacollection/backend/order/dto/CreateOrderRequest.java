package com.kanhacollection.backend.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Shipping address is required")
    @Valid
    private OrderAddressRequest shippingAddress;

    private String couponCode;

    // Optional address ID if selecting from saved user addresses
    private UUID savedAddressId;
}
