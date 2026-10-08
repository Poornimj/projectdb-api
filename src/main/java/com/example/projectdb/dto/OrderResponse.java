package com.example.projectdb.dto;

import com.example.projectdb.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Integer id,
        Integer customerId,
        LocalDateTime orderDate,
        LocalDateTime deliveryDate,
        Integer shippingAddressId,
        String status,
        List<OrderItem> items,
        BigDecimal total
) {
}