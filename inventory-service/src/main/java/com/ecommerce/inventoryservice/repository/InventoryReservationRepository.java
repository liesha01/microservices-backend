package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    List<InventoryReservation> findByOrderNumberAndStatus(String orderNumber, InventoryReservation.ReservationStatus status);
    List<InventoryReservation> findByOrderNumber(String orderNumber);
}
