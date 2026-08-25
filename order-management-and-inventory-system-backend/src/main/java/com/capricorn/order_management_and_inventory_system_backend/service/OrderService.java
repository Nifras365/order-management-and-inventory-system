package com.capricorn.order_management_and_inventory_system_backend.service;

import com.capricorn.order_management_and_inventory_system_backend.dto.OrderRequest;
import com.capricorn.order_management_and_inventory_system_backend.dto.OrderRequestItem;
import com.capricorn.order_management_and_inventory_system_backend.dto.OrderResponse;
import com.capricorn.order_management_and_inventory_system_backend.entity.Order;
import com.capricorn.order_management_and_inventory_system_backend.entity.OrderItem;
import com.capricorn.order_management_and_inventory_system_backend.entity.OrderStatusHistory;
import com.capricorn.order_management_and_inventory_system_backend.entity.Product;
import com.capricorn.order_management_and_inventory_system_backend.entity.User;
import com.capricorn.order_management_and_inventory_system_backend.enums.OrderStatus;
import com.capricorn.order_management_and_inventory_system_backend.enums.PaymentStatus;
import com.capricorn.order_management_and_inventory_system_backend.exception.InsufficientInventoryException;
import com.capricorn.order_management_and_inventory_system_backend.exception.ResourceNotFoundException;
import com.capricorn.order_management_and_inventory_system_backend.repository.OrderRepository;
import com.capricorn.order_management_and_inventory_system_backend.repository.OrderStatusHistoryRepository;
import com.capricorn.order_management_and_inventory_system_backend.repository.ProductRepository;
import com.capricorn.order_management_and_inventory_system_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OutboxService outboxService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User user = getCurrentUser();

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderRequestItem itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            boolean reserved = inventoryService.reserveInventoryAtomic(
                    product.getId(), itemRequest.getWarehouseId(), itemRequest.getQuantity());

            if (!reserved) {
                throw new InsufficientInventoryException("Insufficient inventory for product " + product.getSku());
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .warehouseId(itemRequest.getWarehouseId())
                    .price(product.getPrice())
                    .build();

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        recordStatusHistory(savedOrder, OrderStatus.PENDING);

        OrderResponse response = OrderResponse.fromEntity(savedOrder);
        outboxService.saveEvent("ORDER", savedOrder.getId().toString(), "ORDER_CREATED", response);

        return response;
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(getCurrentUser().getId()) && 
            !SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new IllegalArgumentException("Cannot cancel another user's order");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalArgumentException("Order cannot be cancelled in its current state: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        Order savedOrder = orderRepository.save(order);
        recordStatusHistory(savedOrder, OrderStatus.CANCELLED);

        // Release inventory back to the respective warehouses
        for (OrderItem item : savedOrder.getOrderItems()) {
            inventoryService.releaseInventoryAtomic(
                    item.getProduct().getId(),
                    item.getWarehouseId(),
                    item.getQuantity()
            );
        }

        OrderResponse response = OrderResponse.fromEntity(savedOrder);
        outboxService.saveEvent("ORDER", savedOrder.getId().toString(), "ORDER_CANCELLED", response);

        return response;
    }

    public List<OrderResponse> getOrders() {
        User user = getCurrentUser();
        return orderRepository.findByUserId(user.getId()).stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(getCurrentUser().getId()) &&
            !SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new IllegalArgumentException("Cannot view another user's order");
        }

        return OrderResponse.fromEntity(order);
    }

    private void recordStatusHistory(Order order, OrderStatus status) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(status)
                .build();
        orderStatusHistoryRepository.save(history);
    }

    @Transactional
    public void processSuccessfulPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Payment cannot be processed for order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        Order savedOrder = orderRepository.save(order);
        recordStatusHistory(savedOrder, OrderStatus.CONFIRMED);

        outboxService.saveEvent("ORDER", savedOrder.getId().toString(), "ORDER_CONFIRMED", OrderResponse.fromEntity(savedOrder));
    }

    @Transactional
    public void processFailedPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Payment failure cannot be recorded for order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        Order savedOrder = orderRepository.save(order);
        recordStatusHistory(savedOrder, OrderStatus.CANCELLED);

        // Release inventory since the order is cancelled due to payment failure
        for (OrderItem item : savedOrder.getOrderItems()) {
            inventoryService.releaseInventoryAtomic(
                    item.getProduct().getId(),
                    item.getWarehouseId(),
                    item.getQuantity()
            );
        }
        
        outboxService.saveEvent("ORDER", savedOrder.getId().toString(), "ORDER_CANCELLED", OrderResponse.fromEntity(savedOrder));
    }
}
