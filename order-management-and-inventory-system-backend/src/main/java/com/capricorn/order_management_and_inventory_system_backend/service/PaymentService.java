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

import java.util.Optional;
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
    public PaymentResponse processPayment(String idempotencyKey, PaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        // 1. Idempotency Check
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            log.info("Idempotent request detected for key {}. Returning existing payment.", idempotencyKey);
            return PaymentResponse.fromEntity(existingPayment.get());
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (paymentRepository.findByOrderId(order.getId()).isPresent()) {
            throw new IllegalArgumentException("A payment attempt has already been made for this order with a different idempotency key");
        }

        if (order.getTotalAmount().compareTo(request.getAmount()) != 0) {
            throw new IllegalArgumentException("Payment amount does not match the order total amount");
        }

        // Mock payment processing logic: 85% SUCCESS, 10% FAILED, 5% TIMEOUT
        int randomValue = random.nextInt(100);
        PaymentStatus status;
        if (randomValue < 10) {
            status = PaymentStatus.FAILED;
        } else if (randomValue < 15) {
            status = PaymentStatus.TIMEOUT;
        } else {
            status = PaymentStatus.SUCCESS;
        }
        
        Payment payment = Payment.builder()
                .order(order)
                .amount(request.getAmount())
                .idempotencyKey(idempotencyKey)
                .status(status)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        if (status == PaymentStatus.SUCCESS) {
            log.info("Payment successful for order {}", order.getId());
            orderService.processSuccessfulPayment(order.getId());
        } else if (status == PaymentStatus.FAILED) {
            log.warn("Payment failed for order {}", order.getId());
            orderService.processFailedPayment(order.getId());
        } else {
            log.warn("Payment timed out for order {}", order.getId());
            // In a real system, TIMEOUT might require a manual sync/reconciliation later
        }

        return PaymentResponse.fromEntity(savedPayment);
    }
    
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));
        return PaymentResponse.fromEntity(payment);
    }
}
