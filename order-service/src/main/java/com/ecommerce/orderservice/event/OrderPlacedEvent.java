package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Event published when an order is placed.
 * Contains all information needed for inventory reservation and payment processing.
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
