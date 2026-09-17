package com.kanhacollection.backend.payment;

import com.kanhacollection.backend.exception.BadRequestException;
import com.kanhacollection.backend.exception.ResourceNotFoundException;
import com.kanhacollection.backend.inventory.InventoryService;
import com.kanhacollection.backend.order.Order;
import com.kanhacollection.backend.order.OrderRepository;
import com.kanhacollection.backend.order.OrderStatus;
import com.kanhacollection.backend.order.PaymentStatus;
import com.kanhacollection.backend.payment.dto.RazorpayOrderResponse;
import com.kanhacollection.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID userId;
    private UUID orderId;
    private Order sampleOrder;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_12345");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "secret_12345");

        sampleUser = User.builder()
                .id(userId)
                .fullName("Test User")
                .email("test@example.com")
                .phone("9876543210")
                .build();

        sampleOrder = Order.builder()
                .id(orderId)
                .orderNumber("ORD-123456")
                .user(sampleUser)
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .netAmount(BigDecimal.valueOf(1499.00))
                .build();
    }

    @Test
    @DisplayName("Should return existing payment order response if payment already initialized (Idempotency)")
    void createRazorpayOrder_ExistingPayment_ReturnsResponse() {
        Payment existingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .order(sampleOrder)
                .razorpayOrderId("order_rzp_existing_999")
                .amount(sampleOrder.getNetAmount())
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(sampleOrder));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingPayment));

        RazorpayOrderResponse response = paymentService.createRazorpayOrder(userId, orderId);

        assertThat(response).isNotNull();
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_rzp_existing_999");
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(1499.00));
    }

    @Test
    @DisplayName("Should throw BadRequestException if order is not in PENDING_PAYMENT state")
    void createRazorpayOrder_InvalidOrderStatus_ThrowsException() {
        sampleOrder.setOrderStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> paymentService.createRazorpayOrder(userId, orderId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not in PENDING_PAYMENT state");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when order is not found for user")
    void createRazorpayOrder_OrderNotFound_ThrowsException() {
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createRazorpayOrder(userId, orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
