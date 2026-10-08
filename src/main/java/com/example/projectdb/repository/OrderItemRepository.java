package com.example.projectdb.repository;

import com.example.projectdb.entity.OrderItem;
import com.example.projectdb.entity.OrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, OrderItemId> {

    List<OrderItem> findByIdOrderId(Integer orderId);
}