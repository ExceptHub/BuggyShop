package com.buggyshop.service;

import com.buggyshop.entity.Inventory;
import com.buggyshop.entity.Product;
import com.buggyshop.exception.InsufficientInventoryException;
import com.buggyshop.exception.ResourceNotFoundException;
import com.buggyshop.repository.InventoryRepository;
import com.buggyshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Cacheable(value = "inventory", key = "#productId")
    public Inventory getInventory(Long productId) {
        log.info("Getting inventory for product: {}", productId);

        return inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
    }

    @Transactional
    public void reserveStock(Long productId, Integer quantity) {
        log.info("Reserving {} units of product {}", quantity, productId);

        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (inventory.getAvailable() < quantity) {
            throw new InsufficientInventoryException(
                    String.format("Insufficient stock. Available: %d, Requested: %d",
                            inventory.getAvailable(), quantity));
        }

        inventory.setReserved(inventory.getReserved() + quantity);
        inventoryRepository.save(inventory);

        log.info("Reserved {} units. New available: {}", quantity, inventory.getAvailable());
    }

    @Transactional
    @CacheEvict(value = "inventory", key = "#productId")
    public void restockProduct(Long productId, Integer quantity) {
        log.info("Restocking product {} with {} units", productId, quantity);

        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventory.setUpdatedAt(LocalDateTime.now());

        inventoryRepository.save(inventory);

        log.info("Restocked. New quantity: {}", inventory.getQuantity());
    }

    @Transactional
    public void confirmReservation(Long productId, Integer quantity) {
        log.info("Confirming reservation for product {}: {} units", productId, quantity);

        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.setReserved(inventory.getReserved() - quantity);

        inventoryRepository.save(inventory);
    }

    @Transactional
    public void releaseReservation(Long productId, Integer quantity) {
        log.info("Releasing reservation for product {}: {} units", productId, quantity);

        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        inventory.setReserved(inventory.getReserved() - quantity);

        inventoryRepository.save(inventory);
    }

    public List<Product> getLowStockProducts() {
        log.info("Getting low stock products");

        List<Inventory> lowStock = inventoryRepository.findLowStock();

        return lowStock.stream()
                .map(inv -> productRepository.findById(inv.getProductId()).orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }
}
