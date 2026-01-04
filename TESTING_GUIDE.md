# BuggyShop - Testing Guide

## Uruchomienie Aplikacji

```bash
cd C:\Users\ppyrc\BuggyShop
mvn clean install
mvn spring-boot:run
```

Aplikacja: `http://localhost:8081`
H2 Console: `http://localhost:8081/h2-console`

## Konfiguracja ExceptHub

Zmień API key w `application.yml`:
```yaml
excepthub:
  api-key: eak_YOUR_API_KEY  # Zamień na swój klucz
  endpoint: https://dev.excepthub.dev/api/v1/errors
```

---

## 25+ Bugs do Przetestowania

### Product Management (5 bugs)

#### 1. Page Out of Bounds
**Endpoint:** `GET /api/products?page=999`
**Błąd:** Pagination error - próba dostępu do nieistniejącej strony

```bash
curl "http://localhost:8081/api/products?page=999&size=10"
```

**Oczekiwany błąd:** Empty page or IndexOutOfBoundsException

---

#### 2. Negative Price Constraint Violation
**Endpoint:** `POST /api/products`
**Błąd:** Validation - cena ujemna

```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Invalid Product",
    "price": -10.00,
    "categoryId": 1,
    "sku": "INVALID-001"
  }'
```

**Oczekiwany błąd:** Validation failed - Price must be greater than 0

---

#### 3. Optimistic Locking Failure
**Endpoint:** `PUT /api/products/{id}`
**Błąd:** Concurrent modification - dwa requesty aktualizują ten sam produkt

```bash
# Terminal 1
curl -X PUT http://localhost:8081/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Laptop",
    "price": 1399.99,
    "categoryId": 1,
    "sku": "LAPTOP-001"
  }'

# Terminal 2 (równocześnie)
curl -X PUT http://localhost:8081/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Different Update",
    "price": 1499.99,
    "categoryId": 1,
    "sku": "LAPTOP-001"
  }'
```

**Oczekiwany błąd:** OptimisticLockException

---

#### 4. Foreign Key Violation
**Endpoint:** `DELETE /api/products/{id}`
**Błąd:** Próba usunięcia produktu który ma powiązania (order items)

```bash
# Najpierw utwórz zamówienie z produktem 1, potem próbuj usunąć
curl -X DELETE http://localhost:8081/api/products/1
```

**Oczekiwany błąd:** DataIntegrityViolationException - Cannot delete product with existing orders

---

#### 5. Invalid Sort Field
**Endpoint:** `GET /api/products?sort=nonExistentField`
**Błąd:** Nieprawidłowe pole sortowania

```bash
curl "http://localhost:8081/api/products?sort=invalidField"
```

**Oczekiwany błąd:** PropertyReferenceException - No property 'invalidField' found

---

### Inventory Management (4 bugs)

#### 6. Cache vs DB Inconsistency
**Endpoint:** `GET /api/inventory/{id}`
**Błąd:** Cached data różni się od DB (po direct DB update)

```bash
# Pobierz inventory (trafia do cache)
curl http://localhost:8081/api/inventory/1

# Zaktualizuj bezpośrednio w H2 Console
# UPDATE inventory SET quantity = 999 WHERE product_id = 1

# Pobierz ponownie (zwróci stare dane z cache)
curl http://localhost:8081/api/inventory/1
```

**Oczekiwany błąd:** Stale cached data

---

#### 7. Race Condition - Overselling
**Endpoint:** `PUT /api/inventory/{id}/reserve`
**Błąd:** Dwa równoczesne requesty rezerwują ostatni item

```bash
# Terminal 1 & 2 równocześnie (product 7 ma tylko 5 sztuk)
curl -X PUT "http://localhost:8081/api/inventory/7/reserve?quantity=3"
curl -X PUT "http://localhost:8081/api/inventory/7/reserve?quantity=3"
```

**Oczekiwany błąd:** InsufficientInventoryException (jeden z requestów powinien failnąć)

---

#### 8. Deadlock Simulation
**Endpoint:** `POST /api/inventory/{id}/restock`
**Błąd:** Równoczesny update wielu produktów w różnej kolejności

```bash
# Terminal 1
curl -X POST "http://localhost:8081/api/inventory/1/restock?quantity=10"
curl -X POST "http://localhost:8081/api/inventory/2/restock?quantity=10"

# Terminal 2 (równocześnie, odwrotna kolejność)
curl -X POST "http://localhost:8081/api/inventory/2/restock?quantity=10"
curl -X POST "http://localhost:8081/api/inventory/1/restock?quantity=10"
```

**Oczekiwany błąd:** Potential deadlock or timeout

---

#### 9. N+1 Query Problem
**Endpoint:** `GET /api/inventory/low-stock`
**Błąd:** N+1 queries - dla każdego inventory osobne query dla product

```bash
curl http://localhost:8081/api/inventory/low-stock
```

**Oczekiwany efekt:** Sprawdź logi - powinno być N+1 SELECT queries

---

### Order Processing (6 bugs)

#### 10. Insufficient Inventory on Order Creation
**Endpoint:** `POST /api/orders`
**Błąd:** Próba zamówienia większej ilości niż dostępne

```bash
# Dodaj do koszyka więcej niż dostępne (product 7 ma tylko 5)
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1,
    "items": [
      {"productId": 7, "quantity": 10}
    ]
  }'
```

**Oczekiwany błąd:** InsufficientInventoryException

---

#### 11. Access Control Violation
**Endpoint:** `GET /api/orders/{id}?userId={wrongUserId}`
**Błąd:** Próba dostępu do zamówienia innego użytkownika

```bash
# Utwórz zamówienie dla user 1, potem próbuj pobrać jako user 2
curl "http://localhost:8081/api/orders/1?userId=2"
```

**Oczekiwany błąd:** SecurityException - Access denied

---

#### 12. Invalid State Transition
**Endpoint:** `PUT /api/orders/{id}/cancel`
**Błąd:** Próba anulowania wysłanego zamówienia

```bash
# Najpierw zmień status na SHIPPED w H2 Console
# UPDATE orders SET status = 'SHIPPED' WHERE id = 1

# Potem próbuj anulować
curl -X PUT http://localhost:8081/api/orders/1/cancel
```

**Oczekiwany błąd:** InvalidStateTransitionException - Cannot cancel shipped order

---

#### 13. External API Timeout
**Endpoint:** `POST /api/orders/{id}/payment`
**Błąd:** Payment gateway timeout (10% szansa w symulacji)

```bash
# Wywołuj wielokrotnie aż wystąpi timeout
curl -X POST "http://localhost:8081/api/orders/1/payment?paymentMethod=CARD"
```

**Oczekiwany błąd:** RuntimeException - Payment gateway timeout

---

#### 14. Decimal Precision Error
**Endpoint:** Występuje przy obliczaniu finalTotal z discount

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1,
    "couponCode": "SAVE10"
  }'
```

**Oczekiwany efekt:** Sprawdź czy rounding jest prawidłowy (może być 0.001 różnicy)

---

#### 15. Idempotency Violation - Double Refund
**Endpoint:** `POST /api/orders/{id}/refund`
**Błąd:** Próba zwrotu pieniędzy za już zwrócone zamówienie

```bash
# Pierwszy refund
curl -X POST http://localhost:8081/api/orders/1/refund

# Drugi refund (powinien failnąć)
curl -X POST http://localhost:8081/api/orders/1/refund
```

**Oczekiwany błąd:** IllegalStateException - Order has already been refunded

---

### Coupons & Promotions (3 bugs)

#### 16. Expired Coupon
**Endpoint:** `POST /api/orders` z expired coupon

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1,
    "couponCode": "EXPIRED"
  }'
```

**Oczekiwany błąd:** IllegalArgumentException - Coupon has expired

---

#### 17. Max Uses Exceeded
**Endpoint:** `POST /api/orders` z fully used coupon

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1,
    "couponCode": "LIMITED"
  }'
```

**Oczekiwany błąd:** IllegalArgumentException - Coupon has reached maximum usage limit

---

#### 18. Invalid Coupon Code
**Endpoint:** `POST /api/orders` z nieistniejącym kodem

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1,
    "couponCode": "NONEXISTENT"
  }'
```

**Oczekiwany błąd:** ResourceNotFoundException - Coupon not found

---

### Advanced Bugs (3 bugs)

#### 19. Circular Dependency in Bundle
**Endpoint:** `POST /api/products/{id}/bundle`
**Błąd:** Produkt dodany do własnego bundle (rekursja)

```bash
# Dodaj produkt 1 do swojego własnego bundle
curl -X POST "http://localhost:8081/api/products/1/bundle?bundledProductId=1"

# Lub: Produkt A -> B, B -> A
curl -X POST "http://localhost:8081/api/products/1/bundle?bundledProductId=2"
curl -X POST "http://localhost:8081/api/products/2/bundle?bundledProductId=1"
```

**Oczekiwany efekt:** Stack overflow przy ładowaniu lub infinite loop

---

#### 20. Category Not Found
**Endpoint:** `POST /api/products`
**Błąd:** Nieistniejąca kategoria

```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Product",
    "price": 99.99,
    "categoryId": 999,
    "sku": "NEW-001"
  }'
```

**Oczekiwany błąd:** ResourceNotFoundException - Category not found

---

#### 21. Product Not Found
**Endpoint:** `GET /api/products/{id}`

```bash
curl http://localhost:8081/api/products/999
```

**Oczekiwany błąd:** ResourceNotFoundException - Product not found

---

## Statystyki Błędów

Po wykonaniu wszystkich testów, sprawdź ExceptHub dashboard:

1. **Ile różnych typów błędów** zostało złapanych
2. **Czy AI analysis** poprawnie identyfikuje:
   - Root cause
   - Linię kodu która rzuca błąd
   - Sugerowane rozwiązanie
3. **Czy http_context** zawiera:
   - URL, method, headers, parameters
   - User agent
   - Request body (jeśli POST/PUT)

## Dodatkowe Testy

### Concurrent Order Creation (Race Condition)
Wywołaj równocześnie w 2 terminalach aby zasymulować race condition:

```bash
# Terminal 1 & 2
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "cartId": 1,
    "shippingAddressId": 1
  }'
```

### Memory Test - Load All Products
```bash
# Jeśli dodasz dużo produktów, to może być memory issue
for i in {1..1000}; do
  curl http://localhost:8081/api/products?page=$i&size=100
done
```

---

## Oczekiwane Wyniki

Każdy błąd powinien:
1. Być złapany przez GlobalExceptionHandler
2. Wysłany do ExceptHub przez starter (AOP aspect)
3. Zawierać pełny stack trace
4. Zawierać http_context (URL, method, etc.)
5. Mieć AI analysis który sugeruje fix

**Cel:** Przetestować czy ExceptHub AI potrafi zidentyfikować i zasugerować rozwiązanie dla realistycznych błędów.
