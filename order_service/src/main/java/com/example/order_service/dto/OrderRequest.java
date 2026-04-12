package com.example.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {

    @NotNull(message = "User ID is required.")
    private Long userId;

    @NotEmpty(message = "Order must contain atleast one order.")
    @Valid
    private List<OrderItemRequest> items;

}
