package com.capricorn.order_management_and_inventory_system_backend.service;

import com.capricorn.order_management_and_inventory_system_backend.dto.PaymentRequest;
import com.capricorn.order_management_and_inventory_system_backend.dto.PaymentResponse;
import com.capricorn.order_management_and_inventory_system_backend.entity.Order;
import com.capricorn.order_management_and_inventory_system_backend.entity.Payment;
import com.capricorn.order_management_and_inventory_system_backend.enums.PaymentStatus;
import com.capricorn.order_management_and_inventory_system_backend.exception.ResourceNotFoundException;
import com.capricorn.order_management_and_inventory_system_backend.repository.OrderRepository;
import com.capricorn.order_management_and_inventory_system_backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final Random random = new Random();

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (paymentRepository.findByOrderId(order.getId()).isPresent()) {
            throw new IllegalArgumentException("A payment attempt has already been made for this order");
        }

        if (order.getTotalAmount().compareTo(request.getAmount()) != 0) {
            throw new IllegalArgumentException("Payment amount does not match the order total amount");
        }

        // Mock payment processing logic with a ~10% failure rate
        boolean isSuccess = random.nextInt(100) >= 10;
        
        Payment payment = Payment.builder()
                .order(order)
                .amount(request.getAmount())
                .idempotencyKey(UUID.randomUUID().toString())
                .status(isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        if (isSuccess) {
            log.info("Payment successful for order {}", order.getId());
            orderService.processSuccessfulPayment(order.getId());
        } else {
            log.warn("Payment failed for order {}", order.getId());
            orderService.processFailedPayment(order.getId());
        }

        return PaymentResponse.fromEntity(savedPayment);
    }
    
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));
        return PaymentResponse.fromEntity(payment);
    }
}
