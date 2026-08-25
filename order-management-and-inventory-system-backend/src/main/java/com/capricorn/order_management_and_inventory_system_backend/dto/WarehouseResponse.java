package com.capricorn.order_management_and_inventory_system_backend.dto;

import com.capricorn.order_management_and_inventory_system_backend.entity.Warehouse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseResponse {
    private Long id;
    private String name;
    private String location;

    public static WarehouseResponse fromEntity(Warehouse warehouse) {
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .location(warehouse.getLocation())
                .build();
    }
}
