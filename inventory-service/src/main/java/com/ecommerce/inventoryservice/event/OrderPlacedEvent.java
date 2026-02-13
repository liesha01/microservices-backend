package com.ecommerce.inventoryservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Event received when an order is placed.
 * Triggers inventory reservation check.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {
    private String orderNumber;
    private Long userId;
    private List<OrderItemEvent> items;
    private BigDecimal totalAmount;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemEvent {
        private String skuCode;
        private Integer quantity;
        private BigDecimal price;
    }
}
