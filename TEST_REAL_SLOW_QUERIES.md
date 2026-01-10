# Test REAL Slow Queries (Not N+1)

These endpoints execute **SINGLE SLOW QUERIES** that take a long time to complete.
Perfect for testing slow query detection (not N+1 detection).

Base URL: `https://buggyshop.onrender.com`

---

## 🐌 Slow Query Test Endpoints

### 1. Sleep Query (Guaranteed Slow)
**Single query with pg_sleep() - takes exactly X seconds**

```bash
# 2 second sleep (default)
curl "https://buggyshop.onrender.com/api/test-slow-query/sleep"

# 5 second sleep
curl "https://buggyshop.onrender.com/api/test-slow-query/sleep?seconds=5"

# 1 second sleep (should trigger with 100ms threshold)
curl "https://buggyshop.onrender.com/api/test-slow-query/sleep?seconds=1"

# 500ms sleep (should trigger with 100ms threshold)
curl "https://buggyshop.onrender.com/api/test-slow-query/sleep?seconds=0.5"
```

**What it does:** Executes `SELECT pg_sleep(X), COUNT(*) FROM products`
- Single SQL query
- Takes exactly X seconds to complete
- Will be detected as slow query if X > threshold

---

### 2. Cartesian Product (CROSS JOIN)
**Creates massive result set with CROSS JOIN**

```bash
curl "https://buggyshop.onrender.com/api/test-slow-query/cartesian"
```

**What it does:**
- CROSS JOIN: products × products × products
- Creates N³ combinations (7 products = 343 combinations)
- Single slow query

---

### 3. Complex Aggregation
**Multiple nested subqueries**

```bash
curl "https://buggyshop.onrender.com/api/test-slow-query/complex-aggregation"
```

**What it does:**
- 4 subqueries per product (count reviews, avg rating, times ordered, quantity)
- Nested WHERE with subquery
- Single query but very complex

---

### 4. LIKE Full Table Scan
**Full table scan with LIKE pattern**

```bash
# Scan all
curl "https://buggyshop.onrender.com/api/test-slow-query/like-scan"

# Search pattern
curl "https://buggyshop.onrender.com/api/test-slow-query/like-scan?pattern=%guide%"

curl "https://buggyshop.onrender.com/api/test-slow-query/like-scan?pattern=%premium%"
```

**What it does:**
- Full table scan on unindexed columns
- LIKE pattern matching
- Subquery for review count

---

### 5. Sleep + JOIN (Guaranteed Slow)
**Combines sleep with complex joins**

```bash
# 1 second with joins
curl "https://buggyshop.onrender.com/api/test-slow-query/sleep-with-join?seconds=1"

# 3 seconds with joins
curl "https://buggyshop.onrender.com/api/test-slow-query/sleep-with-join?seconds=3"

# 500ms with joins
curl "https://buggyshop.onrender.com/api/test-slow-query/sleep-with-join?seconds=0.5"
```

**What it does:**
- pg_sleep(X) + LEFT JOINs + GROUP BY
- Single slow query
- Guaranteed to exceed threshold

---

## 📊 Expected Results

### With threshold = 100ms:

| Endpoint | Expected Time | Should Be Detected? |
|----------|--------------|---------------------|
| `/sleep?seconds=1` | ~1000ms | ✅ YES |
| `/sleep?seconds=0.5` | ~500ms | ✅ YES |
| `/sleep?seconds=0.2` | ~200ms | ✅ YES |
| `/cartesian` | Variable | ✅ Likely YES |
| `/complex-aggregation` | Variable | ⚠️ Maybe |
| `/like-scan` | Variable | ⚠️ Maybe |
| `/sleep-with-join?seconds=1` | ~1000ms+ | ✅ YES |

---

## 🎯 Quick Test Script

```bash
#!/bin/bash
BASE_URL="https://buggyshop.onrender.com/api/test-slow-query"

echo "🐌 Testing REAL slow queries..."

echo "1️⃣ Testing 2-second sleep..."
curl -s "$BASE_URL/sleep?seconds=2" > /dev/null && echo "✓ Completed"

echo "2️⃣ Testing 1-second sleep..."
curl -s "$BASE_URL/sleep?seconds=1" > /dev/null && echo "✓ Completed"

echo "3️⃣ Testing sleep + JOIN..."
curl -s "$BASE_URL/sleep-with-join?seconds=1" > /dev/null && echo "✓ Completed"

echo "4️⃣ Testing cartesian product..."
curl -s "$BASE_URL/cartesian" > /dev/null && echo "✓ Completed"

echo "5️⃣ Testing complex aggregation..."
curl -s "$BASE_URL/complex-aggregation" > /dev/null && echo "✓ Completed"

echo "🎉 All slow query tests completed!"
echo "Check ExceptHub dashboard for slow query detections."
```

---

## ⚠️ Important Differences

### N+1 Problem (Old endpoints):
- `/api/analytics/products-with-reviews`
- Multiple FAST queries (1 + N queries)
- Each query < 100ms
- Total time might be > 100ms

### Real Slow Query (New endpoints):
- `/api/test-slow-query/sleep`
- **Single SLOW query**
- One query > 100ms
- Guaranteed to be detected

---

## 🔍 Debugging

If slow queries are NOT detected:

1. **Check threshold** in `application.yml`:
   ```yaml
   excepthub:
     slow-queries:
       threshold-ms: 100
   ```

2. **Check logs** for:
   ```
   🐢 Slow query detected (XXXms): SELECT pg_sleep...
   ```

3. **Verify library version**:
   ```xml
   <dependency>
     <groupId>io.github.excepthub</groupId>
     <artifactId>excepthub-spring-boot-starter</artifactId>
     <version>1.0.19</version>
   </dependency>
   ```

4. **Check database type**: pg_sleep() only works on PostgreSQL

---

**Use these endpoints to test ACTUAL slow query detection, not N+1!** 🚀
