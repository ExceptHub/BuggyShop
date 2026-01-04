package com.buggyshop.controller;

import com.buggyshop.entity.Inventory;
import com.buggyshop.entity.Product;
import com.buggyshop.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> getInventory(@PathVariable Long productId) {
        log.info("GET /api/inventory/{}", productId);
        Inventory inventory = inventoryService.getInventory(productId);
        return ResponseEntity.ok(inventory);
    }

    @PutMapping("/{productId}/reserve")
    public ResponseEntity<Void> reserveStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        log.info("PUT /api/inventory/{}/reserve - quantity={}", productId, quantity);
        inventoryService.reserveStock(productId, quantity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{productId}/restock")
    public ResponseEntity<Void> restockProduct(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        log.info("POST /api/inventory/{}/restock - quantity={}", productId, quantity);
        inventoryService.restockProduct(productId, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts() {
        log.info("GET /api/inventory/low-stock");
        List<Product> products = inventoryService.getLowStockProducts();
        return ResponseEntity.ok(products);
    }
}
