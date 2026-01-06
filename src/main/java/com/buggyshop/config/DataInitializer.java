package com.buggyshop.config;

import com.buggyshop.entity.*;
import com.buggyshop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CartRepository cartRepository;
    private final CouponRepository couponRepository;
    private final AddressRepository addressRepository;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing test data...");

        createUsers();
        createCategories();
        createProducts();
        createCoupons();
        createReviewsAndOrders();

        log.info("Test data initialization complete!");
    }

    private void createUsers() {
        User user1 = User.builder()
                .email("john@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .addresses(new ArrayList<>())
                .orders(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        User user2 = User.builder()
                .email("jane@example.com")
                .password("password123")
                .firstName("Jane")
                .lastName("Smith")
                .addresses(new ArrayList<>())
                .orders(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        Address address1 = Address.builder()
                .user(user1)
                .street("123 Main St")
                .city("New York")
                .state("NY")
                .zipCode("10001")
                .country("USA")
                .isDefault(true)
                .build();

        Address address2 = Address.builder()
                .user(user2)
                .street("456 Oak Ave")
                .city("Los Angeles")
                .state("CA")
                .zipCode("90001")
                .country("USA")
                .isDefault(true)
                .build();

        addressRepository.save(address1);
        addressRepository.save(address2);

        Cart cart1 = Cart.builder()
                .user(user1)
                .items(new ArrayList<>())
                .build();

        Cart cart2 = Cart.builder()
                .user(user2)
                .items(new ArrayList<>())
                .build();

        cartRepository.save(cart1);
        cartRepository.save(cart2);

        log.info("Created {} users", 2);
    }

    private void createCategories() {
        Category electronics = Category.builder()
                .name("Electronics")
                .description("Electronic devices and accessories")
                .products(new ArrayList<>())
                .children(new ArrayList<>())
                .build();

        Category clothing = Category.builder()
                .name("Clothing")
                .description("Apparel and fashion")
                .products(new ArrayList<>())
                .children(new ArrayList<>())
                .build();

        Category books = Category.builder()
                .name("Books")
                .description("Books and literature")
                .products(new ArrayList<>())
                .children(new ArrayList<>())
                .build();

        categoryRepository.saveAll(List.of(electronics, clothing, books));

        log.info("Created {} categories", 3);
    }

    private void createProducts() {
        Category electronics = categoryRepository.findById(1L).orElseThrow();
        Category clothing = categoryRepository.findById(2L).orElseThrow();
        Category books = categoryRepository.findById(3L).orElseThrow();

        Product laptop = Product.builder()
                .name("Laptop Pro 15\"")
                .description("High-performance laptop for professionals")
                .price(new BigDecimal("1299.99"))
                .sku("LAPTOP-001")
                .category(electronics)
                .bundledProducts(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        Product mouse = Product.builder()
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .price(new BigDecimal("29.99"))
                .sku("MOUSE-001")
                .category(electronics)
                .bundledProducts(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        Product tshirt = Product.builder()
                .name("Cotton T-Shirt")
                .description("Comfortable cotton t-shirt")
                .price(new BigDecimal("19.99"))
                .sku("TSHIRT-001")
                .category(clothing)
                .bundledProducts(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        Product jeans = Product.builder()
                .name("Denim Jeans")
                .description("Classic blue jeans")
                .price(new BigDecimal("49.99"))
                .sku("JEANS-001")
                .category(clothing)
                .bundledProducts(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        Product book1 = Product.builder()
                .name("Spring Boot in Action")
                .description("Comprehensive guide to Spring Boot")
                .price(new BigDecimal("39.99"))
                .sku("BOOK-001")
                .category(books)
                .bundledProducts(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        Product book2 = Product.builder()
                .name("Java Performance")
                .description("The definitive guide to Java performance")
                .price(new BigDecimal("44.99"))
                .sku("BOOK-002")
                .category(books)
                .bundledProducts(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        Product lowStock = Product.builder()
                .name("Limited Edition Item")
                .description("Only 5 left in stock!")
                .price(new BigDecimal("99.99"))
                .sku("LIMITED-001")
                .category(electronics)
                .bundledProducts(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        List<Product> savedProducts = productRepository.saveAll(List.of(laptop, mouse, tshirt, jeans, book1, book2, lowStock));

        Inventory inv1 = Inventory.builder()
                .product(savedProducts.get(0))
                .quantity(50)
                .reserved(0)
                .build();

        Inventory inv2 = Inventory.builder()
                .product(savedProducts.get(1))
                .quantity(100)
                .reserved(0)
                .build();

        Inventory inv3 = Inventory.builder()
                .product(savedProducts.get(2))
                .quantity(200)
                .reserved(0)
                .build();

        Inventory inv4 = Inventory.builder()
                .product(savedProducts.get(3))
                .quantity(75)
                .reserved(0)
                .build();

        Inventory inv5 = Inventory.builder()
                .product(savedProducts.get(4))
                .quantity(30)
                .reserved(0)
                .build();

        Inventory inv6 = Inventory.builder()
                .product(savedProducts.get(5))
                .quantity(25)
                .reserved(0)
                .build();

        Inventory inv7 = Inventory.builder()
                .product(savedProducts.get(6))
                .quantity(5)
                .reserved(0)
                .build();

        inventoryRepository.saveAll(List.of(inv1, inv2, inv3, inv4, inv5, inv6, inv7));

        log.info("Created {} products with inventory", 7);
    }

    private void createCoupons() {
        Coupon validCoupon = Coupon.builder()
                .code("SAVE10")
                .discount(new BigDecimal("10.00"))
                .isPercentage(true)
                .maxUses(100)
                .usedCount(0)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        Coupon expiredCoupon = Coupon.builder()
                .code("EXPIRED")
                .discount(new BigDecimal("20.00"))
                .isPercentage(true)
                .maxUses(100)
                .usedCount(0)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        Coupon limitedCoupon = Coupon.builder()
                .code("LIMITED")
                .discount(new BigDecimal("15.00"))
                .isPercentage(true)
                .maxUses(5)
                .usedCount(5)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        couponRepository.saveAll(List.of(validCoupon, expiredCoupon, limitedCoupon));

        log.info("Created {} coupons", 3);
    }

    private void createReviewsAndOrders() {
        List<User> users = userRepository.findAll();
        List<Product> products = productRepository.findAll();

        if (users.isEmpty() || products.isEmpty()) {
            log.warn("Cannot create reviews and orders - no users or products found");
            return;
        }

        // Create reviews for each product to trigger N+1 queries
        int reviewCount = 0;
        for (Product product : products) {
            // Create 3-8 reviews per product
            int numReviews = 3 + (int) (Math.random() * 6);
            for (int i = 0; i < numReviews; i++) {
                User reviewer = users.get((int) (Math.random() * users.size()));
                int rating = 1 + (int) (Math.random() * 5); // 1-5 stars

                Review review = Review.builder()
                        .product(product)
                        .user(reviewer)
                        .rating(rating)
                        .comment(generateReviewComment(rating))
                        .build();

                reviewRepository.save(review);
                reviewCount++;
            }
        }

        // Create several orders to test order-related slow queries
        int orderCount = 0;
        for (User user : users) {
            // Create 2-5 orders per user
            int numOrders = 2 + (int) (Math.random() * 4);
            for (int i = 0; i < numOrders; i++) {
                Order order = Order.builder()
                        .user(user)
                        .status(OrderStatus.values()[(int) (Math.random() * OrderStatus.values().length)])
                        .total(BigDecimal.ZERO)
                        .finalTotal(BigDecimal.ZERO)
                        .items(new ArrayList<>())
                        .build();

                if (!user.getAddresses().isEmpty()) {
                    order.setShippingAddress(user.getAddresses().get(0));
                }

                // Add 1-4 items to each order
                int numItems = 1 + (int) (Math.random() * 4);
                BigDecimal orderTotal = BigDecimal.ZERO;

                for (int j = 0; j < numItems; j++) {
                    Product product = products.get((int) (Math.random() * products.size()));
                    int quantity = 1 + (int) (Math.random() * 3);

                    OrderItem item = OrderItem.builder()
                            .order(order)
                            .product(product)
                            .quantity(quantity)
                            .price(product.getPrice())
                            .build();

                    order.getItems().add(item);
                    orderTotal = orderTotal.add(product.getPrice().multiply(new BigDecimal(quantity)));
                }

                order.setTotal(orderTotal);
                order.setFinalTotal(orderTotal);

                orderRepository.save(order);
                orderCount++;
            }
        }

        log.info("Created {} reviews and {} orders for testing slow queries", reviewCount, orderCount);
    }

    private String generateReviewComment(int rating) {
        String[] greatComments = {
                "Excellent product! Highly recommended.",
                "Best purchase I've made this year!",
                "Amazing quality, exceeded my expectations.",
                "Perfect! Will definitely buy again.",
                "Outstanding product, fast shipping too!"
        };

        String[] goodComments = {
                "Good product, works as described.",
                "Satisfied with this purchase.",
                "Nice quality for the price.",
                "Would recommend to others."
        };

        String[] averageComments = {
                "It's okay, nothing special.",
                "Meets basic expectations.",
                "Average product, could be better."
        };

        String[] poorComments = {
                "Not as good as I expected.",
                "Disappointed with the quality.",
                "Would not buy again.",
                "Below average, not worth the price.",
                "Terrible product, waste of money."
        };

        if (rating >= 5) {
            return greatComments[(int) (Math.random() * greatComments.length)];
        } else if (rating >= 4) {
            return goodComments[(int) (Math.random() * goodComments.length)];
        } else if (rating >= 3) {
            return averageComments[(int) (Math.random() * averageComments.length)];
        } else {
            return poorComments[(int) (Math.random() * poorComments.length)];
        }
    }
}
