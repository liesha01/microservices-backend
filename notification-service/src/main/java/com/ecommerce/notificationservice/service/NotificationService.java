package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Notification Service implementing event-driven notifications.
 * 
 * This is a SIMULATED notification service for demonstration purposes.
 * In production, this would integrate with actual notification providers:
 * - Email: SendGrid, AWS SES, Mailgun
 * - SMS: Twilio, AWS SNS
 * - Push Notifications: Firebase, OneSignal
 * 
 * Listens to various saga events and sends appropriate notifications.
 */
@Service
@Slf4j
public class NotificationService {

    /**
     * Handle OrderPlacedEvent - Send order confirmation notification.
     */
    @KafkaListener(topics = "order-placed-topic", groupId = "notification-service-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("========================================");
        log.info("[NOTIFICATION] Order Placed");
        log.info("========================================");
        log.info("Order Number: {}", event.getOrderNumber());
        log.info("User ID: {}", event.getUserId());
        log.info("Total Amount: {}", event.getTotalAmount());
        log.info("Items: {}", event.getItems().size());
        log.info("----------------------------------------");
        
        // Simulate sending email notification
        sendEmail(
            event.getUserId(),
            "Order Received - " + event.getOrderNumber(),
            String.format("""
                Dear Customer,
                
                Thank you for your order!
                
                Order Number: %s
                Total Amount: $%s
                
                We are processing your order and will notify you once it's confirmed.
                
                Best regards,
                E-Commerce Team
                """, event.getOrderNumber(), event.getTotalAmount())
        );
    }

    /**
     * Handle PaymentProcessedEvent - Send payment confirmation notification.
     */
    @KafkaListener(topics = "payment-processed-topic", groupId = "notification-service-group")
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("========================================");
        log.info("[NOTIFICATION] Payment Processed");
        log.info("========================================");
        log.info("Order Number: {}", event.getOrderNumber());
        log.info("Payment ID: {}", event.getPaymentId());
        log.info("Transaction ID: {}", event.getTransactionId());
        log.info("----------------------------------------");
        
        // Simulate sending email notification
        sendEmail(
            null, // In real scenario, we'd fetch user details
            "Payment Confirmed - " + event.getOrderNumber(),
            String.format("""
                Dear Customer,
                
                Great news! Your payment has been processed successfully.
                
                Order Number: %s
                Payment ID: %s
                Transaction ID: %s
                
                Your order is now confirmed and will be shipped soon.
                
                Best regards,
                E-Commerce Team
                """, event.getOrderNumber(), event.getPaymentId(), event.getTransactionId())
        );
        
        // Simulate sending SMS notification
        sendSms(null, "Your payment for order " + event.getOrderNumber() + " is confirmed. Thank you!");
    }

    /**
     * Handle PaymentFailedEvent - Send payment failure notification.
     */
    @KafkaListener(topics = "payment-failed-topic", groupId = "notification-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("========================================");
        log.info("[NOTIFICATION] Payment Failed");
        log.info("========================================");
        log.info("Order Number: {}", event.getOrderNumber());
        log.info("Reason: {}", event.getReason());
        log.info("----------------------------------------");
        
        // Simulate sending email notification
        sendEmail(
            null,
            "Payment Failed - " + event.getOrderNumber(),
            String.format("""
                Dear Customer,
                
                We're sorry, but your payment could not be processed.
                
                Order Number: %s
                Reason: %s
                
                Please try again or use a different payment method.
                Your reserved items have been released.
                
                If you need assistance, please contact our support team.
                
                Best regards,
                E-Commerce Team
                """, event.getOrderNumber(), event.getReason())
        );
    }

    /**
     * Handle InventoryReservationFailedEvent - Send out of stock notification.
     */
    @KafkaListener(topics = "inventory-reservation-failed-topic", groupId = "notification-service-group")
    public void handleInventoryReservationFailed(InventoryReservationFailedEvent event) {
        log.info("========================================");
        log.info("[NOTIFICATION] Inventory Reservation Failed");
        log.info("========================================");
        log.info("Order Number: {}", event.getOrderNumber());
        log.info("Reason: {}", event.getReason());
        log.info("----------------------------------------");
        
        // Simulate sending email notification
        sendEmail(
            null,
            "Order Cancelled - Item Unavailable - " + event.getOrderNumber(),
            String.format("""
                Dear Customer,
                
                We're sorry, but we couldn't complete your order due to inventory issues.
                
                Order Number: %s
                Reason: %s
                
                Please try again or choose an alternative product.
                
                We apologize for the inconvenience.
                
                Best regards,
                E-Commerce Team
                """, event.getOrderNumber(), event.getReason())
        );
    }

    /**
     * Simulate sending an email.
     * In production, this would integrate with SendGrid, AWS SES, etc.
     */
    private void sendEmail(Long userId, String subject, String body) {
        log.info("[SIMULATION] Sending EMAIL to user: {}", userId);
        log.info("[SIMULATION] Subject: {}", subject);
        log.info("[SIMULATION] Body: \n{}", body);
        log.info("[SIMULATION] Email sent successfully!");
    }

    /**
     * Simulate sending an SMS.
     * In production, this would integrate with Twilio, AWS SNS, etc.
     */
    private void sendSms(Long userId, String message) {
        log.info("[SIMULATION] Sending SMS to user: {}", userId);
        log.info("[SIMULATION] Message: {}", message);
        log.info("[SIMULATION] SMS sent successfully!");
    }
}
