package com.buggyshop.service;

import com.buggyshop.dto.ProductRequest;
import com.buggyshop.dto.ProductResponse;
import com.buggyshop.entity.Category;
import com.buggyshop.entity.Inventory;
import com.buggyshop.entity.Product;
import com.buggyshop.exception.ResourceNotFoundException;
import com.buggyshop.repository.CategoryRepository;
import com.buggyshop.repository.InventoryRepository;
import com.buggyshop.repository.ProductRepository;
import com.buggyshop.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product: {}", request.getName());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .sku(request.getSku())
                .category(category)
                .build();

        product = productRepository.save(product);

        if (request.getInitialStock() != null && request.getInitialStock() > 0) {
            Inventory inventory = Inventory.builder()
                    .product(product)
                    .productId(product.getId())
                    .quantity(request.getInitialStock())
                    .reserved(0)
                    .build();
            inventoryRepository.save(inventory);
        }

        return mapToResponse(product);
    }

    public Page<ProductResponse> getProducts(int page, int size, String sortBy) {
        log.info("Getting products: page={}, size={}, sortBy={}", page, size, sortBy);

        // Validate sort field
        List<String> validSortFields = List.of("name", "price", "createdAt", "id");
        if (!validSortFields.contains(sortBy)) {
            throw new IllegalArgumentException(
                    String.format("Invalid sort field: '%s'. Allowed fields: %s",
                            sortBy, String.join(", ", validSortFields)));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        Page<Product> products = productRepository.findAll(pageable);

        return products.map(this::mapToResponse);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSku(request.getSku());
        product.setCategory(category);

        product = productRepository.save(product);

        return mapToResponse(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        productRepository.delete(product);
    }

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProduct(Long id) {
        log.info("Getting product from cache: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return mapToResponse(product);
    }

    public void addToBundle(Long productId, Long bundledProductId) {
        log.info("Adding product {} to bundle {}", bundledProductId, productId);

        // Prevent circular dependency
        if (productId.equals(bundledProductId)) {
            throw new IllegalArgumentException(
                    "Cannot add product to its own bundle. Circular dependency detected.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Product bundledProduct = productRepository.findById(bundledProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Bundled product not found"));

        if (product.getBundledProducts() == null) {
            product.setBundledProducts(new ArrayList<>());
        }

        product.getBundledProducts().add(bundledProduct);

        productRepository.save(product);
    }

    private ProductResponse mapToResponse(Product product) {
        Integer availableStock = null;
        if (product.getInventory() != null) {
            availableStock = product.getInventory().getAvailable();
        }

        Double avgRating = reviewRepository.getAverageRating(product.getId());
        Integer reviewCount = product.getReviews() != null ? product.getReviews().size() : 0;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .sku(product.getSku())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .availableStock(availableStock)
                .averageRating(avgRating)
                .reviewCount(reviewCount)
                .build();
    }
}
