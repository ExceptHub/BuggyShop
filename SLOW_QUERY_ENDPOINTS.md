# Slow Query Testing Endpoints

Dodano 4 endpointy do testowania wykrywania wolnych zapytań w ExceptHub. Wszystkie endpointy znajdują się w `/api/analytics/*` i są specjalnie zaprojektowane z naturalnymi problemami wydajnościowymi.

## Endpointy

### 1. GET `/api/analytics/products-with-reviews`
**Problem**: N+1 Query Problem

Pobiera wszystkie produkty wraz z ich recenzjami, powodując:
- 1 zapytanie SQL do pobrania wszystkich produktów
- N osobnych zapytań do pobrania recenzji dla każdego produktu (lazy loading)

**Przykład**:
```bash
curl http://localhost:8080/api/analytics/products-with-reviews
```

**Oczekiwane zachowanie**:
Jeśli jest 7 produktów, zostanie wykonanych 8 zapytań SQL (1 + 7).

---

### 2. GET `/api/analytics/search-by-description?keyword={keyword}`
**Problem**: Wyszukiwanie po kolumnie bez indeksu

Wyszukuje produkty po polu `description` używając `LIKE`, które nie ma indeksu. Wymaga pełnego skanowania tabeli.

**Przykład**:
```bash
curl "http://localhost:8080/api/analytics/search-by-description?keyword=guide"
```

**Oczekiwane zachowanie**:
Zapytanie musi przeskanować wszystkie rekordy w tabeli `products` bez użycia indeksu.

---

### 3. GET `/api/analytics/order-details/{id}`
**Problem**: Kompleksowe JOIN-y bez optymalizacji

Pobiera szczegóły zamówienia wraz z wszystkimi powiązanymi danymi (user, items, products, categories, address) w nieefektywny sposób - każda relacja powoduje osobne zapytanie.

**Przykład**:
```bash
curl http://localhost:8080/api/analytics/order-details/1
```

**Oczekiwane zachowanie**:
Dla zamówienia z 3 produktami zostanie wykonanych ~10+ zapytań:
- 1 dla zamówienia
- 1 dla użytkownika
- 1 dla items
- 3 dla produktów (po jednym dla każdego item)
- 3 dla kategorii produktów
- 1 dla adresu dostawy

---

### 4. GET `/api/analytics/product-ratings-report`
**Problem**: Agregacja na dużym zbiorze danych

Pobiera wszystkie produkty i dla każdego oblicza statystyki recenzji, powodując:
- N+1 problem (podobny do #1)
- Kalkulacje w pamięci zamiast w SQL

**Przykład**:
```bash
curl http://localhost:8080/api/analytics/product-ratings-report
```

**Oczekiwane zachowanie**:
Dla 7 produktów: 8 zapytań SQL + przetwarzanie statystyk w pamięci dla każdego produktu.

---

## Dane testowe

Przy starcie aplikacji automatycznie tworzone są:
- 7 produktów (Electronics, Clothing, Books)
- 2 użytkowników (john@example.com, jane@example.com)
- 3-8 recenzji dla każdego produktu (~35-50 recenzji total)
- 2-5 zamówień dla każdego użytkownika (~4-10 zamówień total)
- Każde zamówienie ma 1-4 produkty

## Jak testować

1. Uruchom BuggyShop:
```bash
mvn spring-boot:run
```

2. Wywołaj endpointy (każdy powinien wygenerować slow query detection w ExceptHub):
```bash
# N+1 Problem
curl http://localhost:8080/api/analytics/products-with-reviews

# Unindexed search
curl "http://localhost:8080/api/analytics/search-by-description?keyword=guide"

# Complex unoptimized joins
curl http://localhost:8080/api/analytics/order-details/1

# Aggregation without optimization
curl http://localhost:8080/api/analytics/product-ratings-report
```

3. Sprawdź w ExceptHub dashboard:
   - Przejdź do Slow Queries page
   - Powinieneś zobaczyć wykryte wolne zapytania
   - Każde zapytanie powinno mieć informacje o endpoint, metodzie HTTP, czasie wykonania

## Notatki techniczne

- **Nie używamy `Thread.sleep()`** - wszystkie wolne zapytania są wynikiem prawdziwych problemów wydajnościowych
- Problemy są typowe dla rzeczywistych aplikacji: N+1, brak indeksów, złe JOIN-y
- ExceptHub Spring Boot Starter automatycznie wykryje zapytania > 500ms
- Każde zapytanie zawiera pełny stack trace, endpoint i metodę HTTP
