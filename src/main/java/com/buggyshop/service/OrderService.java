package com.buggyshop.service;

import com.buggyshop.dto.OrderRequest;
import com.buggyshop.entity.*;
import com.buggyshop.exception.InsufficientInventoryException;
import com.buggyshop.exception.InvalidStateTransitionException;
import com.buggyshop.exception.ResourceNotFoundException;
import com.buggyshop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final InventoryService inventoryService;

    @Transactional
    public Order createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findById(request.getCartId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        Address shippingAddress = addressRepository.findById(request.getShippingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        for (CartItem item : cart.getItems()) {
            try {
                inventoryService.reserveStock(item.getProduct().getId(), item.getQuantity());
            } catch (InsufficientInventoryException e) {
                throw new InsufficientInventoryException(
                        String.format("Product '%s' has insufficient stock", item.getProduct().getName()));
            }
        }

        BigDecimal total = calculateTotal(cart);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal finalTotal = total;

        if (request.getCouponCode() != null && !request.getCouponCode().isEmpty()) {
            Coupon coupon = applyCoupon(request.getCouponCode());
            discount = calculateDiscount(total, coupon);
            finalTotal = total.subtract(discount);
        }

        Order order = Order.builder()
                .user(user)
                .total(total)
                .discount(discount)
                .finalTotal(finalTotal)
                .status(OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .items(new ArrayList<>())
                .build();

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getProduct().getPrice())
                    .subtotal(cartItem.getProduct().getPrice()
                            .multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();
            order.getItems().add(orderItem);
        }

        order = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order created successfully: {}", order.getId());
        return order;
    }

    @Transactional
    public Order processPayment(Long orderId, String paymentMethod) {
        log.info("Processing payment for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot process payment. Order status is: %s", order.getStatus()));
        }

        simulateExternalPaymentGateway();

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentId("PAY-" + UUID.randomUUID().toString());

        for (OrderItem item : order.getItems()) {
            inventoryService.confirmReservation(item.getProduct().getId(), item.getQuantity());
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        log.info("Cancelling order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidStateTransitionException(
                    "Cannot cancel order that has been shipped or delivered");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Order is already cancelled");
        }

        for (OrderItem item : order.getItems()) {
            if (order.getStatus() == OrderStatus.PENDING) {
                inventoryService.releaseReservation(item.getProduct().getId(), item.getQuantity());
            } else if (order.getStatus() == OrderStatus.PAID) {
                inventoryService.restockProduct(item.getProduct().getId(), item.getQuantity());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Transactional
    public Order refundOrder(Long orderId) {
        log.info("Processing refund for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidStateTransitionException(
                    "Can only refund paid or shipped orders");
        }

        if (order.getRefundId() != null) {
            throw new IllegalStateException("Order has already been refunded");
        }

        order.setStatus(OrderStatus.REFUNDED);
        order.setRefundId("REF-" + UUID.randomUUID().toString());

        for (OrderItem item : order.getItems()) {
            inventoryService.restockProduct(item.getProduct().getId(), item.getQuantity());
        }

        return orderRepository.save(order);
    }

    public Order getOrder(Long orderId, Long userId) {
        log.info("Getting order: {} for user: {}", orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied. This order belongs to another user");
        }

        return order;
    }

    public List<Order> getUserOrders(Long userId) {
        log.info("Getting orders for user: {}", userId);
        return orderRepository.findByUserId(userId);
    }

    private BigDecimal calculateTotal(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            BigDecimal itemTotal = item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private Coupon applyCoupon(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Coupon has expired");
        }

        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new IllegalArgumentException("Coupon has reached maximum usage limit");
        }

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        return coupon;
    }

    private BigDecimal calculateDiscount(BigDecimal total, Coupon coupon) {
        if (Boolean.TRUE.equals(coupon.getIsPercentage())) {
            return total.multiply(coupon.getDiscount())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            return coupon.getDiscount();
        }
    }

    private void simulateExternalPaymentGateway() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted");
        }

        if (Math.random() < 0.1) {
            throw new RuntimeException("Payment gateway timeout");
        }
    }
}
