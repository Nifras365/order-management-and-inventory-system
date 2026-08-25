package com.capricorn.order_management_and_inventory_system_backend.repository;

import com.capricorn.order_management_and_inventory_system_backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
