package com.shopmart.order.saga;

import com.shopmart.order.event.OrderEvent;
import com.shopmart.order.event.SagaEventType;
import com.shopmart.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Câu 5: Test kịch bản ROLLBACK của Saga.
 * Khi order-service nhận sự kiện PAYMENT_FAILED -> phải HUỶ đơn (cancelOrder), KHÔNG hoàn tất đơn.
 */
@ExtendWith(MockitoExtension.class)
class OrderSagaListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderSagaListener listener;

    @Test
    void onPaymentFailed_shouldCancelOrder_rollback() {
        OrderEvent event = OrderEvent.builder()
                .orderId(1L)
                .type(SagaEventType.PAYMENT_FAILED)
                .message("Cổng thanh toán lỗi")
                .build();

        listener.onOrderEvent(event);

        verify(orderService).cancelOrder(eq(1L), anyString());  // đơn bị huỷ (rollback)
        verify(orderService, never()).completeOrder(eq(1L));    // KHÔNG được hoàn tất
    }

    @Test
    void onInventoryFailed_shouldCancelOrder() {
        OrderEvent event = OrderEvent.builder()
                .orderId(2L)
                .type(SagaEventType.INVENTORY_FAILED)
                .message("Không đủ tồn kho")
                .build();

        listener.onOrderEvent(event);

        verify(orderService).cancelOrder(eq(2L), anyString());
    }

    @Test
    void onPaymentCompleted_shouldCompleteOrder() {
        OrderEvent event = OrderEvent.builder()
                .orderId(3L)
                .type(SagaEventType.PAYMENT_COMPLETED)
                .build();

        listener.onOrderEvent(event);

        verify(orderService).completeOrder(eq(3L));
        verify(orderService, never()).cancelOrder(eq(3L), anyString());
    }
}
