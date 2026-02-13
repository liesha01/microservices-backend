package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.OrderRequest;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody OrderRequest orderRequest) {
        String orderNumber = orderService.placeOrder(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "status", "PENDING",
            "orderNumber", orderNumber,
            "message", "Order placed successfully. Saga processing started."
        ));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable String orderNumber) {

        return orderService.getOrderByOrderNumber(orderNumber)
                .map(order -> {

                    Map<String, Object> response = new HashMap<>();
                    response.put("orderNumber", order.getOrderNumber());
                    response.put("status", order.getStatus());
                    response.put("userId", order.getUserId());
                    response.put("itemCount", order.getOrderLineItems().size());

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "order-service"
        ));
    }
}
