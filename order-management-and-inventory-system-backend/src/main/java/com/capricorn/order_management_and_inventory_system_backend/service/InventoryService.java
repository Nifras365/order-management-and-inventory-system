package com.capricorn.order_management_and_inventory_system_backend.service;

import com.capricorn.order_management_and_inventory_system_backend.dto.InventoryRequest;
import com.capricorn.order_management_and_inventory_system_backend.dto.InventoryResponse;
import com.capricorn.order_management_and_inventory_system_backend.entity.Inventory;
import com.capricorn.order_management_and_inventory_system_backend.entity.Product;
import com.capricorn.order_management_and_inventory_system_backend.entity.Warehouse;
import com.capricorn.order_management_and_inventory_system_backend.exception.ResourceNotFoundException;
import com.capricorn.order_management_and_inventory_system_backend.repository.InventoryRepository;
import com.capricorn.order_management_and_inventory_system_backend.repository.ProductRepository;
import com.capricorn.order_management_and_inventory_system_backend.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional
    public InventoryResponse addOrUpdateInventory(InventoryRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        Optional<Inventory> existingOpt = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId());

        Inventory inventory;
        if (existingOpt.isPresent()) {
            inventory = existingOpt.get();
            // Using optimistic locking here for standard updates by warehouse managers
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + request.getAvailableQuantity());
        } else {
            inventory = Inventory.builder()
                    .product(product)
                    .warehouse(warehouse)
                    .availableQuantity(request.getAvailableQuantity())
                    .reservedQuantity(0)
                    .build();
        }

        return InventoryResponse.fromEntity(inventoryRepository.save(inventory));
    }

    public List<InventoryResponse> getInventoryByProduct(Long productId) {
        return inventoryRepository.findByProductId(productId).stream()
                .map(InventoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InventoryResponse> getInventoryByWarehouse(Long warehouseId) {
        return inventoryRepository.findByWarehouseId(warehouseId).stream()
                .map(InventoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean reserveInventoryAtomic(Long productId, Long warehouseId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be strictly greater than 0");
        }
        // Highly concurrent atomic reservation
        int updatedRows = inventoryRepository.reserveInventoryAtomic(productId, warehouseId, quantity);
        return updatedRows > 0;
    }
}
