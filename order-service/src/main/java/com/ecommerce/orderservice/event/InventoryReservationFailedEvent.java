package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event received when inventory reservation has failed.
 * Triggers order cancellation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservationFailedEvent {
    private String orderNumber;
    private String reason;
}
