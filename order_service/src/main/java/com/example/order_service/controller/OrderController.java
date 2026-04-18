package com.example.order_service.controller;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.service.OrderService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping()
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @PostMapping("{orderId}")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        return ResponseEntity.ok(orderService.createOrder(orderRequest));
    }

    @PutMapping("{orderId}")
    public ResponseEntity<?> updateOrderById(@PathVariable Long orderId, @Valid @RequestBody OrderRequest orderRequest) {
        return ResponseEntity.ok(orderService.updateOrderById(orderId, orderRequest));
    }

    @DeleteMapping("{orderId}")
    public ResponseEntity<String> deleteOrderById(@PathVariable Long orderId) {

        orderService.deleteOrderById(orderId);

        return ResponseEntity.ok("Order Deleted Successfully");
    }
}
