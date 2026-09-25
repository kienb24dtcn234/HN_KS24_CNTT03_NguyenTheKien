package com.shopmart.order.saga;

import com.shopmart.order.event.KafkaTopics;
import com.shopmart.order.event.OrderEvent;
import com.shopmart.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Câu 3 (Choreography Saga): order-service lắng nghe kết quả của các bước sau.
 *
 *  - PAYMENT_COMPLETED  : Saga thành công  -> đơn COMPLETED.
 *  - PAYMENT_FAILED     : thanh toán lỗi   -> đơn CANCELLED (inventory-service sẽ tự hoàn kho).
 *  - INVENTORY_FAILED   : hết hàng ngay bước đầu -> đơn CANCELLED (chưa trừ kho nên không cần hoàn).
 *  - INVENTORY_RELEASED : xác nhận đã hoàn kho xong (chỉ log để theo dõi).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaListener {

    private final OrderService orderService;

    @KafkaListener(topics = KafkaTopics.ORDER, groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderEvent(OrderEvent event) {
        if (event.getType() == null) {
            return;
        }
        switch (event.getType()) {
            case PAYMENT_COMPLETED -> {
                log.info("[order] Nhận PAYMENT_COMPLETED orderId={} -> hoàn tất đơn", event.getOrderId());
                orderService.completeOrder(event.getOrderId());
            }
            case PAYMENT_FAILED -> {
                log.error("[order] Nhận PAYMENT_FAILED orderId={} -> HUỶ đơn (rollback)", event.getOrderId());
                orderService.cancelOrder(event.getOrderId(), "Thanh toán thất bại: " + event.getMessage());
            }
            case INVENTORY_FAILED -> {
                log.error("[order] Nhận INVENTORY_FAILED orderId={} -> HUỶ đơn", event.getOrderId());
                orderService.cancelOrder(event.getOrderId(), "Không đủ tồn kho: " + event.getMessage());
            }
            case INVENTORY_RELEASED ->
                    log.info("[order] Nhận INVENTORY_RELEASED orderId={} -> đã hoàn kho, Saga rollback hoàn tất",
                            event.getOrderId());
            default -> { /* ORDER_CREATED, INVENTORY_RESERVED do service khác xử lý */ }
        }
    }
}
