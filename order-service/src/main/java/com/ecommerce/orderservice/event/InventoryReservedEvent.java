package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event received when inventory has been successfully reserved.
 * Triggers payment processing.
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
