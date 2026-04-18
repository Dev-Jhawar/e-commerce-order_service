package com.example.order_service.dto;

import com.example.order_service.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private Long orderId;
    private Long userId;
    private String userName;

    private List<OrderItemResponse> itemResponse;

    private Double totalAmount;
    private Status status;

}
