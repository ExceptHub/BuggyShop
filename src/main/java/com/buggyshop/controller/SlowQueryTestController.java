package com.buggyshop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controller for testing ACTUAL slow queries (single slow query, not N+1).
 * These endpoints execute single queries that are genuinely slow.
 */
@RestController
@RequestMapping("/api/test-slow-query")
@RequiredArgsConstructor
@Slf4j
public class SlowQueryTestController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * SLOW QUERY TEST #1: Uses pg_sleep() to simulate a slow query
     * This is a SINGLE query that takes X seconds to complete.
     * Perfect for testing slow query detection.
     */
    @GetMapping("/sleep")
    public ResponseEntity<Map<String, Object>> testSlowQueryWithSleep(
            @RequestParam(defaultValue = "2") int seconds) {

        log.info("Executing slow query with pg_sleep({})", seconds);

        // This is a SINGLE query that sleeps for X seconds
        String query = "SELECT pg_sleep(" + seconds + "), COUNT(*) as count FROM products";

        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);

        return ResponseEntity.ok(Map.of(
            "message", "Query completed after " + seconds + " seconds",
            "result", result
        ));
    }

    /**
     * SLOW QUERY TEST #2: Cartesian product (CROSS JOIN)
     * This creates a very large result set which is slow to process.
     */
    @GetMapping("/cartesian")
    public ResponseEntity<Map<String, Object>> testSlowQueryCartesian() {

        log.info("Executing slow CROSS JOIN query");

        // CROSS JOIN creates products × products rows (7 × 7 = 49 rows minimum)
        // This is intentionally slow
        String query = """
            SELECT COUNT(*) as total_combinations
            FROM products p1
            CROSS JOIN products p2
            CROSS JOIN products p3
            """;

        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);

        return ResponseEntity.ok(Map.of(
            "message", "Cartesian product query completed",
            "result", result
        ));
    }

    /**
     * SLOW QUERY TEST #3: Complex aggregation with subqueries
     * Multiple nested subqueries that are slow to execute.
     */
    @GetMapping("/complex-aggregation")
    public ResponseEntity<List<Map<String, Object>>> testComplexAggregation() {

        log.info("Executing complex aggregation query");

        String query = """
            SELECT
                p.id,
                p.name,
                (SELECT COUNT(*) FROM reviews r WHERE r.product_id = p.id) as review_count,
                (SELECT AVG(r.rating) FROM reviews r WHERE r.product_id = p.id) as avg_rating,
                (SELECT COUNT(*) FROM order_items oi WHERE oi.product_id = p.id) as times_ordered,
                (SELECT SUM(oi.quantity) FROM order_items oi WHERE oi.product_id = p.id) as total_quantity_sold
            FROM products p
            WHERE p.id IN (
                SELECT DISTINCT oi.product_id
                FROM order_items oi
                JOIN orders o ON oi.order_id = o.id
                WHERE o.status = 'PAID'
            )
            ORDER BY p.id
            """;

        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);

        return ResponseEntity.ok(result);
    }

    /**
     * SLOW QUERY TEST #4: Full table scan with LIKE
     * Scans entire table with LIKE pattern matching (no index).
     */
    @GetMapping("/like-scan")
    public ResponseEntity<List<Map<String, Object>>> testLikeScan(
            @RequestParam(defaultValue = "%") String pattern) {

        log.info("Executing LIKE full table scan with pattern: {}", pattern);

        // Full table scan - description column is not indexed
        String query = """
            SELECT p.*,
                   (SELECT COUNT(*) FROM reviews r WHERE r.product_id = p.id) as review_count
            FROM products p
            WHERE LOWER(p.description) LIKE LOWER(?)
               OR LOWER(p.name) LIKE LOWER(?)
            """;

        List<Map<String, Object>> result = jdbcTemplate.queryForList(query, pattern, pattern);

        return ResponseEntity.ok(result);
    }

    /**
     * SLOW QUERY TEST #5: Configurable sleep + join
     * Combines sleep with complex joins for guaranteed slow query.
     */
    @GetMapping("/sleep-with-join")
    public ResponseEntity<List<Map<String, Object>>> testSlowQueryWithJoin(
            @RequestParam(defaultValue = "1") int seconds) {

        log.info("Executing slow query with sleep({}) and joins", seconds);

        String query = """
            SELECT
                pg_sleep(?) as slept,
                p.id,
                p.name,
                c.name as category_name,
                COUNT(r.id) as review_count
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN reviews r ON r.product_id = p.id
            GROUP BY p.id, p.name, c.name
            ORDER BY p.id
            """;

        List<Map<String, Object>> result = jdbcTemplate.queryForList(query, seconds);

        return ResponseEntity.ok(result);
    }
}
