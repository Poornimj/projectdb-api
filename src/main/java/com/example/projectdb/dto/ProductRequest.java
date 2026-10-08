package com.example.projectdb.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 8, fraction = 2,
                message = "Price allows up to 8 integer digits and 2 decimal places")
        BigDecimal price,

        @NotNull(message = "Stock quantity is required")
        @PositiveOrZero(message = "Stock quantity cannot be negative")
        Integer stockQuantity,

        @Positive(message = "Category ID must be positive")
        Integer categoryId,

        @Positive(message = "Supplier ID must be positive")
        Integer supplierId

) {
}