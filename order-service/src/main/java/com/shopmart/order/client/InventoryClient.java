package com.shopmart.order.client;

import com.shopmart.order.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Câu 2: Gọi inventory-service THEO TÊN (không hard-code host/port).
 * Spring Cloud LoadBalancer sẽ phân giải "inventory-service" thành 1 instance đang đăng ký trong Eureka;
 * nếu chạy nhiều instance -> tự động luân phiên (round-robin) để minh chứng Load Balancing.
 */
@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/inventory/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long id);
}
