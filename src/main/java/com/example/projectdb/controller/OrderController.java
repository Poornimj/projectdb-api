package com.example.projectdb.controller;

import com.example.projectdb.dto.OrderItemRequest;
import com.example.projectdb.dto.OrderRequest;
import com.example.projectdb.dto.OrderResponse;
import com.example.projectdb.entity.Order;
import com.example.projectdb.entity.OrderItem;
import com.example.projectdb.entity.OrderItemId;
import com.example.projectdb.entity.Product;
import com.example.projectdb.repository.OrderItemRepository;
import com.example.projectdb.repository.OrderRepository;
import com.example.projectdb.repository.ProductRepository;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.projectdb.dto.OrderUpdateRequest;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    public OrderController(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            JdbcTemplate jdbcTemplate) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request) {

        Integer customerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE id = ?",
                Integer.class,
                request.customerId());

        if (customerCount == null || customerCount == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Customer does not exist");
        }

        if (request.shippingAddressId() != null) {
            Integer addressCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM customeraddresses
                    WHERE id = ? AND customer_id = ?
                    """,
                    Integer.class,
                    request.shippingAddressId(),
                    request.customerId());

            if (addressCount == null || addressCount == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Shipping address does not belong to this customer");
            }
        }

        Set<Integer> productIds = new HashSet<>();

        for (OrderItemRequest item : request.items()) {
            if (!productIds.add(item.productId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each product must appear only once in an order");
            }
        }

        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setShippingAddressId(request.shippingAddressId());

        Order savedOrder = orderRepository.saveAndFlush(order);

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository
                    .findById(itemRequest.productId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Product does not exist: " + itemRequest.productId()));

            OrderItem item = new OrderItem(
                    new OrderItemId(savedOrder.getId(), product.getId()),
                    itemRequest.quantity(),
                    product.getPrice());

            orderItemRepository.save(item);
        }

        orderItemRepository.flush();

        return ResponseEntity
                .created(URI.create("/api/orders/" + savedOrder.getId()))
                .body(toResponse(savedOrder));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Integer id) {

        return orderRepository.findById(id)
                .map(order -> ResponseEntity.ok(toResponse(order)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable Integer id,
            @Valid @RequestBody OrderUpdateRequest request) {

        Order order = orderRepository.findById(id).orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.deliveryDate() != null
                && request.deliveryDate().isBefore(order.getOrderDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Delivery date cannot be before the order date");
        }

        order.setStatus(request.status());
        order.setDeliveryDate(request.deliveryDate());

        Order updatedOrder = orderRepository.saveAndFlush(order);

        return ResponseEntity.ok(toResponse(updatedOrder));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Integer id) {

        Order order = orderRepository.findById(id).orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        List<OrderItem> items =
                orderItemRepository.findByIdOrderId(id);

        orderItemRepository.deleteAll(items);
        orderItemRepository.flush();

        orderRepository.delete(order);
        orderRepository.flush();

        return ResponseEntity.noContent().build();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItem> items =
                orderItemRepository.findByIdOrderId(order.getId());

        BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(
                        BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getOrderDate(),
                order.getDeliveryDate(),
                order.getShippingAddressId(),
                order.getStatus(),
                items,
                total);
    }
}