package com.ecommerce.inventoryservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event received when payment has been successfully processed.
 * Triggers reservation commitment.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentProcessedEvent {
    private String orderNumber;
    private String paymentId;
    private String transactionId;
}
