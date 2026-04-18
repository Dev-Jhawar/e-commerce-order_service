package com.example.order_service.serviceImpl;

import com.example.order_service.dto.OrderItemResponse;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.dto.UserDetails;
import com.example.order_service.enums.Status;
import com.example.order_service.exception.InsufficientStockException;
import com.example.order_service.exception.OrderNotFoundException;
import com.example.order_service.exception.ProductNotFoundException;
import com.example.order_service.exception.UserNotFoundException;
import com.example.order_service.grpc.client.ProductGrpcClient;
import com.example.order_service.grpc.client.UserGrpcClient;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderItem;
import com.example.order_service.repo.OrderRepo;
import com.example.order_service.service.OrderService;
import com.example.product_service.grpc.ProductResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepo repo;
    private final UserGrpcClient userGrpcClient;
    private final ProductGrpcClient productGrpcClient;
    private final ExecutorService executorService;

    @Override
    public List<OrderResponse> getAllOrders() {

        List<Order> allOrders = repo.findAll();

        if (allOrders.isEmpty()) {
            return List.of();
        }

        List<CompletableFuture<OrderResponse>> futures = allOrders.stream()
                .map(order -> CompletableFuture.supplyAsync(() -> {

                    var user = userGrpcClient.getUserById(order.getUserId());

                    UserDetails userDetails = new UserDetails(
                            user.getId(),
                            user.getFirstName() + " " + user.getLastName()
                    );

                    var productMap = fetchProducts(order);

                    return buildOrderResponse(order, userDetails, productMap);

                }, executorService))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {

        Order order = repo.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not Found with ID : " + orderId));

        var user = userGrpcClient.getUserById(order.getUserId());

        UserDetails userDetails = new UserDetails(
                user.getId(),
                user.getFirstName() + " " + user.getLastName()
        );

        var productMap = fetchProducts(order);

        return buildOrderResponse(order, userDetails, productMap);
    }

    @Override
    public OrderResponse createOrder(OrderRequest orderRequest) {

        var user = userGrpcClient.getUserById(orderRequest.getUserId());
        if (user.getId() == 0) {
            throw new UserNotFoundException("User not found with ID : " + orderRequest.getUserId());
        }

        var productMap = fetchProductsFromRequest(orderRequest);

        double totalAmount = calculateTotal(orderRequest, productMap);

        Order order = new Order();
        order.setUserId(orderRequest.getUserId());
        order.setItems(buildOrderItems(orderRequest));
        order.setTotalAmount(totalAmount);
        order.setStatus(Status.Created);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        repo.save(order);

        UserDetails userDetails = new UserDetails(user.getId(), user.getFirstName() + " " + user.getLastName());

        return buildOrderResponse(order, userDetails, productMap);
    }

    @Override
    public OrderResponse updateOrderById(Long orderId, OrderRequest orderRequest) {

        Order order = repo.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found with ID: " + orderId)
                );

        if (order.getStatus() == Status.Delivered) {
            throw new RuntimeException("Delivered order cannot be updated.");
        }

        var user = userGrpcClient.getUserById(orderRequest.getUserId());

        if (user.getId() == 0) {
            throw new UserNotFoundException("User not found with ID : " + order.getUserId());
        }

        var productMap = fetchProductsFromRequest(orderRequest);

        double totalAmount = calculateTotal(orderRequest, productMap);

        order.setUserId(orderRequest.getUserId());
        order.setItems(buildOrderItems(orderRequest));
        order.setTotalAmount(totalAmount);
        order.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = repo.save(order);

        UserDetails userDetails = new UserDetails(user.getId(), user.getFirstName() + " " + user.getLastName());

        return buildOrderResponse(updatedOrder, userDetails, productMap);
    }

    public void deleteOrderById(Long orderId) {

        Order order = repo.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found with ID: " + orderId)
                );

        if (order.getStatus() == Status.Delivered) {
            throw new RuntimeException("Delivered order cannot be deleted");
        }

        repo.delete(order);
    }

    private Map<Long, ProductResponse> fetchProductsFromRequest(OrderRequest request) {

        List<CompletableFuture<ProductResponse>> futures = request.getItems().stream()
                .map(item -> CompletableFuture.supplyAsync(() ->
                        productGrpcClient.getProduct(item.getProductId()), executorService))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(ProductResponse::getId, p -> p));
    }

    private Map<Long, ProductResponse> fetchProducts(Order order) {

        List<CompletableFuture<ProductResponse>> futures = order.getItems().stream()
                .map(item -> CompletableFuture.supplyAsync(() ->
                        productGrpcClient.getProduct(item.getProductId()), executorService))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(ProductResponse::getId, p -> p));
    }

    public List<OrderItem> buildOrderItems(OrderRequest orderRequest) {
        return orderRequest.getItems().stream()
                .map(i -> new OrderItem(null, i.getProductId(), i.getQuantity()))
                .toList();
    }

    private double calculateTotal(OrderRequest request, Map<Long, ProductResponse> productMap) {

        return request.getItems().stream()
                .mapToDouble(item -> {

                    var product = productMap.get(item.getProductId());

                    if (product == null) {
                        throw new ProductNotFoundException("Product not found: " + item.getProductId());
                    }

                    if (product.getStock() < item.getQuantity()) {
                        throw new InsufficientStockException(
                                "Insufficient stock: " + product.getName()
                        );
                    }

                    return product.getPrice() * item.getQuantity();
                })
                .sum();
    }

    private OrderResponse buildOrderResponse(
            Order order,
            UserDetails userDetails,
            Map<Long, ProductResponse> productMap
    ) {

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {

                    var product = productMap.get(item.getProductId());

                    return OrderItemResponse.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .price(product.getPrice())
                            .quantity(item.getQuantity())
                            .build();
                })
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(userDetails.userId())
                .userName(userDetails.userName())
                .itemResponse(itemResponses)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .build();
    }
}
