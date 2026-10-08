package com.example.projectdb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Pattern;

public record OrderUpdateRequest(

        @NotBlank(message = "Status is required")
        @Size(max = 50, message = "Status must not exceed 50 characters")
        @Pattern(
                regexp = "NEW|PROCESSING|SHIPPED|CANCELLED",
                message = "Status must be NEW, PROCESSING, SHIPPED or CANCELLED"
        )
        String status,

        LocalDateTime deliveryDate

) {
}