package com.ecommerce.inventoryservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event published when inventory has been successfully reserved.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservedEvent {
    private String orderNumber;
    private Long userId;
    private BigDecimal totalAmount;
}
