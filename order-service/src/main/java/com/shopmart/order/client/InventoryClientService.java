package com.shopmart.order.client;

import com.shopmart.order.dto.ProductDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Câu 2: Bọc lời gọi Feign bằng Resilience4j @CircuitBreaker.
 *
 * 3 trạng thái của Circuit Breaker (instance "inventory-service" cấu hình trong order-service.yml):
 *   - CLOSED    : mạch đóng, mọi request đi thẳng tới inventory-service. Nếu tỉ lệ lỗi >= 50%
 *                 trong cửa sổ 10 request gần nhất -> chuyển OPEN.
 *   - OPEN      : mạch mở, KHÔNG gọi service đích nữa mà trả về fallback ngay (fail-fast),
 *                 tránh lỗi dây chuyền (cascading failure). Sau 10s -> chuyển HALF_OPEN.
 *   - HALF_OPEN : cho phép một số request thăm dò; nếu OK -> CLOSED, nếu vẫn lỗi -> OPEN lại.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryClientService {

    private final InventoryClient inventoryClient;

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "getProductFallback")
    public ProductDto getProduct(Long id) {
        log.info("[order] Gọi inventory-service lấy thông tin product id={}", id);
        return inventoryClient.getProduct(id);
    }

    /** Fallback khi inventory-service lỗi/không phản hồi (chữ ký thêm Throwable ở cuối). */
    private ProductDto getProductFallback(Long id, Throwable t) {
        log.error("[order] FALLBACK Circuit Breaker cho product id={} (lý do: {})", id, t.toString());
        return ProductDto.builder()
                .id(id)
                .name("FALLBACK - inventory-service tạm thời không khả dụng")
                .price(null)   // price = null báo hiệu không lấy được dữ liệu thật
                .stock(0)
                .build();
    }
}
