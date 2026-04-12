package com.example.order_service.serviceImpl;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderItem;
import com.example.order_service.repo.OrderRepo;
import com.example.order_service.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepo repo;

    @Override
    public Order createOrder(OrderRequest orderRequest) {

        List<OrderItem> items = orderRequest.getItems().stream()
                .map(i -> new OrderItem(null, i.getProductId(), i.getQuantity()))
                .collect(Collectors.toList());

        Order order = new Order();
        order.setUserId(orderRequest.getUserId());
        order.setItems(items);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        return repo.save(order);
    }
}
