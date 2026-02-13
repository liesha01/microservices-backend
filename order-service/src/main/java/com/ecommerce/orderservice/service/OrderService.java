package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderLineItemsDto;
import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.event.*;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order Service implementing Choreography-based Saga Pattern.
 * 
 * Saga Flow:
 * 1. Order Created (Status: PENDING) -> Publish OrderPlacedEvent
 * 2. Listen for InventoryReservedEvent -> (handled by PaymentService)
 * 3. Listen for PaymentProcessedEvent -> Update status to CONFIRMED
 * 4. Listen for PaymentFailedEvent -> Update status to CANCELLED
 * 5. Listen for InventoryReservationFailedEvent -> Update status to CANCELLED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_TOPIC = "order-placed-topic";

    /**
     * Place a new order and start the saga.
     * The order is created with PENDING status.
     * An OrderPlacedEvent is published to start the saga flow.
     */
    @Transactional
    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setUserId(orderRequest.getUserId());
        order.setStatus("PENDING");

        List<OrderItem> orderLineItems = orderRequest.getOrderLineItems()
                .stream()
                .map(this::mapToEntity)
                .collect(Collectors.toList());

        order.setOrderLineItems(orderLineItems);
        orderRepository.save(order);

        // Calculate total amount
        BigDecimal totalAmount = orderLineItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build order items for the event
        List<OrderPlacedEvent.OrderItemEvent> eventItems = orderLineItems.stream()
                .map(item -> OrderPlacedEvent.OrderItemEvent.builder()
                        .skuCode(item.getSkuCode())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        // Publish OrderPlacedEvent to start the saga
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .items(eventItems)
                .totalAmount(totalAmount)
                .build();

        kafkaTemplate.send(ORDER_TOPIC, event);
        log.info("Order placed successfully. OrderNumber: {}. Saga started.", order.getOrderNumber());

        return order.getOrderNumber();
    }

    /**
     * Handle PaymentProcessedEvent - Final step of successful saga.
     * Updates order status to CONFIRMED.
     */
    @KafkaListener(topics = "payment-processed-topic", groupId = "order-service-group")
    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received PaymentProcessedEvent for order: {}", event.getOrderNumber());
        
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(event.getOrderNumber());
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus("CONFIRMED");
            orderRepository.save(order);
            log.info("Order {} confirmed. Saga completed successfully.", event.getOrderNumber());
        } else {
            log.error("Order not found for orderNumber: {}", event.getOrderNumber());
        }
    }

    /**
     * Handle PaymentFailedEvent - Compensating transaction.
     * Updates order status to CANCELLED.
     */
    @KafkaListener(topics = "payment-failed-topic", groupId = "order-service-group")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent for order: {}. Reason: {}", event.getOrderNumber(), event.getReason());
        
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(event.getOrderNumber());
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            log.info("Order {} cancelled due to payment failure.", event.getOrderNumber());
        } else {
            log.error("Order not found for orderNumber: {}", event.getOrderNumber());
        }
    }

    /**
     * Handle InventoryReservationFailedEvent - Compensating transaction.
     * Updates order status to CANCELLED.
     */
    @KafkaListener(topics = "inventory-reservation-failed-topic", groupId = "order-service-group")
    @Transactional
    public void handleInventoryReservationFailed(InventoryReservationFailedEvent event) {
        log.info("Received InventoryReservationFailedEvent for order: {}. Reason: {}", event.getOrderNumber(), event.getReason());
        
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(event.getOrderNumber());
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            log.info("Order {} cancelled due to inventory reservation failure.", event.getOrderNumber());
        } else {
            log.error("Order not found for orderNumber: {}", event.getOrderNumber());
        }
    }

    /**
     * Get order by order number.
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    private OrderItem mapToEntity(OrderLineItemsDto dto) {
        return OrderItem.builder()
                .price(dto.getPrice())
                .quantity(dto.getQuantity())
                .skuCode(dto.getSkuCode())
                .build();
    }
}
