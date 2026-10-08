package com.example.projectdb.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderRequest(

        @NotNull(message = "Customer ID is required")
        @Positive(message = "Customer ID must be positive")
        Integer customerId,

        @Positive(message = "Shipping address ID must be positive")
        Integer shippingAddressId,

        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<@NotNull OrderItemRequest> items

) {
}