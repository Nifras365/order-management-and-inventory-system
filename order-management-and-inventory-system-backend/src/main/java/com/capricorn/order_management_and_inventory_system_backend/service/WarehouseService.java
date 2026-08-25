package com.capricorn.order_management_and_inventory_system_backend.service;

import com.capricorn.order_management_and_inventory_system_backend.dto.WarehouseRequest;
import com.capricorn.order_management_and_inventory_system_backend.dto.WarehouseResponse;
import com.capricorn.order_management_and_inventory_system_backend.entity.Warehouse;
import com.capricorn.order_management_and_inventory_system_backend.exception.ResourceNotFoundException;
import com.capricorn.order_management_and_inventory_system_backend.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        if (warehouseRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Warehouse with name " + request.getName() + " already exists.");
        }

        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .location(request.getLocation())
                .build();

        return WarehouseResponse.fromEntity(warehouseRepository.save(warehouse));
    }

    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(WarehouseResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public WarehouseResponse getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        return WarehouseResponse.fromEntity(warehouse);
    }
}
