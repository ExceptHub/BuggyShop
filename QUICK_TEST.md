# Quick Test Commands

## Setup
```bash
cd C:\Users\ppyrc\BuggyShop
mvn spring-boot:run
```

## Top 10 Errors to Test First

### 1. Product Not Found (404)
```bash
curl http://localhost:8081/api/products/999
```

### 2. Negative Price Validation
```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Bad Product","price":-10,"categoryId":1,"sku":"BAD-001"}'
```

### 3. Invalid Sort Field
```bash
curl "http://localhost:8081/api/products?sort=invalidField"
```

### 4. Insufficient Inventory
```bash
curl -X PUT "http://localhost:8081/api/inventory/7/reserve?quantity=100"
```

### 5. Expired Coupon
```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"cartId":1,"shippingAddressId":1,"couponCode":"EXPIRED"}'
```

### 6. Invalid State Transition
```bash
# First create an order, then try to cancel it twice
curl -X PUT http://localhost:8081/api/orders/1/cancel
curl -X PUT http://localhost:8081/api/orders/1/cancel
```

### 7. Page Out of Bounds
```bash
curl "http://localhost:8081/api/products?page=999"
```

### 8. Category Not Found
```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Product","price":99.99,"categoryId":999,"sku":"P-001"}'
```

### 9. Double Refund (Idempotency)
```bash
curl -X POST http://localhost:8081/api/orders/1/refund
curl -X POST http://localhost:8081/api/orders/1/refund
```

### 10. Circular Bundle Dependency
```bash
curl -X POST "http://localhost:8081/api/products/1/bundle?bundledProductId=1"
```

## Check Results

Open: https://dev.excepthub.dev/dashboard

Look for:
- ✅ 10+ different error types
- ✅ Full HTTP context (URL, method, headers)
- ✅ AI analysis with root cause
- ✅ Suggested fixes
