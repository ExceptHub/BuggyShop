# How to Fix Slow Queries in BuggyShop

This document explains each slow query pattern detected by ExceptHub and shows how to fix them.

---

## 📋 Table of Contents

1. [N+1 Query Problem](#1-n1-query-problem)
2. [Unindexed Column Search](#2-unindexed-column-search)
3. [Unoptimized Complex Joins](#3-unoptimized-complex-joins)
4. [Aggregation Without Index](#4-aggregation-without-index)

---

## 1. N+1 Query Problem

### 🐛 The Problem

**Endpoint:** `GET /api/analytics/products-with-reviews`

**Current Implementation:**
```java
@Transactional(readOnly = true)
public List<Map<String, Object>> getProductsWithReviews() {
    List<Product> products = productRepository.findAll();

    List<Map<String, Object>> result = new ArrayList<>();
    for (Product product : products) {
        Map<String, Object> productData = new HashMap<>();
        productData.put("id", product.getId());
        productData.put("name", product.getName());

        // THIS TRIGGERS A SEPARATE QUERY FOR EACH PRODUCT!
        List<Review> reviews = product.getReviews();
        productData.put("reviewCount", reviews.size());

        result.add(productData);
    }
    return result;
}
```

**Why It's Slow:**
- Fetches all products: `SELECT * FROM products` (1 query)
- For each product, lazily loads reviews: `SELECT * FROM reviews WHERE product_id = ?` (N queries)
- **Total: 1 + N queries** where N = number of products
- With 100 products → 101 queries!

**Performance Impact:**
- 🔴 Current: ~500-1000ms (101 queries)
- 🟢 After fix: ~50-100ms (1 query)

---

### ✅ Solution 1: Use JOIN FETCH

**Fixed Implementation:**
```java
// Repository method
@Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.reviews")
List<Product> findAllWithReviews();

// Service method
@Transactional(readOnly = true)
public List<Map<String, Object>> getProductsWithReviews() {
    List<Product> products = productRepository.findAllWithReviews();

    List<Map<String, Object>> result = new ArrayList<>();
    for (Product product : products) {
        Map<String, Object> productData = new HashMap<>();
        productData.put("id", product.getId());
        productData.put("name", product.getName());
        productData.put("reviewCount", product.getReviews().size());
        result.add(productData);
    }
    return result;
}
```

**What Changed:**
- Single query with JOIN: `SELECT * FROM products LEFT JOIN reviews ON ...`
- All data fetched at once
- No additional queries when accessing `product.getReviews()`

---

### ✅ Solution 2: Use EntityGraph (Alternative)

```java
@EntityGraph(attributePaths = {"reviews"})
@Query("SELECT p FROM Product p")
List<Product> findAllWithReviews();
```

**Benefits:**
- More declarative
- Can be reused with Spring Data methods
- Easier to maintain

---

### ✅ Solution 3: Use DTO Projection (Most Efficient)

```java
// Custom DTO
public record ProductWithReviewCount(Long id, String name, Long reviewCount) {}

// Repository method
@Query("""
    SELECT new com.buggyshop.dto.ProductWithReviewCount(
        p.id,
        p.name,
        COUNT(r.id)
    )
    FROM Product p
    LEFT JOIN p.reviews r
    GROUP BY p.id, p.name
    """)
List<ProductWithReviewCount> findAllWithReviewCounts();
```

**Benefits:**
- Only fetches needed columns
- Aggregation done in database
- No entity hydration overhead
- **Fastest solution** 🚀

---

## 2. Unindexed Column Search

### 🐛 The Problem

**Endpoint:** `GET /api/analytics/search-by-description?keyword=premium`

**Current Implementation:**
```java
@Query("SELECT p FROM Product p WHERE p.description LIKE %:keyword%")
List<Product> searchProductsByDescription(@Param("keyword") String keyword);
```

**Why It's Slow:**
- `description` column is NOT indexed
- `LIKE %keyword%` requires **full table scan**
- Database must read every row to check if description contains keyword
- Cannot use index because of leading wildcard `%`

**Performance Impact:**
- 🔴 Current: ~300-500ms (full table scan)
- 🟢 After fix: ~20-50ms (indexed search)

---

### ✅ Solution 1: Add Database Index

**Add Migration:**
```sql
-- Create GIN index for full-text search (PostgreSQL)
CREATE INDEX idx_products_description_gin
ON products
USING GIN (to_tsvector('english', description));

-- Or create trigram index for LIKE queries
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_products_description_trgm
ON products
USING GIN (description gin_trgm_ops);
```

**Update Query for Full-Text Search:**
```java
@Query(value = """
    SELECT * FROM products
    WHERE to_tsvector('english', description) @@ plainto_tsquery('english', :keyword)
    """, nativeQuery = true)
List<Product> searchProductsByDescription(@Param("keyword") String keyword);
```

**Benefits:**
- Uses PostgreSQL full-text search
- Extremely fast on large datasets
- Handles stemming (e.g., "running" matches "run")
- Relevance ranking support

---

### ✅ Solution 2: Use Trigram Index (for LIKE queries)

**Keep existing LIKE query but add trigram index:**
```java
@Query("SELECT p FROM Product p WHERE p.description LIKE %:keyword%")
List<Product> searchProductsByDescription(@Param("keyword") String keyword);
```

**Benefits:**
- Still uses LIKE syntax
- Works with leading wildcards `%keyword%`
- No query changes needed

---

### ✅ Solution 3: Elasticsearch Integration (Best for Large Scale)

**Add Elasticsearch:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

**Entity Configuration:**
```java
@Document(indexName = "products")
public class ProductSearchDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "english")
    private String description;

    @Field(type = FieldType.Text)
    private String name;
}
```

**Repository:**
```java
public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, Long> {
    List<ProductSearchDocument> findByDescriptionContaining(String keyword);
}
```

**Benefits:**
- Sub-millisecond search
- Advanced features (fuzzy search, synonyms, etc.)
- Scales to millions of documents
- Best for production applications

---

## 3. Unoptimized Complex Joins

### 🐛 The Problem

**Endpoint:** `GET /api/analytics/order-details/{id}`

**Current Implementation:**
```java
@Transactional(readOnly = true)
public Map<String, Object> getOrderDetailsUnoptimized(Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found"));

    Map<String, Object> details = new HashMap<>();
    details.put("orderId", order.getId());
    details.put("total", order.getTotal());

    // LAZY LOAD: Triggers query for user
    details.put("userName", order.getUser().getFirstName());

    // LAZY LOAD: Triggers query for each order item
    List<Map<String, Object>> items = new ArrayList<>();
    for (OrderItem item : order.getItems()) {
        Map<String, Object> itemData = new HashMap<>();

        // LAZY LOAD: Triggers query for product
        itemData.put("productName", item.getProduct().getName());
        itemData.put("quantity", item.getQuantity());
        items.add(itemData);
    }
    details.put("items", items);

    // LAZY LOAD: Triggers query for address
    if (order.getShippingAddress() != null) {
        details.put("shippingAddress",
            order.getShippingAddress().getStreet());
    }

    return details;
}
```

**Why It's Slow:**
- Initial query: `SELECT * FROM orders WHERE id = ?` (1 query)
- Load user: `SELECT * FROM users WHERE id = ?` (1 query)
- Load items: `SELECT * FROM order_items WHERE order_id = ?` (1 query)
- For each item, load product: `SELECT * FROM products WHERE id = ?` (N queries)
- Load address: `SELECT * FROM addresses WHERE id = ?` (1 query)
- **Total: 4 + N queries** where N = number of items

**Performance Impact:**
- 🔴 Current: ~400-800ms (10+ queries)
- 🟢 After fix: ~50-100ms (1 query)

---

### ✅ Solution: Use JOIN FETCH for All Relationships

**Repository Method:**
```java
@Query("""
    SELECT DISTINCT o FROM Order o
    LEFT JOIN FETCH o.user u
    LEFT JOIN FETCH o.items i
    LEFT JOIN FETCH i.product p
    LEFT JOIN FETCH o.shippingAddress a
    WHERE o.id = :orderId
    """)
Optional<Order> findByIdWithAllRelations(@Param("orderId") Long orderId);
```

**Service Method:**
```java
@Transactional(readOnly = true)
public Map<String, Object> getOrderDetailsOptimized(Long orderId) {
    Order order = orderRepository.findByIdWithAllRelations(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found"));

    Map<String, Object> details = new HashMap<>();
    details.put("orderId", order.getId());
    details.put("total", order.getTotal());
    details.put("userName", order.getUser().getFirstName()); // No query!

    List<Map<String, Object>> items = new ArrayList<>();
    for (OrderItem item : order.getItems()) { // No query!
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("productName", item.getProduct().getName()); // No query!
        itemData.put("quantity", item.getQuantity());
        items.add(itemData);
    }
    details.put("items", items);

    if (order.getShippingAddress() != null) { // No query!
        details.put("shippingAddress", order.getShippingAddress().getStreet());
    }

    return details;
}
```

**What Changed:**
- Single complex JOIN query fetches everything
- All relationships loaded eagerly in one trip
- No lazy loading surprises
- Predictable performance

---

### ✅ Alternative: Use EntityGraph

```java
@EntityGraph(attributePaths = {
    "user",
    "items",
    "items.product",
    "shippingAddress"
})
Optional<Order> findById(Long id);
```

**Benefits:**
- More concise
- Can be applied to standard Spring Data methods
- Easier to maintain

---

## 4. Aggregation Without Index

### 🐛 The Problem

**Endpoint:** `GET /api/analytics/product-ratings-report`

**Current Implementation:**
```java
@Transactional(readOnly = true)
public List<Map<String, Object>> getProductRatingsReport() {
    List<Product> products = productRepository.findAll();

    List<Map<String, Object>> report = new ArrayList<>();
    for (Product product : products) {
        Map<String, Object> productRating = new HashMap<>();
        productRating.put("id", product.getId());
        productRating.put("name", product.getName());

        // LAZY LOAD + IN-MEMORY AGGREGATION
        List<Review> reviews = product.getReviews();
        double avgRating = reviews.stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);

        productRating.put("averageRating", avgRating);
        productRating.put("reviewCount", reviews.size());

        report.add(productRating);
    }
    return report;
}
```

**Why It's Slow:**
- Fetches ALL products and ALL reviews into memory
- N+1 query problem (same as #1)
- Calculates average in Java instead of database
- No indexes on review aggregation columns

**Performance Impact:**
- 🔴 Current: ~600-1000ms
- 🟢 After fix: ~50-100ms

---

### ✅ Solution 1: Database Aggregation with Index

**Add Index:**
```sql
-- Create composite index for aggregation
CREATE INDEX idx_reviews_product_rating
ON reviews(product_id, rating);
```

**Repository Method:**
```java
@Query("""
    SELECT new com.buggyshop.dto.ProductRatingReport(
        p.id,
        p.name,
        COALESCE(AVG(r.rating), 0.0),
        COUNT(r.id)
    )
    FROM Product p
    LEFT JOIN p.reviews r
    GROUP BY p.id, p.name
    ORDER BY AVG(r.rating) DESC
    """)
List<ProductRatingReport> getProductRatingsReport();
```

**DTO:**
```java
public record ProductRatingReport(
    Long id,
    String name,
    Double averageRating,
    Long reviewCount
) {}
```

**Benefits:**
- Single query with aggregation in database
- Index speeds up GROUP BY and AVG
- Database engines optimize aggregations
- Minimal memory usage

---

### ✅ Solution 2: Materialized View (Best for Large Scale)

**Create Materialized View:**
```sql
CREATE MATERIALIZED VIEW product_ratings_summary AS
SELECT
    p.id AS product_id,
    p.name AS product_name,
    COALESCE(AVG(r.rating), 0) AS average_rating,
    COUNT(r.id) AS review_count
FROM products p
LEFT JOIN reviews r ON r.product_id = p.id
GROUP BY p.id, p.name;

-- Create index on materialized view
CREATE INDEX idx_product_ratings_summary_rating
ON product_ratings_summary(average_rating DESC);

-- Refresh materialized view periodically
CREATE OR REPLACE FUNCTION refresh_product_ratings()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY product_ratings_summary;
END;
$$ LANGUAGE plpgsql;
```

**Query Materialized View:**
```java
@Query(value = """
    SELECT
        product_id as id,
        product_name as name,
        average_rating as averageRating,
        review_count as reviewCount
    FROM product_ratings_summary
    ORDER BY average_rating DESC
    """, nativeQuery = true)
List<ProductRatingReport> getProductRatingsReportFromMV();
```

**Benefits:**
- Instant reads (pre-computed)
- Can be refreshed on schedule (e.g., every hour)
- Perfect for reporting/analytics
- Handles millions of reviews easily

---

### ✅ Solution 3: Cached Aggregation

**Add Caching:**
```java
@Cacheable(value = "productRatings", unless = "#result.isEmpty()")
@Transactional(readOnly = true)
public List<ProductRatingReport> getProductRatingsReport() {
    return productRepository.getProductRatingsReport();
}

// Clear cache when reviews are added/updated
@CacheEvict(value = "productRatings", allEntries = true)
public void clearProductRatingsCache() {
    // Called after review creation/update
}
```

**Configuration:**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour
```

**Benefits:**
- First request slow, subsequent requests instant
- Automatic expiration
- Reduces database load
- Good for frequently accessed reports

---

## 📊 Performance Comparison Summary

| Query Type | Before | After | Improvement |
|------------|--------|-------|-------------|
| N+1 Problem | 500-1000ms | 50-100ms | **10x faster** |
| Unindexed Search | 300-500ms | 20-50ms | **10-15x faster** |
| Complex Joins | 400-800ms | 50-100ms | **8x faster** |
| Aggregation | 600-1000ms | 50-100ms | **10x faster** |

---

## 🎯 General Best Practices

### 1. Always Use Indexes
```sql
-- Index foreign keys
CREATE INDEX idx_reviews_product_id ON reviews(product_id);

-- Index search columns
CREATE INDEX idx_products_name ON products(name);

-- Composite indexes for common queries
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
```

### 2. Use Query Optimization Tools

**Enable Query Logging:**
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
```

**Use EXPLAIN ANALYZE:**
```sql
EXPLAIN ANALYZE
SELECT * FROM products
WHERE description LIKE '%premium%';
```

### 3. Monitor with ExceptHub

ExceptHub automatically detects slow queries and provides:
- Query execution time
- Stack trace showing where query originated
- Frequency of slow queries
- AI-powered optimization suggestions

### 4. Set Appropriate Thresholds

```yaml
excepthub:
  slow-queries:
    enabled: true
    threshold-ms: 200  # Alert if query > 200ms
```

**Recommended Thresholds:**
- **Development:** 100ms (catch everything)
- **Staging:** 200ms (realistic threshold)
- **Production:** 500ms (only critical issues)

---

## 🚀 Next Steps

1. **Identify**: Use ExceptHub to find slow queries
2. **Analyze**: Check query execution plans
3. **Fix**: Apply appropriate solution from this guide
4. **Verify**: Measure performance improvement
5. **Monitor**: Keep ExceptHub enabled to catch regressions

---

**Need Help?**
- ExceptHub Dashboard: https://excepthub.dev
- Contact: team@excepthub.dev
