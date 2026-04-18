package com.example.order_service.grpc.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import com.example.product_service.grpc.ProductRequest;
import com.example.product_service.grpc.ProductServiceGrpc;

@Service
public class ProductGrpcClient{

    @GrpcClient("product-service")
    private ProductServiceGrpc.ProductServiceBlockingStub productStub;

    public com.example.product_service.grpc.ProductResponse getProduct(Long productId) {

        ProductRequest request = ProductRequest.newBuilder()
                .setProductId(productId)
                .build();

        return productStub.getProductById(request);
    }
}
