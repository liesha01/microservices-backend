package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;

    /**
     * Check if items are in stock.
     * Example: GET /api/inventory?skuCode=iphone_15&skuCode=macbook_pro
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStock(@RequestParam List<String> skuCode) {
        return inventoryService.isInStock(skuCode);
    }

    /**
     * Get all inventory items.
     */
    @GetMapping("/all")
    public ResponseEntity<List<Inventory>> getAllInventory() {
        return ResponseEntity.ok(inventoryRepository.findAll());
    }

    /**
     * Add new inventory item or update existing.
     * This is a helper endpoint for testing purposes.
     */
    @PostMapping
    public ResponseEntity<Inventory> addInventory(@RequestBody Inventory inventory) {
        Inventory saved = inventoryRepository.save(inventory);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Update inventory quantity.
     */
    @PutMapping("/{skuCode}")
    public ResponseEntity<Map<String, Object>> updateInventory(
            @PathVariable String skuCode,
            @RequestParam Integer quantity) {
        return inventoryRepository.findBySkuCode(skuCode)
                .map(inventory -> {
                    inventory.setQuantity(quantity);
                    inventoryRepository.save(inventory);

                    Map<String, Object> response = Map.of(
                            "message", "Inventory updated",
                            "skuCode", skuCode,
                            "quantity", quantity
                    );

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "inventory-service"
        ));
    }
}
