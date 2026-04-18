package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;

import java.util.List;


public interface OrderService {

    List<OrderResponse> getAllOrders();

    OrderResponse getOrderById(Long orderId);

    OrderResponse createOrder(OrderRequest orderRequest);

    OrderResponse updateOrderById(Long orderId, OrderRequest orderRequest);

    void deleteOrderById(Long orderId);

}
