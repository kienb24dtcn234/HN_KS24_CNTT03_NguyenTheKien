package com.shopmart.payment.saga;

import com.shopmart.payment.dto.PaymentRequest;
import com.shopmart.payment.dto.PaymentResponse;
import com.shopmart.payment.entity.PaymentStatus;
import com.shopmart.payment.event.KafkaTopics;
import com.shopmart.payment.event.OrderEvent;
import com.shopmart.payment.event.SagaEventType;
import com.shopmart.payment.messaging.PaymentEventProducer;
import com.shopmart.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Câu 3 (Choreography Saga): payment-service phản ứng với INVENTORY_RESERVED.
 *
 *  - Sau khi kho đã được trừ, thực hiện thanh toán:
 *      + SUCCESS -> phát PAYMENT_COMPLETED (order-service sẽ hoàn tất đơn)
 *      + FAILED  -> phát PAYMENT_FAILED    (order-service huỷ đơn, inventory-service hoàn kho)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSagaListener {

    private final PaymentService paymentService;
    private final PaymentEventProducer producer;

    @KafkaListener(topics = KafkaTopics.ORDER, groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderEvent(OrderEvent event) {
        if (event.getType() != SagaEventType.INVENTORY_RESERVED) {
            return; // chỉ xử lý khi kho đã được giữ chỗ
        }
        log.info("[payment] Nhận INVENTORY_RESERVED orderId={} -> thanh toán amount={}",
                event.getOrderId(), event.getAmount());

        PaymentResponse result = paymentService.processPayment(
                new PaymentRequest(event.getOrderId(), event.getAmount()));

        SagaEventType resultType = result.getStatus() == PaymentStatus.SUCCESS
                ? SagaEventType.PAYMENT_COMPLETED
                : SagaEventType.PAYMENT_FAILED;

        producer.publish(OrderEvent.builder()
                .orderId(event.getOrderId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .amount(event.getAmount())
                .type(resultType)
                .message(result.getMessage())
                .build());
    }
}
