package com.buggyshop.service;

import com.buggyshop.entity.Order;
import com.buggyshop.entity.Product;
import com.buggyshop.entity.Review;
import com.buggyshop.entity.OrderItem;
import com.buggyshop.repository.OrderRepository;
import com.buggyshop.repository.ProductRepository;
import com.buggyshop.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    /**
     * N+1 Query Problem:
     * 1 query to fetch all products
     * + N queries to fetch reviews for each product (lazy loading triggered)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductsWithReviews() {
        // Fetch all products (1 query)
        List<Product> products = productRepository.findAll();

        // For each product, access reviews - triggers N separate queries due to lazy loading
        return products.stream().map(product -> {
            Map<String, Object> productData = new HashMap<>();
            productData.put("id", product.getId());
            productData.put("name", product.getName());
            productData.put("price", product.getPrice());

            // This triggers a separate query for each product's reviews
            List<Review> reviews = product.getReviews();
            productData.put("reviewCount", reviews.size());

            if (!reviews.isEmpty()) {
                double avgRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
                productData.put("averageRating", avgRating);
            } else {
                productData.put("averageRating", 0.0);
            }

            return productData;
        }).collect(Collectors.toList());
    }

    /**
     * Unindexed Column Search:
     * LIKE query on 'description' column which has no index
     * Requires full table scan
     */
    @Transactional(readOnly = true)
    public List<Product> searchProductsByDescription(String keyword) {
        // This uses native query to force the unindexed search
        return productRepository.findAll().stream()
            .filter(p -> p.getDescription() != null &&
                        p.getDescription().toLowerCase().contains(keyword.toLowerCase()))
            .collect(Collectors.toList());
    }

    /**
     * Complex Joins Without Optimization:
     * Fetches order and then triggers multiple lazy-loaded relationships
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderDetailsUnoptimized(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

        Map<String, Object> details = new HashMap<>();
        details.put("orderId", order.getId());
        details.put("total", order.getTotal());
        details.put("status", order.getStatus());

        // Each of these triggers a separate query due to lazy loading
        details.put("userName", order.getUser().getFirstName() + " " + order.getUser().getLastName());
        details.put("userEmail", order.getUser().getEmail());

        // This triggers queries for order items
        List<Map<String, Object>> items = order.getItems().stream().map(item -> {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("quantity", item.getQuantity());
            itemData.put("price", item.getPrice());

            // Another query for each product
            Product product = item.getProduct();
            itemData.put("productName", product.getName());
            itemData.put("productSku", product.getSku());

            // Yet another query for each product's category
            if (product.getCategory() != null) {
                itemData.put("category", product.getCategory().getName());
            }

            return itemData;
        }).collect(Collectors.toList());

        details.put("items", items);

        if (order.getShippingAddress() != null) {
            Map<String, Object> address = new HashMap<>();
            address.put("street", order.getShippingAddress().getStreet());
            address.put("city", order.getShippingAddress().getCity());
            address.put("country", order.getShippingAddress().getCountry());
            details.put("shippingAddress", address);
        }

        return details;
    }

    /**
     * Aggregation on Large Dataset:
     * Scans all reviews to calculate ratings for all products
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductRatingsReport() {
        List<Product> products = productRepository.findAll();

        return products.stream().map(product -> {
            Map<String, Object> report = new HashMap<>();
            report.put("productId", product.getId());
            report.put("productName", product.getName());

            // Triggers separate query for reviews
            List<Review> reviews = product.getReviews();

            if (!reviews.isEmpty()) {
                // Calculate statistics
                double avgRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);

                int maxRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .max()
                    .orElse(0);

                int minRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .min()
                    .orElse(0);

                // Count ratings by value (1-5 stars)
                Map<Integer, Long> ratingDistribution = reviews.stream()
                    .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

                report.put("totalReviews", reviews.size());
                report.put("averageRating", Math.round(avgRating * 100.0) / 100.0);
                report.put("maxRating", maxRating);
                report.put("minRating", minRating);
                report.put("ratingDistribution", ratingDistribution);
            } else {
                report.put("totalReviews", 0);
                report.put("averageRating", 0.0);
            }

            return report;
        }).collect(Collectors.toList());
    }
}
