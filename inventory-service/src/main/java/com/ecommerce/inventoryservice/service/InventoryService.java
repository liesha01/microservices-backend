package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.entity.Inventory;
import com.ecommerce.inventoryservice.entity.InventoryReservation;
import com.ecommerce.inventoryservice.event.*;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Inventory Service implementing Choreography-based Saga Pattern.
 * 
 * Saga Flow:
 * 1. Listen for OrderPlacedEvent -> Reserve inventory
 * 2. If reservation successful -> Publish InventoryReservedEvent
 * 3. If reservation failed -> Publish InventoryReservationFailedEvent
 * 4. Listen for PaymentFailedEvent -> Rollback inventory (compensating transaction)
 * 5. Listen for PaymentProcessedEvent -> Commit reservation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String INVENTORY_RESERVED_TOPIC = "inventory-reserved-topic";
    private static final String INVENTORY_FAILED_TOPIC = "inventory-reservation-failed-topic";

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCodes) {
        return inventoryRepository.findBySkuCodeIn(skuCodes).stream()
                .map(inventory ->
                        InventoryResponse.builder()
                                .skuCode(inventory.getSkuCode())
                                .isInStock(inventory.getQuantity() > 0)
                                .build()
                ).collect(Collectors.toList());
    }

    /**
     * Handle OrderPlacedEvent from Order Service.
     * Attempts to reserve inventory for all items in the order.
     */
    @KafkaListener(topics = "order-placed-topic", groupId = "inventory-service-group")
    @Transactional
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent for order: {}", event.getOrderNumber());

        List<InventoryReservation> reservations = new ArrayList<>();
        boolean allReserved = true;
        String failureReason = null;

        for (OrderPlacedEvent.OrderItemEvent item : event.getItems()) {
            Optional<Inventory> inventoryOpt = inventoryRepository.findBySkuCode(item.getSkuCode());

            if (inventoryOpt.isEmpty()) {
                allReserved = false;
                failureReason = "Product not found: " + item.getSkuCode();
                log.warn("Inventory not found for SKU: {}", item.getSkuCode());
                break;
            }

            Inventory inventory = inventoryOpt.get();
            if (inventory.getQuantity() < item.getQuantity()) {
                allReserved = false;
                failureReason = "Insufficient stock for product: " + item.getSkuCode() + 
                               ". Available: " + inventory.getQuantity() + 
                               ", Requested: " + item.getQuantity();
                log.warn("Insufficient stock for SKU: {}. Available: {}, Requested: {}", 
                        item.getSkuCode(), inventory.getQuantity(), item.getQuantity());
                break;
            }

            // Reserve the stock
            inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
            inventoryRepository.save(inventory);

            // Record the reservation
            InventoryReservation reservation = InventoryReservation.builder()
                    .orderNumber(event.getOrderNumber())
                    .skuCode(item.getSkuCode())
                    .quantity(item.getQuantity())
                    .status(InventoryReservation.ReservationStatus.RESERVED)
                    .build();
            reservations.add(reservation);

            log.info("Reserved {} units of SKU: {} for order: {}", 
                    item.getQuantity(), item.getSkuCode(), event.getOrderNumber());
        }

        if (allReserved) {
            // Save all reservations
            reservationRepository.saveAll(reservations);

            // Publish InventoryReservedEvent
            InventoryReservedEvent reservedEvent = InventoryReservedEvent.builder()
                    .orderNumber(event.getOrderNumber())
                    .userId(event.getUserId())
                    .totalAmount(event.getTotalAmount())
                    .build();
            kafkaTemplate.send(INVENTORY_RESERVED_TOPIC, reservedEvent);
            log.info("Inventory reserved successfully for order: {}. Published InventoryReservedEvent.", 
                    event.getOrderNumber());
        } else {
            // Rollback any partial reservations
            rollbackReservations(reservations);

            // Publish InventoryReservationFailedEvent
            InventoryReservationFailedEvent failedEvent = InventoryReservationFailedEvent.builder()
                    .orderNumber(event.getOrderNumber())
                    .reason(failureReason)
                    .build();
            kafkaTemplate.send(INVENTORY_FAILED_TOPIC, failedEvent);
            log.info("Inventory reservation failed for order: {}. Published InventoryReservationFailedEvent.", 
                    event.getOrderNumber());
        }
    }

    /**
     * Handle PaymentFailedEvent - Compensating transaction.
     * Restores the reserved inventory.
     */
    @KafkaListener(topics = "payment-failed-topic", groupId = "inventory-service-group")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent for order: {}. Starting compensation.", event.getOrderNumber());

        List<InventoryReservation> reservations = reservationRepository
                .findByOrderNumberAndStatus(event.getOrderNumber(), InventoryReservation.ReservationStatus.RESERVED);

        if (!reservations.isEmpty()) {
            for (InventoryReservation reservation : reservations) {
                Optional<Inventory> inventoryOpt = inventoryRepository.findBySkuCode(reservation.getSkuCode());
                if (inventoryOpt.isPresent()) {
                    Inventory inventory = inventoryOpt.get();
                    inventory.setQuantity(inventory.getQuantity() + reservation.getQuantity());
                    inventoryRepository.save(inventory);
                    
                    reservation.setStatus(InventoryReservation.ReservationStatus.RELEASED);
                    reservationRepository.save(reservation);
                    
                    log.info("Restored {} units of SKU: {} for order: {}", 
                            reservation.getQuantity(), reservation.getSkuCode(), event.getOrderNumber());
                }
            }
            log.info("Inventory rolled back successfully for order: {}", event.getOrderNumber());
        } else {
            log.warn("No reservations found for order: {}. Compensation skipped.", event.getOrderNumber());
        }
    }

    /**
     * Handle PaymentProcessedEvent - Commit the reservation.
     * Marks the reservation as committed (no rollback possible).
     */
    @KafkaListener(topics = "payment-processed-topic", groupId = "inventory-service-group")
    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received PaymentProcessedEvent for order: {}. Committing reservation.", event.getOrderNumber());

        List<InventoryReservation> reservations = reservationRepository
                .findByOrderNumberAndStatus(event.getOrderNumber(), InventoryReservation.ReservationStatus.RESERVED);

        for (InventoryReservation reservation : reservations) {
            reservation.setStatus(InventoryReservation.ReservationStatus.COMMITTED);
            reservationRepository.save(reservation);
        }

        log.info("Reservation committed for order: {}", event.getOrderNumber());
    }

    /**
     * Rollback reservations by restoring quantities.
     * Used for partial rollback during reservation phase.
     */
    private void rollbackReservations(List<InventoryReservation> reservations) {
        for (InventoryReservation reservation : reservations) {
            Optional<Inventory> inventoryOpt = inventoryRepository.findBySkuCode(reservation.getSkuCode());
            if (inventoryOpt.isPresent()) {
                Inventory inventory = inventoryOpt.get();
                inventory.setQuantity(inventory.getQuantity() + reservation.getQuantity());
                inventoryRepository.save(inventory);
                log.info("Rolled back {} units of SKU: {}", reservation.getQuantity(), reservation.getSkuCode());
            }
        }
    }
}
