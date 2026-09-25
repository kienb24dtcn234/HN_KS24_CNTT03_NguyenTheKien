package com.shopmart.inventory.saga;

import com.shopmart.inventory.event.OrderEvent;
import com.shopmart.inventory.event.KafkaTopics;
import com.shopmart.inventory.event.SagaEventType;
import com.shopmart.inventory.exception.InsufficientStockException;
import com.shopmart.inventory.messaging.InventoryEventProducer;
import com.shopmart.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Câu 3 (Choreography Saga): inventory-service phản ứng với 2 sự kiện trên topic "order".
 *
 *  - ORDER_CREATED  : thử TRỪ tồn kho -> phát INVENTORY_RESERVED (ok) hoặc INVENTORY_FAILED (thiếu hàng).
 *  - PAYMENT_FAILED : bước bù trừ (compensating) -> HOÀN tồn kho -> phát INVENTORY_RELEASED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventorySagaListener {

    private final ProductService productService;
    private final InventoryEventProducer producer;

    @KafkaListener(topics = KafkaTopics.ORDER, groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderEvent(OrderEvent event) {
        if (event.getType() == null) {
            return;
        }
        switch (event.getType()) {
            case ORDER_CREATED -> reserveStock(event);
            case PAYMENT_FAILED -> releaseStock(event);
            default -> { /* Các sự kiện khác không thuộc trách nhiệm của inventory-service */ }
        }
    }

    private void reserveStock(OrderEvent event) {
        log.info("[inventory] Nhận ORDER_CREATED orderId={} -> trừ tồn kho productId={} qty={}",
                event.getOrderId(), event.getProductId(), event.getQuantity());
        try {
            productService.decreaseStock(event.getProductId(), event.getQuantity());
            producer.publish(OrderEvent.builder()
                    .orderId(event.getOrderId())
                    .productId(event.getProductId())
                    .quantity(event.getQuantity())
                    .amount(event.getAmount())
                    .type(SagaEventType.INVENTORY_RESERVED)
                    .message("Đã trừ tồn kho thành công")
                    .build());
        } catch (InsufficientStockException ex) {
            log.error("[inventory] Trừ tồn kho THẤT BẠI orderId={}: {}", event.getOrderId(), ex.getMessage());
            producer.publish(OrderEvent.builder()
                    .orderId(event.getOrderId())
                    .productId(event.getProductId())
                    .quantity(event.getQuantity())
                    .amount(event.getAmount())
                    .type(SagaEventType.INVENTORY_FAILED)
                    .message(ex.getMessage())
                    .build());
        }
    }

    private void releaseStock(OrderEvent event) {
        log.warn("[inventory] Nhận PAYMENT_FAILED orderId={} -> HOÀN tồn kho (compensating) productId={} qty={}",
                event.getOrderId(), event.getProductId(), event.getQuantity());
        productService.increaseStock(event.getProductId(), event.getQuantity());
        producer.publish(OrderEvent.builder()
                .orderId(event.getOrderId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .amount(event.getAmount())
                .type(SagaEventType.INVENTORY_RELEASED)
                .message("Đã hoàn tồn kho sau khi thanh toán lỗi")
                .build());
    }
}
