# BuggyShop API - Complete Test Endpoints for Error & Slow Query Detection

**Base URL:** `https://buggyshop.onrender.com`

This comprehensive guide contains curl commands to test all error scenarios and slow query patterns in the BuggyShop demo application for ExceptHub monitoring.

---

## 📋 Table of Contents
1. [Slow Query Endpoints](#-slow-query-endpoints)
2. [Resource Not Found Errors](#-resource-not-found-errors-404)
3. [Validation & Business Logic Errors](#-validation--business-logic-errors)
4. [State Transition Errors](#-state-transition-errors)
5. [Inventory & Stock Errors](#-inventory--stock-errors)
6. [Coupon & Discount Errors](#-coupon--discount-errors)
7. [Payment Processing Errors](#-payment-processing-errors)
8. [Security & Authorization Errors](#-security--authorization-errors)
9. [Bulk Testing Scripts](#-bulk-testing-scripts)

---

## 🐌 Slow Query Endpoints

These endpoints intentionally trigger slow database queries to demonstrate ExceptHub's slow query detection.

### 1. N+1 Query Problem
**Problem:** Fetches all products then triggers separate query for each product's reviews.

```bash
curl -X GET "https://buggyshop.onrender.com/api/analytics/products-with-reviews"
```

**What it does:** Generates 1 query for products + N queries for reviews (one per product).

---

### 2. Unindexed Column Search
**Problem:** Full table scan on unindexed `description` column.

```bash
curl -X GET "https://buggyshop.onrender.com/api/analytics/search-by-description?keyword=premium"
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/analytics/search-by-description?keyword=quality"
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/analytics/search-by-description?keyword=professional"
```

---

### 3. Unoptimized Complex Joins
**Problem:** Multiple separate queries instead of JOIN FETCH.

```bash
curl -X GET "https://buggyshop.onrender.com/api/analytics/order-details/1"
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/analytics/order-details/2"
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/analytics/order-details/3"
```

---

### 4. Aggregation Without Index
**Problem:** Scans all reviews to calculate average ratings.

```bash
curl -X GET "https://buggyshop.onrender.com/api/analytics/product-ratings-report"
```

---

## 🔍 Resource Not Found Errors (404)

### 1. Product Not Found

```bash
curl -X GET "https://buggyshop.onrender.com/api/products/999999"
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/products/88888"
```

```bash
curl -X PUT "https://buggyshop.onrender.com/api/products/999999" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Product",
    "description": "Test",
    "price": 99.99,
    "categoryId": 1
  }'
```

```bash
curl -X DELETE "https://buggyshop.onrender.com/api/products/999999"
```

**Expected Error:**
```
Product not found
```

---

### 2. Category Not Found

```bash
curl -X POST "https://buggyshop.onrender.com/api/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Product",
    "description": "Test product",
    "price": 99.99,
    "categoryId": 999999,
    "sku": "TEST-001"
  }'
```

```bash
curl -X PUT "https://buggyshop.onrender.com/api/products/1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Product",
    "description": "Test",
    "price": 99.99,
    "categoryId": 888888
  }'
```

**Expected Error:**
```
Category not found
```

---

### 3. User Not Found

```bash
curl -X POST "https://buggyshop.onrender.com/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 999999,
    "cartId": 1,
    "shippingAddressId": 1
  }'
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/orders/user/999999"
```

**Expected Error:**
```
User not found
```

---

### 4. Cart Not Found

```bash
curl -X POST "https://buggyshop.onrender.com/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 999999,
    "shippingAddressId": 1
  }'
```

**Expected Error:**
```
Cart not found
```

---

### 5. Shipping Address Not Found

```bash
curl -X POST "https://buggyshop.onrender.com/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 999999
  }'
```

**Expected Error:**
```
Shipping address not found
```

---

### 6. Inventory Not Found

```bash
curl -X GET "https://buggyshop.onrender.com/api/inventory/999999"
```

```bash
curl -X PUT "https://buggyshop.onrender.com/api/inventory/999999/reserve?quantity=5"
```

```bash
curl -X POST "https://buggyshop.onrender.com/api/inventory/999999/restock?quantity=10"
```

**Expected Error:**
```
Inventory not found
```

---

### 7. Order Not Found (Analytics)

```bash
curl -X GET "https://buggyshop.onrender.com/api/analytics/order-details/999999"
```

**Expected Error:**
```
Order not found
```

---

## ✅ Validation & Business Logic Errors

### 1. Invalid Sort Field

```bash
curl -X GET "https://buggyshop.onrender.com/api/products?sortBy=invalidField"
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/products?sortBy=categoryName"
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/products?sortBy=description"
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/products?sortBy=stock"
```

**Expected Error:**
```
Invalid sort field: 'invalidField'. Allowed fields: name, price, createdAt, id
```

---

### 2. Circular Bundle Dependency

```bash
curl -X POST "https://buggyshop.onrender.com/api/products/1/bundle?bundledProductId=1"
```

```bash
curl -X POST "https://buggyshop.onrender.com/api/products/5/bundle?bundledProductId=5"
```

```bash
curl -X POST "https://buggyshop.onrender.com/api/products/10/bundle?bundledProductId=10"
```

**Expected Error:**
```
Cannot add product to its own bundle. Circular dependency detected.
```

---

### 3. Product Not Found in Bundle

```bash
curl -X POST "https://buggyshop.onrender.com/api/products/1/bundle?bundledProductId=999999"
```

```bash
curl -X POST "https://buggyshop.onrender.com/api/products/999999/bundle?bundledProductId=1"
```

**Expected Error:**
```
Product not found / Bundled product not found
```

---

### 4. Empty Cart

```bash
curl -X POST "https://buggyshop.onrender.com/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1,
    "items": []
  }'
```

**Expected Error:**
```
Cart is empty
```

---

## 🔄 State Transition Errors

### 1. Cannot Process Payment - Invalid Status

```bash
# Assuming order 1 is not in PENDING status
curl -X POST "https://buggyshop.onrender.com/api/orders/1/payment?paymentMethod=credit_card"
```

**Expected Error:**
```
Cannot process payment. Order status is: PAID (or CANCELLED, SHIPPED, etc.)
```

---

### 2. Cannot Cancel Shipped/Delivered Order

```bash
# Try to cancel an order that's already shipped
curl -X PUT "https://buggyshop.onrender.com/api/orders/1/cancel"
```

```bash
curl -X PUT "https://buggyshop.onrender.com/api/orders/2/cancel"
```

**Expected Error:**
```
Cannot cancel order that has been shipped or delivered
```

---

### 3. Order Already Cancelled

```bash
# Cancel the same order twice
curl -X PUT "https://buggyshop.onrender.com/api/orders/5/cancel"
curl -X PUT "https://buggyshop.onrender.com/api/orders/5/cancel"
```

**Expected Error:**
```
Order is already cancelled
```

---

### 4. Invalid Refund State

```bash
# Try to refund an order that's not paid or shipped
curl -X POST "https://buggyshop.onrender.com/api/orders/1/refund"
```

```bash
curl -X POST "https://buggyshop.onrender.com/api/orders/2/refund"
```

**Expected Error:**
```
Can only refund paid or shipped orders
```

---

### 5. Order Already Refunded

```bash
# Refund the same order twice (first succeed, second fail)
curl -X POST "https://buggyshop.onrender.com/api/orders/3/refund"
curl -X POST "https://buggyshop.onrender.com/api/orders/3/refund"
```

**Expected Error:**
```
Order has already been refunded
```

---

## 📦 Inventory & Stock Errors

### 1. Insufficient Inventory

```bash
curl -X PUT "https://buggyshop.onrender.com/api/inventory/1/reserve?quantity=999999"
```

```bash
curl -X PUT "https://buggyshop.onrender.com/api/inventory/2/reserve?quantity=100000"
```

```bash
curl -X PUT "https://buggyshop.onrender.com/api/inventory/3/reserve?quantity=50000"
```

**Expected Error:**
```
Insufficient stock. Available: X, Requested: 999999
```

---

### 2. Insufficient Inventory During Order Creation

```bash
# This would trigger InsufficientInventoryException when reserving stock
curl -X POST "https://buggyshop.onrender.com/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1,
    "items": [
      {
        "productId": 1,
        "quantity": 999999
      }
    ]
  }'
```

**Expected Error:**
```
Product 'XYZ' has insufficient stock
```

---

## 🎟️ Coupon & Discount Errors

### 1. Coupon Not Found

```bash
curl -X POST "https://buggyshop.onrender.com/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1,
    "couponCode": "INVALID_COUPON_2024"
  }'
```

```bash
curl -X POST "https://buggyshop.onrender.com/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1,
    "couponCode": "NONEXISTENT123"
  }'
```

**Expected Error:**
```
Coupon not found
```

---

### 2. Coupon Has Expired

```bash
# Assuming there's an expired coupon in the database
curl -X POST "https://buggyshop.onrender.com/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1,
    "couponCode": "EXPIRED2023"
  }'
```

**Expected Error:**
```
Coupon has expired
```

---

### 3. Coupon Reached Maximum Usage Limit

```bash
# Assuming there's a coupon with maxUses reached
curl -X POST "https://buggyshop.onrender.com/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1,
    "couponCode": "MAXED_OUT"
  }'
```

**Expected Error:**
```
Coupon has reached maximum usage limit
```

---

## 💳 Payment Processing Errors

### 1. Payment Gateway Timeout (Random 10%)

**Note:** This error has a 10% chance of occurring. Run multiple times to trigger it.

```bash
# Run this 10-20 times to hit the random error
for i in {1..15}; do
  curl -X POST "https://buggyshop.onrender.com/api/orders/1/payment?paymentMethod=credit_card"
  echo ""
done
```

```bash
curl -X POST "https://buggyshop.onrender.com/api/orders/2/payment?paymentMethod=paypal"
```

```bash
curl -X POST "https://buggyshop.onrender.com/api/orders/3/payment?paymentMethod=debit_card"
```

```bash
curl -X POST "https://buggyshop.onrender.com/api/orders/4/payment?paymentMethod=bank_transfer"
```

**Expected Error (10% of the time):**
```
Payment gateway timeout
```

---

### 2. Payment Processing Interrupted

```bash
# This simulates an interrupted payment process
curl -X POST "https://buggyshop.onrender.com/api/orders/5/payment?paymentMethod=credit_card" &
# Quickly terminate the request
```

**Expected Error:**
```
Payment processing interrupted
```

---

## 🔒 Security & Authorization Errors

### 1. Unauthorized Access to Order

```bash
# Try to access order 1 with wrong user ID
curl -X GET "https://buggyshop.onrender.com/api/orders/1?userId=999"
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/orders/2?userId=888"
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/orders/3?userId=777"
```

```bash
curl -X GET "https://buggyshop.onrender.com/api/orders/5?userId=666"
```

**Expected Error:**
```
Access denied. This order belongs to another user
```

---

## 🔄 Bulk Testing Scripts

### Test All Slow Queries

```bash
#!/bin/bash
echo "🐌 Testing all slow queries..."
curl -s "https://buggyshop.onrender.com/api/analytics/products-with-reviews" > /dev/null
echo "✓ N+1 query tested"

curl -s "https://buggyshop.onrender.com/api/analytics/search-by-description?keyword=premium" > /dev/null
echo "✓ Unindexed search tested"

curl -s "https://buggyshop.onrender.com/api/analytics/order-details/1" > /dev/null
echo "✓ Unoptimized joins tested"

curl -s "https://buggyshop.onrender.com/api/analytics/product-ratings-report" > /dev/null
echo "✓ Aggregation tested"

echo "🎉 Slow query tests completed!"
```

---

### Test All Resource Not Found Errors

```bash
#!/bin/bash
echo "🔍 Testing Resource Not Found errors..."

curl -s "https://buggyshop.onrender.com/api/products/999999" > /dev/null
echo "✓ Product not found"

curl -s "https://buggyshop.onrender.com/api/inventory/999999" > /dev/null
echo "✓ Inventory not found"

curl -s "https://buggyshop.onrender.com/api/analytics/order-details/999999" > /dev/null
echo "✓ Order not found"

curl -s -X POST "https://buggyshop.onrender.com/api/orders" \
  -H "Content-Type: application/json" \
  -d '{"userId":999999,"cartId":1,"shippingAddressId":1}' > /dev/null
echo "✓ User not found"

curl -s -X POST "https://buggyshop.onrender.com/api/orders" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"cartId":999999,"shippingAddressId":1}' > /dev/null
echo "✓ Cart not found"

echo "🎉 Resource not found tests completed!"
```

---

### Test All Validation Errors

```bash
#!/bin/bash
echo "✅ Testing validation errors..."

curl -s "https://buggyshop.onrender.com/api/products?sortBy=invalidField" > /dev/null
echo "✓ Invalid sort field"

curl -s -X POST "https://buggyshop.onrender.com/api/products/1/bundle?bundledProductId=1" > /dev/null
echo "✓ Circular bundle dependency"

curl -s -X PUT "https://buggyshop.onrender.com/api/inventory/1/reserve?quantity=999999" > /dev/null
echo "✓ Insufficient inventory"

curl -s -X GET "https://buggyshop.onrender.com/api/orders/1?userId=999" > /dev/null
echo "✓ Unauthorized access"

echo "🎉 Validation tests completed!"
```

---

### Test Payment Gateway Timeouts (Stress Test)

```bash
#!/bin/bash
echo "💳 Testing payment gateway (10% random errors)..."

success=0
errors=0

for i in {1..20}; do
  response=$(curl -s -w "%{http_code}" "https://buggyshop.onrender.com/api/orders/1/payment?paymentMethod=credit_card")
  http_code="${response: -3}"

  if [ "$http_code" == "200" ]; then
    ((success++))
  else
    ((errors++))
  fi

  echo "Request $i: HTTP $http_code"
done

echo ""
echo "📊 Results: $success successful, $errors errors"
echo "🎉 Payment gateway test completed!"
```

---

### Complete Stress Test (All Errors)

```bash
#!/bin/bash
echo "🚀 Running comprehensive error generation test..."

# Resource not found errors
curl -s "https://buggyshop.onrender.com/api/products/999999" &
curl -s "https://buggyshop.onrender.com/api/inventory/999999" &
curl -s "https://buggyshop.onrender.com/api/analytics/order-details/999999" &

# Validation errors
curl -s "https://buggyshop.onrender.com/api/products?sortBy=invalidField" &
curl -s -X POST "https://buggyshop.onrender.com/api/products/1/bundle?bundledProductId=1" &

# Inventory errors
curl -s -X PUT "https://buggyshop.onrender.com/api/inventory/1/reserve?quantity=999999" &
curl -s -X PUT "https://buggyshop.onrender.com/api/inventory/2/reserve?quantity=100000" &

# Security errors
curl -s -X GET "https://buggyshop.onrender.com/api/orders/1?userId=999" &
curl -s -X GET "https://buggyshop.onrender.com/api/orders/2?userId=888" &

# Payment errors (run multiple for random timeout)
for i in {1..10}; do
  curl -s "https://buggyshop.onrender.com/api/orders/1/payment?paymentMethod=credit_card" &
done

# Slow queries
curl -s "https://buggyshop.onrender.com/api/analytics/products-with-reviews" &
curl -s "https://buggyshop.onrender.com/api/analytics/search-by-description?keyword=premium" &
curl -s "https://buggyshop.onrender.com/api/analytics/order-details/1" &
curl -s "https://buggyshop.onrender.com/api/analytics/product-ratings-report" &

wait
echo "🎉 Stress test completed! Check ExceptHub dashboard for results."
```

---

## 📊 Expected Results in ExceptHub Dashboard

After running these curls, you should see the following grouped errors in ExceptHub:

### Error Categories:

1. **ResourceNotFoundException** (~7 patterns)
   - Product not found
   - Category not found
   - User not found
   - Cart not found
   - Shipping address not found
   - Inventory not found
   - Order not found

2. **IllegalArgumentException** (~6 patterns)
   - Invalid sort field
   - Circular bundle dependency
   - Cart is empty
   - Coupon expired
   - Coupon max uses reached
   - Negative quantity

3. **InsufficientInventoryException** (~2 patterns)
   - Insufficient stock during reservation
   - Insufficient stock during order creation

4. **InvalidStateTransitionException** (~4 patterns)
   - Cannot cancel shipped order
   - Order already cancelled
   - Cannot process payment (invalid status)
   - Can only refund paid/shipped orders

5. **IllegalStateException** (~1 pattern)
   - Order already refunded

6. **SecurityException** (~1 pattern)
   - Unauthorized access to order

7. **RuntimeException** (~3 patterns)
   - Payment gateway timeout
   - Payment processing interrupted
   - Order not found (analytics)

### Slow Query Categories:

1. **N+1 Query Problem** - products-with-reviews
2. **Full Table Scan** - search-by-description
3. **Unoptimized Joins** - order-details
4. **Heavy Aggregation** - product-ratings-report

### Pattern Detection Features:

- Error grouping by type and message
- Frequency tracking
- First/last occurrence timestamps
- AI-powered root cause analysis
- Recommendations for fixes
- Stack trace fingerprinting

---

## 🎯 Testing Tips

1. **Generate diverse errors** - Run different variations to test fingerprinting
2. **Mix error types** - Combine multiple error scenarios in single test run
3. **Use different IDs** - Vary product/order IDs to create different stack traces
4. **Monitor in real-time** - Keep ExceptHub dashboard open while testing
5. **Run stress tests** - Use bulk scripts to generate high volume
6. **Check slow query thresholds** - Adjust if queries aren't detected
7. **Verify error grouping** - Same error types should group together
8. **Test AI analysis** - Check if AI recommendations are generated
9. **Validate notifications** - If configured, check if alerts are sent
10. **Review historical data** - Check error trends over time

---

## 🔗 Additional Endpoints

### Health Check
```bash
curl -X GET "https://buggyshop.onrender.com/actuator/health"
```

### Get All Products (Normal Operation)
```bash
curl -X GET "https://buggyshop.onrender.com/api/products"
```

### Get Product by ID (Normal Operation)
```bash
curl -X GET "https://buggyshop.onrender.com/api/products/1"
```

### Get Low Stock Products
```bash
curl -X GET "https://buggyshop.onrender.com/api/inventory/low-stock"
```

---

## 📝 Notes

- All error endpoints are intentionally buggy for demonstration purposes
- Payment gateway timeout has 10% randomness - run multiple times
- Slow queries are real performance issues, not simulated
- Error grouping uses fingerprinting algorithm - similar errors cluster together
- All endpoints are production-ready on Render: `https://buggyshop.onrender.com`

---

**Happy Testing! 🚀**

For questions or issues, refer to ExceptHub documentation at https://excepthub.dev
