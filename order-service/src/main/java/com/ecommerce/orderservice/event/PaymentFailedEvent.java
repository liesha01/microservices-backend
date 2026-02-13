package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event received when payment has failed.
 * Triggers compensating transaction (inventory rollback, order cancellation).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentFailedEvent {
    private String orderNumber;
    private String reason;
}
