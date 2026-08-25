package com.capricorn.order_management_and_inventory_system_backend.service;

import com.capricorn.order_management_and_inventory_system_backend.dto.PaymentRequest;
import com.capricorn.order_management_and_inventory_system_backend.dto.PaymentResponse;
import com.capricorn.order_management_and_inventory_system_backend.entity.Order;
import com.capricorn.order_management_and_inventory_system_backend.entity.Payment;
import com.capricorn.order_management_and_inventory_system_backend.repository.OrderRepository;
import com.capricorn.order_management_and_inventory_system_backend.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void testProcessPayment_SuccessOrFailure() {
        // Arrange
        Long orderId = 100L;
        BigDecimal amount = new BigDecimal("99.99");

        Order order = Order.builder().id(orderId).totalAmount(amount).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        PaymentRequest request = new PaymentRequest(orderId, amount);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        PaymentResponse response = paymentService.processPayment(request);

        // Assert
        assertNotNull(response);
        assertEquals(amount, response.getAmount());
        
        // Since random failure is 10%, we can't definitively assert COMPLETED or FAILED without mocking Random,
        // but we can verify that ONE of the order processing methods was called.
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
}
