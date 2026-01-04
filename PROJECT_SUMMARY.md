# BuggyShop - Project Summary

## ✅ Projekt Ukończony!

**BuggyShop** to kompleksowa aplikacja e-commerce stworzona specjalnie do testowania ExceptHub z **21+ realistycznymi błędami**.

---

## 📊 Statystyki Projektu

### Kod
- **39 plików Java** (skompilowanych)
- **10 Entities** - kompletny model danych e-commerce
- **10 Repositories** - Spring Data JPA
- **3 Services** - ProductService, InventoryService, OrderService
- **3 Controllers** - 15+ endpointów
- **4 DTOs** - request/response objects
- **GlobalExceptionHandler** - centralna obsługa błędów z integracją ExceptHub
- **DataInitializer** - automatyczne tworzenie danych testowych

### Features
- **Product Management** - CRUD, categories, bundles
- **Inventory Management** - stock tracking, reservations, restocking
- **Order Processing** - cart, orders, payments, refunds, cancellations
- **Coupon System** - discounts, expiration, usage limits
- **User Management** - users, addresses

---

## 🐛 21+ Realistycznych Błędów

### Database Issues (6)
1. **Foreign Key Violation** - usuwanie produktu z aktywnymi zamówieniami
2. **Optimistic Lock Exception** - concurrent updates
3. **Data Integrity Violation** - constraint violations
4. **Deadlock** - concurrent updates w różnej kolejności
5. **N+1 Query Problem** - inefficient lazy loading
6. **Invalid Sort Field** - PropertyReferenceException

### Concurrency Issues (3)
7. **Race Condition - Overselling** - dwa requesty rezerwują ostatni item
8. **Stale Entity** - concurrent modifications
9. **Cache Inconsistency** - cached data vs DB

### Business Logic Errors (6)
10. **Insufficient Inventory** - próba zamówienia więcej niż dostępne
11. **Invalid State Transition** - anulowanie wysłanego zamówienia
12. **Expired Coupon** - użycie wygasłego kuponu
13. **Max Uses Exceeded** - przekroczenie limitu użyć kuponu
14. **Coupon Not Found** - nieistniejący kod
15. **Idempotency Violation** - double refund

### Validation & Data Errors (3)
16. **Negative Price** - validation error
17. **Page Out of Bounds** - pagination error
18. **Decimal Precision** - rounding issues

### System Errors (3)
19. **Resource Not Found** - product/category/order nie istnieje
20. **External API Timeout** - payment gateway delay
21. **Circular Dependency** - product in own bundle

### Security (1)
22. **Access Control Violation** - dostęp do cudzego zamówienia

---

## 🎯 Cel Projektu

**Przetestować AI analysis w ExceptHub na realistycznych błędach:**

### Co sprawdzamy:
1. ✅ Czy ExceptHub Starter 1.0.13 (AOP-based) łapie wszystkie błędy
2. ✅ Czy GlobalExceptionHandler nie blokuje wysyłki do ExceptHub
3. ✅ Czy http_context jest wypełniony (URL, method, headers, body)
4. ✅ Czy AI analysis poprawnie identyfikuje:
   - Root cause
   - Linię kodu
   - Sugerowane rozwiązanie
5. ✅ Czy różne typy błędów (DB, concurrency, business logic) są dobrze analizowane

---

## 📁 Struktura Projektu

```
BuggyShop/
├── src/main/java/com/buggyshop/
│   ├── entity/          # 10 JPA entities
│   ├── repository/      # 10 Spring Data repositories
│   ├── service/         # 3 services z błędami
│   ├── controller/      # 3 REST controllers
│   ├── dto/             # 4 DTOs
│   ├── exception/       # GlobalExceptionHandler + custom exceptions
│   ├── config/          # DataInitializer
│   └── BuggyShopApplication.java
├── src/main/resources/
│   └── application.yml  # H2 + ExceptHub config
├── pom.xml
├── README.md            # Główna dokumentacja
├── TESTING_GUIDE.md     # 21 curl commandów
└── PROJECT_SUMMARY.md   # Ten plik
```

---

## 🚀 Jak Przetestować

### 1. Konfiguracja
```yaml
# src/main/resources/application.yml
excepthub:
  api-key: eak_YOUR_KEY  # Twój klucz z dev.excepthub.dev
  endpoint: https://dev.excepthub.dev/api/v1/errors
```

### 2. Uruchomienie
```bash
cd C:\Users\ppyrc\BuggyShop
mvn clean install
mvn spring-boot:run
```

### 3. Testowanie
Otwórz `TESTING_GUIDE.md` i wykonaj 21+ curl commandów.

### 4. Weryfikacja
- Sprawdź https://dev.excepthub.dev/dashboard
- Zweryfikuj czy wszystkie błędy zostały złapane
- Oceń jakość AI analysis dla każdego typu błędu

---

## 🏗️ Architektura Błędów

### Poziom 1: Database Layer
- Foreign key violations
- Optimistic locking
- Deadlocks
- N+1 queries

### Poziom 2: Business Logic
- Invalid state transitions
- Insufficient resources
- Expired entities
- Authorization failures

### Poziom 3: External Systems
- API timeouts
- Network failures

### Poziom 4: Data Validation
- Constraint violations
- Format errors
- Range errors

---

## 📈 Oczekiwane Rezultaty

Po testach powinieneś mieć:

1. **~21+ różnych błędów** w ExceptHub dashboard
2. **Różne fingerprints** dla każdego typu błędu
3. **Pattern detection** - które błędy się powtarzają
4. **AI analysis** z:
   - Root cause explanation
   - Code location (file:line)
   - Suggested fix
5. **HTTP context** zawierający:
   - Request URL, method
   - Headers (Content-Type, etc.)
   - Query parameters
   - Request body (dla POST/PUT)

---

## 🎓 Wnioski z Testów

Po wykonaniu testów, oceń:

1. **Jakość AI analysis:**
   - Czy poprawnie identyfikuje root cause?
   - Czy sugestie są pomocne?
   - Czy rozróżnia różne typy błędów?

2. **Techniczne:**
   - Czy http_context jest kompletny?
   - Czy stack trace jest czytelny?
   - Czy wszystkie błędy są łapane?

3. **User Experience:**
   - Czy dashboard jest czytelny?
   - Czy łatwo znaleźć konkretny błąd?
   - Czy pattern detection działa?

---

## 🔧 Rozszerzenia (opcjonalne)

Jeśli chcesz dodać więcej błędów:

1. **ReviewService** - division by zero w average rating
2. **AnalyticsService** - memory overflow przy raportach
3. **FileUploadService** - file size exceeded
4. **BatchService** - partial commit failures
5. **CacheService** - cache stampede

---

## ✨ Kluczowe Funkcje

### ExceptHub Integration
- ✅ Spring Boot Starter 1.0.13
- ✅ AOP-based exception capture
- ✅ Works with GlobalExceptionHandler
- ✅ Full HTTP context capture
- ✅ Automatic error deduplication

### Realistic Bugs
- ❌ NIE są to `throw new RuntimeException()`
- ✅ Wynikają z prawdziwych scenariuszy biznesowych
- ✅ Mają kontekst (concurrency, state, resources)
- ✅ Różne kategorie (DB, business, validation, external)

---

## 📞 Support

Jeśli napotkasz problemy:
1. Sprawdź logi aplikacji
2. Sprawdź ExceptHub dashboard
3. Zobacz czy starter jest włączony: `excepthub.enabled=true`
4. Sprawdź czy API key jest prawidłowy

---

**Powodzenia w testowaniu!** 🚀
