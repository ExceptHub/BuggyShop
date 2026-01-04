package com.buggyshop.controller;

import com.buggyshop.dto.OrderRequest;
import com.buggyshop.entity.Order;
import com.buggyshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("POST /api/orders - Creating order for user: {}", request.getUserId());
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(
            @PathVariable Long id,
            @RequestParam Long userId) {
        log.info("GET /api/orders/{} - userId={}", id, userId);
        Order order = orderService.getOrder(id, userId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        log.info("GET /api/orders/user/{}", userId);
        List<Order> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<Order> processPayment(
            @PathVariable Long id,
            @RequestParam String paymentMethod) {
        log.info("POST /api/orders/{}/payment - method={}", id, paymentMethod);
        Order order = orderService.processPayment(id, paymentMethod);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        log.info("PUT /api/orders/{}/cancel", id);
        Order order = orderService.cancelOrder(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Order> refundOrder(@PathVariable Long id) {
        log.info("POST /api/orders/{}/refund", id);
        Order order = orderService.refundOrder(id);
        return ResponseEntity.ok(order);
    }
}
