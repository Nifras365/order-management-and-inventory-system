package com.capricorn.order_management_and_inventory_system_backend.service;

import com.capricorn.order_management_and_inventory_system_backend.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void testReserveInventoryAtomic_Success() {
        // Arrange
        when(inventoryRepository.reserveInventoryAtomic(anyLong(), anyLong(), anyInt())).thenReturn(1);

        // Act
        boolean result = inventoryService.reserveInventoryAtomic(1L, 1L, 5);

        // Assert
        assertTrue(result);
        verify(inventoryRepository, times(1)).reserveInventoryAtomic(1L, 1L, 5);
    }

    @Test
    void testReserveInventoryAtomic_InsufficientStock() {
        // Arrange
        when(inventoryRepository.reserveInventoryAtomic(anyLong(), anyLong(), anyInt())).thenReturn(0);

        // Act
        boolean result = inventoryService.reserveInventoryAtomic(1L, 1L, 500);

        // Assert
        assertFalse(result);
        verify(inventoryRepository, times(1)).reserveInventoryAtomic(1L, 1L, 500);
    }

    @Test
    void testReserveInventoryAtomic_NegativeQuantityThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.reserveInventoryAtomic(1L, 1L, -5);
        });

        assertEquals("Reservation quantity must be strictly greater than 0", exception.getMessage());
        verify(inventoryRepository, never()).reserveInventoryAtomic(anyLong(), anyLong(), anyInt());
    }
}
