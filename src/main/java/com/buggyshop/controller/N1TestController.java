package com.buggyshop.controller;

import com.buggyshop.entity.Order;
import com.buggyshop.entity.User;
import com.buggyshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for testing N+1 query problem.
 * This demonstrates the classic N+1 problem where:
 * - 1 query fetches all users
 * - N queries fetch orders for each user (one query per user)
 */
@RestController
@RequestMapping("/api/test-n1")
@RequiredArgsConstructor
@Slf4j
public class N1TestController {

    private final UserRepository userRepository;

    /**
     * N+1 PROBLEM TEST #1: Classic N+1 with User -> Orders
     *
     * This endpoint will execute:
     * 1. SELECT * FROM users (1 query)
     * 2. SELECT * FROM orders WHERE user_id = ? (N queries - one per user)
     *
     * If you have 10 users, this will execute 11 queries total (1 + 10).
     * ExceptHub should detect this as N+1 problem.
     */
    @GetMapping("/user-orders")
    public ResponseEntity<Map<String, Object>> testN1UserOrders() {
        log.info("Testing N+1 problem: User -> Orders");

        // This fetches all users (1 query)
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> userOrderCounts = new ArrayList<>();

        // This loop triggers N queries (one per user)
        for (User user : users) {
            // user.getOrders().size() triggers lazy loading
            // Each iteration = 1 SELECT query to orders table
            int orderCount = user.getOrders().size();

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("email", user.getEmail());
            userInfo.put("firstName", user.getFirstName());
            userInfo.put("orderCount", orderCount);

            userOrderCounts.add(userInfo);
        }

        return ResponseEntity.ok(Map.of(
            "message", "N+1 query executed",
            "usersProcessed", users.size(),
            "expectedQueries", users.size() + 1, // 1 for users + N for orders
            "data", userOrderCounts
        ));
    }

    /**
     * N+1 PROBLEM TEST #2: More obvious N+1 with Order details
     *
     * This fetches all users and for each user:
     * - Gets their orders (N queries)
     * - Accesses order properties (may trigger more queries if not loaded)
     */
    @GetMapping("/user-order-details")
    public ResponseEntity<Map<String, Object>> testN1WithOrderDetails() {
        log.info("Testing N+1 problem with order details");

        List<User> users = userRepository.findAll(); // 1 query

        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("email", user.getEmail());

            // This triggers lazy loading for orders (N queries)
            List<Order> orders = user.getOrders();

            List<Map<String, Object>> orderDetails = new ArrayList<>();
            for (Order order : orders) {
                orderDetails.add(Map.of(
                    "orderId", order.getId(),
                    "status", order.getStatus().toString(),
                    "totalAmount", order.getFinalTotal()
                ));
            }

            userInfo.put("orders", orderDetails);
            result.add(userInfo);
        }

        return ResponseEntity.ok(Map.of(
            "message", "N+1 with full order details",
            "usersProcessed", users.size(),
            "totalOrders", result.stream()
                .mapToInt(u -> ((List<?>) u.get("orders")).size())
                .sum(),
            "data", result
        ));
    }

    /**
     * N+1 PROBLEM TEST #3: Multiple N+1s in one request
     *
     * This demonstrates multiple N+1 problems:
     * - User -> Orders (N queries)
     * - User -> Reviews (N queries)
     *
     * Total queries: 1 (users) + N (orders) + N (reviews) = 1 + 2N
     */
    @GetMapping("/multiple-n1")
    public ResponseEntity<Map<String, Object>> testMultipleN1() {
        log.info("Testing MULTIPLE N+1 problems in single request");

        List<User> users = userRepository.findAll(); // 1 query

        List<Map<String, Object>> result = new ArrayList<>();
        int totalQueries = 1; // users query

        for (User user : users) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("email", user.getEmail());

            // N+1 #1: Orders
            int orderCount = user.getOrders().size(); // 1 query per user
            totalQueries++;

            // N+1 #2: Reviews
            int reviewCount = user.getReviews().size(); // 1 query per user
            totalQueries++;

            userInfo.put("orderCount", orderCount);
            userInfo.put("reviewCount", reviewCount);

            result.add(userInfo);
        }

        return ResponseEntity.ok(Map.of(
            "message", "MULTIPLE N+1 problems executed",
            "usersProcessed", users.size(),
            "totalQueriesExecuted", totalQueries,
            "breakdown", Map.of(
                "usersQuery", 1,
                "ordersQueries", users.size(),
                "reviewsQueries", users.size()
            ),
            "data", result
        ));
    }

    /**
     * CONTROL TEST: No N+1 problem (for comparison)
     *
     * This uses JOIN FETCH to load orders in a single query.
     * This should NOT trigger N+1 detection.
     */
    @GetMapping("/no-n1-join-fetch")
    public ResponseEntity<Map<String, Object>> testNoN1() {
        log.info("Testing WITHOUT N+1 (using JOIN FETCH)");

        // This would need a custom repository method with @Query and JOIN FETCH
        // For now, this is just a placeholder to show the difference

        return ResponseEntity.ok(Map.of(
            "message", "This endpoint would use JOIN FETCH to avoid N+1",
            "note", "Not implemented - requires custom @Query with JOIN FETCH"
        ));
    }
}
