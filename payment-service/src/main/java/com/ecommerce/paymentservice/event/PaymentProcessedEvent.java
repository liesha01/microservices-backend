package com.ecommerce.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when payment has been successfully processed.
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
