package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event received when payment has been successfully processed.
 * Triggers order confirmation.
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
