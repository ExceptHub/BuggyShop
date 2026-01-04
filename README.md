# BuggyShop - E-Commerce Testing Application

## Status Projektu

### ✅ **PROJEKT GOTOWY!**

1. **Struktura projektu** ✅
2. **pom.xml** ✅ - Spring Boot 3.5.7 + ExceptHub 1.0.13
3. **application.yml** ✅ - H2 + ExceptHub config
4. **Entities** ✅ - 10 klas (User, Product, Order, etc.)
5. **Repositories** ✅ - 10 repozytoriów
6. **Exceptions** ✅ - GlobalExceptionHandler + 4 custom exceptions
7. **DTOs** ✅ - ProductRequest, ProductResponse, OrderRequest, CartItemRequest
8. **Services** ✅ - ProductService, InventoryService, OrderService (z błędami)
9. **Controllers** ✅ - ProductController, InventoryController, OrderController
10. **DataInitializer** ✅ - 7 produktów, 2 użytkowników, 3 kupony, dane testowe
11. **TESTING_GUIDE.md** ✅ - 21+ błędów z curl commandami

## Zaplanowane Błędy (25+ endpointów):

### Product Management (5)
1. `GET /api/products?page=999` - Page out of bounds
2. `POST /api/products` - Negative price constraint
3. `PUT /api/products/{id}` - Optimistic locking failure
4. `DELETE /api/products/{id}` - Foreign key violation
5. `GET /api/products?sort=invalid` - Invalid sort field

### Inventory (4)
6. `GET /api/inventory/{id}` - Cache vs DB inconsistency
7. `PUT /api/inventory/reserve` - Race condition (overselling)
8. `POST /api/inventory/restock` - Deadlock simulation
9. `GET /api/inventory/low-stock` - N+1 query problem

### Shopping Cart (3)
10. `POST /api/cart/add` - Insufficient inventory
11. `PUT /api/cart/items/{id}` - Stale entity
12. `POST /api/cart/merge` - Transaction rollback

### Order Processing (6)
13. `POST /api/orders` - Race condition on inventory
14. `GET /api/orders/{id}` - Access control violation
15. `PUT /api/orders/{id}/cancel` - Invalid state transition
16. `POST /api/orders/{id}/payment` - External API timeout
17. `GET /api/orders/{id}/invoice` - Decimal precision error
18. `POST /api/orders/{id}/refund` - Idempotency violation

### Reviews (3)
19. `POST /api/products/{id}/reviews` - Division by zero
20. `GET /api/reviews?userId={id}` - NullPointerException
21. `DELETE /api/reviews/{id}` - Cascade delete issue

### Coupons (4)
22. `POST /api/coupons/apply` - Expired coupon
23. `POST /api/coupons/validate` - Already used
24. `GET /api/promotions/active` - Timezone bug
25. `POST /api/promotions/stack` - Business rule violation

### User Management (3)
26. `POST /api/users/register` - Duplicate email
27. `DELETE /api/users/{id}` - Cascade delete problem
28. `PUT /api/users/{id}/address` - Orphaned data

### Analytics (3)
29. `GET /api/reports/sales?from=invalid` - Invalid date
30. `GET /api/analytics/revenue` - Memory overflow
31. `POST /api/products/import` - Batch failure

### Advanced (2)
32. `POST /api/products/{id}/bundle` - Circular dependency
33. `POST /api/products/{id}/image` - File size exceeded

## 🚀 Quick Start

### 1. Skonfiguruj ExceptHub API Key

Edytuj `src/main/resources/application.yml`:
```yaml
excepthub:
  api-key: eak_YOUR_API_KEY  # Zamień na swój klucz z https://dev.excepthub.dev
```

### 2. Uruchom aplikację

```bash
cd C:\Users\ppyrc\BuggyShop
mvn clean install
mvn spring-boot:run
```

Aplikacja: **http://localhost:8081**
H2 Console: **http://localhost:8081/h2-console**

### 3. Przetestuj błędy

Otwórz `TESTING_GUIDE.md` i wykonaj curl commandy dla 21+ różnych błędów.

### 4. Sprawdź ExceptHub Dashboard

Przejdź na https://dev.excepthub.dev/dashboard i sprawdź czy:
- Wszystkie błędy zostały złapane
- HTTP context jest wypełniony (URL, method, headers)
- AI analysis poprawnie identyfikuje root cause

## ExceptHub Configuration:

```yaml
excepthub:
  enabled: true
  api-key: eak_test  # Zamień na swój klucz
  endpoint: https://dev.excepthub.dev/api/v1/errors
  service: BuggyShop
  environment: test
```

## Architektura błędów:

- **Database errors**: Foreign keys, constraints, optimistic locking, deadlocks
- **Concurrency**: Race conditions, stale data, concurrent modifications
- **Business logic**: Invalid states, expired resources, insufficient inventory
- **Data validation**: NPE, division by zero, invalid formats
- **External APIs**: Timeouts, network failures
- **Performance**: N+1, memory issues, pagination

## Następne kroki (nowy wątek):

1. Stwórz DTOs dla request/response
2. Zaimplementuj serwisy z realistycznymi błędami
3. Stwórz kontrolery z wszystkimi endpointami
4. Dodaj DataInitializer z danymi testowymi
5. Przetestuj z ExceptHub - każdy endpoint powinien generować unikalny błąd
6. Zweryfikuj czy AI analysis w ExceptHub poprawnie identyfikuje przyczyny

## Cel projektu:

Przetestować maksymalnie analiz AI w ExceptHub na realistycznych błędach które:
- NIE są oczywiste (nie `throw new RuntimeException()`)
- Wynikają z błędów technicznych, race conditions, niespójności danych
- Mają kontekst biznesowy
- Pokazują różne kategorie problemów

Projekt powinien mieć **minimum 25 różnych błędów** do testowania.
