package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment Controller for manual payment processing.
 * 
 * Note: In the Saga pattern, payments are typically triggered by events.
 * This REST endpoint is provided for testing and backward compatibility.
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Manually trigger payment processing for an order.
     * This bypasses the saga flow and is mainly for testing.
     */
    @PostMapping("/{orderNumber}")
    public ResponseEntity<Map<String, Object>> processPayment(
            @PathVariable String orderNumber,
            @RequestParam(defaultValue = "100.00") BigDecimal amount) {
        
        log.info("Manual payment request for order: {} with amount: {}", orderNumber, amount);
        
        PaymentService.PaymentResult result = paymentService.processPaymentManually(orderNumber, amount);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "orderNumber", orderNumber,
                "paymentId", result.paymentId(),
                "transactionId", result.transactionId(),
                "message", "Payment processed successfully"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "orderNumber", orderNumber,
                "reason", result.failureReason(),
                "message", "Payment processing failed"
            ));
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "payment-service"
        ));
    }
}
