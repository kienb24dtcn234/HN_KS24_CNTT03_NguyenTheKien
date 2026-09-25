package com.shopmart.inventory.messaging;

import com.shopmart.inventory.event.KafkaTopics;
import com.shopmart.inventory.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Câu 3: Producer phát các sự kiện Saga của inventory-service lên topic "order".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publish(OrderEvent event) {
        log.info("[inventory] PUBLISH {} orderId={}", event.getType(), event.getOrderId());
        kafkaTemplate.send(KafkaTopics.ORDER, String.valueOf(event.getOrderId()), event);
    }
}
