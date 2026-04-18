package com.example.order_service.grpc.client;

import com.example.user_service.grpc.UserRequest;
import com.example.user_service.grpc.UserResponse;
import com.example.user_service.grpc.UserServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class UserGrpcClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    public UserResponse getUserById(Long userId) {

        UserRequest request = UserRequest.newBuilder()
                .setId(userId)
                .build();

        return userStub.getUserById(request);
    }

}