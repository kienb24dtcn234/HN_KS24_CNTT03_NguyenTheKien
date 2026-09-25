package com.shopmart.payment.messaging;

import com.shopmart.payment.event.KafkaTopics;
import com.shopmart.payment.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Câu 3: Producer phát các sự kiện thanh toán (PAYMENT_COMPLETED / PAYMENT_FAILED) lên topic "order".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publish(OrderEvent event) {
        log.info("[payment] PUBLISH {} orderId={}", event.getType(), event.getOrderId());
        kafkaTemplate.send(KafkaTopics.ORDER, String.valueOf(event.getOrderId()), event);
    }
}
