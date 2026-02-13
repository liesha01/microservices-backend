package com.ecommerce.inventoryservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when inventory reservation has failed.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservationFailedEvent {
    private String orderNumber;
    private String reason;
}
