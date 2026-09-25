package com.shopmart.order.controller;

import com.shopmart.order.client.InventoryClientService;
import com.shopmart.order.dto.OrderRequest;
import com.shopmart.order.dto.OrderResponse;
import com.shopmart.order.dto.ProductDto;
import com.shopmart.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final InventoryClientService inventoryClientService; // Câu 2

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping
    public List<OrderResponse> getAll() {
        return orderService.getAllOrders();
    }

    /**
     * Câu 2: Endpoint minh chứng Load Balancing + Circuit Breaker.
     * Gọi liên tục để thấy request được luân phiên giữa các instance inventory-service;
     * tắt inventory-service để thấy Circuit Breaker mở và trả về fallback.
     */
    @GetMapping("/products/{id}")
    public ProductDto getProductViaFeign(@PathVariable Long id) {
        return inventoryClientService.getProduct(id);
    }
}
