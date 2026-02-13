package com.ecommerce.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderNumber;
    private Long userId;

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItem> orderLineItems;
    
    private String status; // CREATED, PAID, SHIPPED, DELIVERED, CANCELLED
}
