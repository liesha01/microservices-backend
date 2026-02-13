package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Payment Service implementing Choreography-based Saga Pattern.
 * 
 * This is a SIMULATED payment service for demonstration purposes.
 * In production, this would integrate with actual payment providers (Stripe, PayPal, etc.)
 * 
 * Saga Flow:
 * 1. Listen for InventoryReservedEvent -> Process payment
 * 2. If payment successful -> Publish PaymentProcessedEvent
 * 3. If payment failed -> Publish PaymentFailedEvent (triggers compensation)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    private static final String PAYMENT_PROCESSED_TOPIC = "payment-processed-topic";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed-topic";

    /**
     * Configurable success rate for payment simulation.
     * Set to 1.0 for always success (demo mode).
     * Set to 0.8 for 80% success rate (realistic simulation).
     */
    @Value("${payment.simulation.success-rate:0.9}")
    private double successRate;

    /**
     * Simulate payment processing delay in milliseconds.
     */
    @Value("${payment.simulation.delay-ms:1000}")
    private long processingDelayMs;

    /**
     * Handle InventoryReservedEvent from Inventory Service.
     * Simulates payment processing and publishes result.
     */
    @KafkaListener(topics = "inventory-reserved-topic", groupId = "payment-service-group")
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReservedEvent for order: {}. Processing payment for amount: {}", 
                event.getOrderNumber(), event.getTotalAmount());

        try {
            // Simulate payment processing delay
            Thread.sleep(processingDelayMs);

            // Simulate payment with external provider
            PaymentResult result = simulatePaymentProcessing(event.getOrderNumber(), event.getTotalAmount());

            if (result.isSuccess()) {
                // Payment successful
                PaymentProcessedEvent successEvent = PaymentProcessedEvent.builder()
                        .orderNumber(event.getOrderNumber())
                        .paymentId(result.paymentId())
                        .transactionId(result.transactionId())
                        .build();
                kafkaTemplate.send(PAYMENT_PROCESSED_TOPIC, successEvent);
                log.info("Payment processed successfully for order: {}. PaymentId: {}, TransactionId: {}", 
                        event.getOrderNumber(), result.paymentId(), result.transactionId());
            } else {
                // Payment failed
                PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                        .orderNumber(event.getOrderNumber())
                        .reason(result.failureReason())
                        .build();
                kafkaTemplate.send(PAYMENT_FAILED_TOPIC, failedEvent);
                log.info("Payment failed for order: {}. Reason: {}", 
                        event.getOrderNumber(), result.failureReason());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted for order: {}", event.getOrderNumber());
            
            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .orderNumber(event.getOrderNumber())
                    .reason("Payment processing was interrupted")
                    .build();
            kafkaTemplate.send(PAYMENT_FAILED_TOPIC, failedEvent);
        }
    }

    /**
     * Simulate payment processing with an external payment provider.
     * 
     * In production, this method would:
     * - Call Stripe API, PayPal API, or other payment gateway
     * - Handle card validation, fraud detection, etc.
     * - Store transaction details in a database
     * 
     * For simulation, we randomly succeed/fail based on configured success rate.
     */
    private PaymentResult simulatePaymentProcessing(String orderNumber, BigDecimal amount) {
        log.info("[SIMULATION] Processing payment for order: {} with amount: {}", orderNumber, amount);

        // Simulate external payment gateway call
        boolean isSuccess = random.nextDouble() < successRate;

        if (isSuccess) {
            String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String transactionId = "TXN-" + System.currentTimeMillis();
            
            log.info("[SIMULATION] Payment gateway returned SUCCESS for order: {}", orderNumber);
            return new PaymentResult(true, paymentId, transactionId, null);
        } else {
            // Simulate various failure reasons
            String[] failureReasons = {
                "Insufficient funds",
                "Card declined",
                "Payment gateway timeout",
                "Fraud detection triggered",
                "Invalid card details"
            };
            String failureReason = failureReasons[random.nextInt(failureReasons.length)];
            
            log.info("[SIMULATION] Payment gateway returned FAILURE for order: {}. Reason: {}", 
                    orderNumber, failureReason);
            return new PaymentResult(false, null, null, failureReason);
        }
    }

    /**
     * Manual payment processing endpoint result.
     * Used for backward compatibility with REST endpoint.
     */
    public PaymentResult processPaymentManually(String orderNumber, BigDecimal amount) {
        try {
            Thread.sleep(processingDelayMs);
            return simulatePaymentProcessing(orderNumber, amount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new PaymentResult(false, null, null, "Payment processing was interrupted");
        }
    }

    /**
     * Record to hold payment processing result.
     */
    public record PaymentResult(boolean isSuccess, String paymentId, String transactionId, String failureReason) {}
}
