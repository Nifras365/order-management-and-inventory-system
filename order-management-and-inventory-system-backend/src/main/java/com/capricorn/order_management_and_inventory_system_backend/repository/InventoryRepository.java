package com.capricorn.order_management_and_inventory_system_backend.repository;

import com.capricorn.order_management_and_inventory_system_backend.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    List<Inventory> findByProductId(Long productId);

    List<Inventory> findByWarehouseId(Long warehouseId);

    // Atomic Database Update strategy for highly concurrent inventory reservation
    @Modifying
    @Query("UPDATE Inventory i SET i.availableQuantity = i.availableQuantity - :quantity, i.reservedQuantity = i.reservedQuantity + :quantity WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId AND i.availableQuantity >= :quantity AND :quantity > 0")
    int reserveInventoryAtomic(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId, @Param("quantity") int quantity);
}
