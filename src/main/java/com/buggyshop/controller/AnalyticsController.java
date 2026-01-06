package com.buggyshop.controller;

import com.buggyshop.entity.Order;
import com.buggyshop.entity.Product;
import com.buggyshop.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Analytics endpoints with intentionally slow queries for testing slow query detection.
 * These demonstrate common performance issues:
 * - N+1 query problems
 * - Queries on unindexed columns
 * - Complex joins without optimization
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Slow Query #1: N+1 Problem
     * Fetches all products and then triggers separate queries for each product's reviews.
     * This will generate 1 query for products + N queries for reviews (one per product).
     */
    @GetMapping("/products-with-reviews")
    public ResponseEntity<List<Map<String, Object>>> getProductsWithReviews() {
        log.info("GET /api/analytics/products-with-reviews - Fetching products with reviews (N+1 query)");
        List<Map<String, Object>> result = analyticsService.getProductsWithReviews();
        return ResponseEntity.ok(result);
    }

    /**
     * Slow Query #2: Unindexed Column Search
     * Searches products by description using LIKE, which requires a full table scan
     * since 'description' column is not indexed.
     */
    @GetMapping("/search-by-description")
    public ResponseEntity<List<Product>> searchByDescription(@RequestParam String keyword) {
        log.info("GET /api/analytics/search-by-description?keyword={} - Searching without index", keyword);
        List<Product> products = analyticsService.searchProductsByDescription(keyword);
        return ResponseEntity.ok(products);
    }

    /**
     * Slow Query #3: Complex Joins Without Optimization
     * Fetches order with all related data (user, items, products, addresses) in separate queries
     * instead of using optimized JOIN FETCH.
     */
    @GetMapping("/order-details/{id}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable Long id) {
        log.info("GET /api/analytics/order-details/{} - Fetching with multiple unoptimized queries", id);
        Map<String, Object> orderDetails = analyticsService.getOrderDetailsUnoptimized(id);
        return ResponseEntity.ok(orderDetails);
    }

    /**
     * Slow Query #4: Aggregation on Large Dataset Without Index
     * Calculates average rating for all products by scanning all reviews
     */
    @GetMapping("/product-ratings-report")
    public ResponseEntity<List<Map<String, Object>>> getProductRatingsReport() {
        log.info("GET /api/analytics/product-ratings-report - Aggregating ratings without optimization");
        List<Map<String, Object>> report = analyticsService.getProductRatingsReport();
        return ResponseEntity.ok(report);
    }
}
