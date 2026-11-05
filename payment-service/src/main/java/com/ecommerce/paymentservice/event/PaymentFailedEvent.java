package com.ecommerce.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder; 
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when payment has failed.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentFailedEvent {
    private String orderNumber;
    private String reason;
}
