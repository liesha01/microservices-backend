package com.ecommerce.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event received when inventory reservation has failed.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservationFailedEvent {
    private String orderNumber;
    private String reason;
}
