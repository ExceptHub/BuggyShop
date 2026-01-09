package com.buggyshop.scheduled;

import com.buggyshop.dto.OrderRequest;
import com.buggyshop.entity.Product;
import com.buggyshop.repository.InventoryRepository;
import com.buggyshop.repository.ProductRepository;
import com.buggyshop.service.InventoryService;
import com.buggyshop.service.OrderService;
import com.buggyshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * Scheduled tasks that automatically generate various errors for testing ExceptHub.
 *
 * These jobs run periodically and trigger different types of realistic bugs:
 * - Database errors (not found, deadlocks, concurrent updates)
 * - Business logic errors (insufficient inventory, expired coupons)
 * - Validation errors (negative prices, invalid states)
 * - Concurrency issues (race conditions, optimistic locking)
 *
 * To disable: Set buggyshop.scheduler.enabled=false in application.yml
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "buggyshop.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ErrorGeneratorScheduler {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final Random random = new Random();

    /**
     * Cron 1: Product Not Found Error
     * Runs every 5 minutes
     * Tries to fetch non-existent products
     */
    @Scheduled(fixedDelay = 60000) // 5 minutes
    public void generateProductNotFoundException() {
        try {
            long nonExistentId = 9999L + random.nextInt(1000);
            log.info("🔴 CRON: Attempting to fetch non-existent product ID: {}", nonExistentId);
            productService.getProduct(nonExistentId);
        } catch (Exception e) {
            log.error("✅ Expected error generated: {}", e.getMessage());
        }
    }
//
//    /**
//     * Cron 2: Insufficient Inventory Error
//     * Runs every 7 minutes
//     * Tries to reserve more stock than available
//     */
//    @Scheduled(fixedDelay = 420000) // 7 minutes
//    public void generateInsufficientInventoryError() {
//        try {
//            // Try to reserve 1000 units of the limited edition item (only 5 available)
//            Long limitedProductId = 7L;
//            int impossibleQuantity = 1000;
//            log.info("🔴 CRON: Attempting to reserve {} units of product {} (only 5 available)",
//                    impossibleQuantity, limitedProductId);
//            inventoryService.reserveStock(limitedProductId, impossibleQuantity);
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 3: Expired Coupon Error
//     * Runs every 10 minutes
//     * Tries to create orders with expired coupons
//     */
//    @Scheduled(fixedDelay = 600000) // 10 minutes
//    public void generateExpiredCouponError() {
//        try {
//            OrderRequest request = new OrderRequest();
//            request.setUserId(1L);
//            request.setCartId(1L);
//            request.setShippingAddressId(1L);
//            request.setCouponCode("EXPIRED");
//
//            log.info("🔴 CRON: Attempting to create order with expired coupon: EXPIRED");
//            orderService.createOrder(request);
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 4: Invalid Price Update (Validation Error)
//     * Runs every 12 minutes
//     * Tries to update product with negative price
//     */
//    @Scheduled(fixedDelay = 720000) // 12 minutes
//    public void generateValidationError() {
//        try {
//            Long productId = 1L;
//            BigDecimal invalidPrice = new BigDecimal("-99.99");
//
//            log.info("🔴 CRON: Attempting to update product {} with negative price: {}",
//                    productId, invalidPrice);
//
//            Product product = productRepository.findById(productId).orElseThrow();
//            product.setPrice(invalidPrice);
//            productRepository.save(product);
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 5: Concurrent Update Conflict (Optimistic Locking)
//     * Runs every 15 minutes
//     * Simulates concurrent inventory updates
//     */
//    @Scheduled(fixedDelay = 900000) // 15 minutes
//    @Transactional
//    public void generateOptimisticLockError() {
//        try {
//            Long productId = 1L;
//
//            log.info("🔴 CRON: Simulating concurrent inventory update for product {}", productId);
//
//            // First transaction updates inventory
//            inventoryService.reserveStock(productId, 1);
//
//            // Simulate another concurrent update that will fail
//            // (In real scenario, this would be from another thread/request)
//            inventoryService.restockProduct(productId, 10);
//
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 6: Deleted Product Update Error
//     * Runs every 8 minutes
//     * Tries to update a product that was deleted
//     */
//    @Scheduled(fixedDelay = 480000) // 8 minutes
//    public void generateDeletedProductUpdateError() {
//        try {
//            long nonExistentId = 8888L;
//            log.info("🔴 CRON: Attempting to update non-existent/deleted product ID: {}", nonExistentId);
//
//            Product product = productRepository.findById(nonExistentId).orElseThrow();
//            product.setName("Updated Name");
//            productRepository.save(product);
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 7: Invalid State Transition
//     * Runs every 20 minutes
//     * Tries to cancel already cancelled orders
//     */
//    @Scheduled(fixedDelay = 1200000) // 20 minutes
//    public void generateInvalidStateTransitionError() {
//        try {
//            // First create an order
//            OrderRequest request = new OrderRequest();
//            request.setUserId(1L);
//            request.setCartId(1L);
//            request.setShippingAddressId(1L);
//
//            var order = orderService.createOrder(request);
//
//            log.info("🔴 CRON: Creating order {} and attempting to cancel it twice", order.getId());
//
//            // Cancel it once - should work
//            orderService.cancelOrder(order.getId());
//
//            // Try to cancel again - should fail
//            orderService.cancelOrder(order.getId());
//
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 8: Pagination Out of Bounds
//     * Runs every 6 minutes
//     * Requests page number that doesn't exist
//     */
//    @Scheduled(fixedDelay = 360000) // 6 minutes
//    public void generatePaginationError() {
//        try {
//            int impossiblePage = 9999;
//            log.info("🔴 CRON: Requesting products page {} (out of bounds)", impossiblePage);
//            productService.getProducts(impossiblePage, 10, "name");
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 9: Circular Bundle Dependency
//     * Runs every 25 minutes
//     * Tries to add product to its own bundle
//     */
//    @Scheduled(fixedDelay = 1500000) // 25 minutes
//    public void generateCircularDependencyError() {
//        try {
//            Long productId = 1L;
//            log.info("🔴 CRON: Attempting to add product {} to its own bundle (circular dependency)",
//                    productId);
//            productService.addToBundle(productId, productId);
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 10: Max Uses Exceeded for Coupon
//     * Runs every 18 minutes
//     * Tries to use coupon that has reached max usage limit
//     */
//    @Scheduled(fixedDelay = 1080000) // 18 minutes
//    public void generateCouponMaxUsesError() {
//        try {
//            OrderRequest request = new OrderRequest();
//            request.setUserId(2L);
//            request.setCartId(2L);
//            request.setShippingAddressId(2L);
//            request.setCouponCode("LIMITED"); // This coupon has 5/5 uses
//
//            log.info("🔴 CRON: Attempting to use coupon 'LIMITED' that has reached max uses (5/5)");
//            orderService.createOrder(request);
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 11: N+1 Query Problem Detector
//     * Runs every 30 minutes
//     * Fetches data inefficiently to trigger slow query warnings
//     */
//    @Scheduled(fixedDelay = 1800000) // 30 minutes
//    public void generateNPlusOneQueryProblem() {
//        try {
//            log.info("🔴 CRON: Triggering N+1 query problem by fetching all products with lazy relationships");
//
//            List<Product> products = productRepository.findAll();
//
//            // Access lazy-loaded relationships in a loop (N+1 problem)
//            for (Product product : products) {
//                log.debug("Product: {}, Category: {}",
//                        product.getName(),
//                        product.getCategory().getName());
//            }
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 12: Division By Zero Error
//     * Runs every 22 minutes
//     * Calculates average price with zero products
//     */
//    @Scheduled(fixedDelay = 1320000) // 22 minutes
//    public void generateDivisionByZeroError() {
//        try {
//            log.info("🔴 CRON: Attempting to calculate average price of non-existent category");
//
//            List<Product> products = productRepository.findAll();
//            // Filter by non-existent category
//            List<Product> filtered = products.stream()
//                    .filter(p -> p.getCategory() != null && p.getCategory().getId() == 9999L)
//                    .toList();
//
//            // This will cause division by zero
//            BigDecimal sum = filtered.stream()
//                    .map(Product::getPrice)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            BigDecimal average = sum.divide(new BigDecimal(filtered.size())); // Division by zero!
//            log.info("Average price: {}", average);
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 13: Null Pointer Exception
//     * Runs every 17 minutes
//     * Accesses null relationship without checking
//     */
//    @Scheduled(fixedDelay = 1020000) // 17 minutes
//    public void generateNullPointerError() {
//        try {
//            log.info("🔴 CRON: Attempting to access null product category");
//
//            Product product = new Product();
//            product.setName("Orphan Product");
//            product.setPrice(new BigDecimal("99.99"));
//            product.setSku("NULL-CATEGORY");
//            // No category set - will be null
//
//            // This will throw NPE
//            String categoryName = product.getCategory().getName();
//            log.info("Category: {}", categoryName);
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 14: Index Out Of Bounds Error
//     * Runs every 19 minutes
//     * Tries to access non-existent array index
//     */
//    @Scheduled(fixedDelay = 1140000) // 19 minutes
//    public void generateIndexOutOfBoundsError() {
//        try {
//            log.info("🔴 CRON: Attempting to access invalid product index");
//
//            List<Product> products = productRepository.findAll();
//            // Try to access index that doesn't exist
//            Product product = products.get(products.size() + 10);
//            log.info("Product: {}", product.getName());
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 15: SQL Constraint Violation - Duplicate SKU
//     * Runs every 28 minutes
//     * Tries to create product with duplicate SKU
//     */
//    @Scheduled(fixedDelay = 1680000) // 28 minutes
//    public void generateDuplicateSKUError() {
//        try {
//            log.info("🔴 CRON: Attempting to create product with duplicate SKU");
//
//            Product product1 = Product.builder()
//                    .name("Product A")
//                    .price(new BigDecimal("50.00"))
//                    .sku("DUPLICATE-SKU-001")
//                    .build();
//            productRepository.save(product1);
//
//            // Try to save another product with same SKU
//            Product product2 = Product.builder()
//                    .name("Product B")
//                    .price(new BigDecimal("75.00"))
//                    .sku("DUPLICATE-SKU-001") // Duplicate!
//                    .build();
//            productRepository.save(product2);
//
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 16: Illegal State - Update Delivered Order
//     * Runs every 23 minutes
//     * Tries to modify order that is already delivered
//     */
//    @Scheduled(fixedDelay = 1380000) // 23 minutes
//    public void generateIllegalStateDeliveredOrderError() {
//        try {
//            log.info("🔴 CRON: Attempting to modify already delivered order");
//
//            // In real scenario, this would find a delivered order
//            // Here we simulate the error
//            throw new IllegalStateException("Cannot modify order in DELIVERED state");
//
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 17: Number Format Exception
//     * Runs every 21 minutes
//     * Tries to parse invalid number from user input
//     */
//    @Scheduled(fixedDelay = 1260000) // 21 minutes
//    public void generateNumberFormatError() {
//        try {
//            log.info("🔴 CRON: Attempting to parse invalid price string");
//
//            String invalidPrice = "99.99€"; // Contains currency symbol
//            BigDecimal price = new BigDecimal(invalidPrice);
//            log.info("Parsed price: {}", price);
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 18: Class Cast Exception
//     * Runs every 26 minutes
//     * Tries to cast object to wrong type
//     */
//    @Scheduled(fixedDelay = 1560000) // 26 minutes
//    public void generateClassCastError() {
//        try {
//            log.info("🔴 CRON: Attempting invalid type casting");
//
//            Object obj = "This is a string";
//            // Try to cast String to Integer
//            Integer number = (Integer) obj;
//            log.info("Number: {}", number);
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 19: Arithmetic Exception - Invalid Scale
//     * Runs every 24 minutes
//     * Tries to divide with invalid rounding mode
//     */
//    @Scheduled(fixedDelay = 1440000) // 24 minutes
//    public void generateArithmeticError() {
//        try {
//            log.info("🔴 CRON: Attempting division with non-terminating decimal");
//
//            BigDecimal price = new BigDecimal("10");
//            BigDecimal quantity = new BigDecimal("3");
//
//            // This will throw ArithmeticException - non-terminating decimal expansion
//            BigDecimal unitPrice = price.divide(quantity);
//            log.info("Unit price: {}", unitPrice);
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Cron 20: Illegal Argument - Negative Quantity
//     * Runs every 16 minutes
//     * Tries to create order item with negative quantity
//     */
//    @Scheduled(fixedDelay = 960000) // 16 minutes
//    public void generateNegativeQuantityError() {
//        try {
//            log.info("🔴 CRON: Attempting to create order with negative quantity");
//
//            int quantity = -5;
//            if (quantity < 0) {
//                throw new IllegalArgumentException("Order quantity cannot be negative: " + quantity);
//            }
//        } catch (Exception e) {
//            log.error("✅ Expected error generated: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Summary cron that logs current error generation status
//     * Runs every hour
//     */
//    @Scheduled(fixedDelay = 3600000) // 1 hour
//    public void logErrorGenerationStatus() {
//        log.info("📊 ERROR GENERATOR STATUS:");
//        log.info("  ✅ 20 error-generating crons are active");
//        log.info("  📍 Errors are being sent to ExceptHub for AI analysis");
//        log.info("  🎯 Testing realistic e-commerce bugs");
//        log.info("  ⏰ Next errors will be generated within 5-30 minutes");
//    }
}
