package com.shopmart.order.messaging;

import com.shopmart.order.event.KafkaTopics;
import com.shopmart.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Câu 3: Producer phát sự kiện khởi động Saga (ORDER_CREATED) lên topic "order".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publish(OrderEvent event) {
        log.info("[order] PUBLISH {} orderId={}", event.getType(), event.getOrderId());
        kafkaTemplate.send(KafkaTopics.ORDER, String.valueOf(event.getOrderId()), event);
    }
}
