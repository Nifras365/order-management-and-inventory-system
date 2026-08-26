package com.capricorn.order_management_and_inventory_system_backend.service;

import com.capricorn.order_management_and_inventory_system_backend.entity.Inventory;
import com.capricorn.order_management_and_inventory_system_backend.entity.Product;
import com.capricorn.order_management_and_inventory_system_backend.entity.Warehouse;
import com.capricorn.order_management_and_inventory_system_backend.entity.Category;
import com.capricorn.order_management_and_inventory_system_backend.repository.InventoryRepository;
import com.capricorn.order_management_and_inventory_system_backend.repository.ProductRepository;
import com.capricorn.order_management_and_inventory_system_backend.repository.WarehouseRepository;
import com.capricorn.order_management_and_inventory_system_backend.repository.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
public class InventoryConcurrencyIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    private Long productId;
    private Long warehouseId;

    @BeforeEach
    void setup() {
        Category category = Category.builder()
                .name("Concurrency Category")
                .description("Test Category")
                .build();
        category = categoryRepository.save(category);

        Product product = Product.builder()
                .sku("TEST-CONCURRENCY-SKU")
                .name("Concurrency Product")
                .price(new BigDecimal("100.00"))
                .status("ACTIVE")
                .category(category)
                .build();
        product = productRepository.save(product);
        productId = product.getId();

        Warehouse warehouse = Warehouse.builder()
                .name("Concurrency Warehouse")
                .location("Test Location")
                .build();
        warehouse = warehouseRepository.save(warehouse);
        warehouseId = warehouse.getId();

        Inventory inventory = Inventory.builder()
                .product(product)
                .warehouse(warehouse)
                .availableQuantity(10) // Exactly 10 units available
                .reservedQuantity(0)
                .build();
        inventoryRepository.save(inventory);
    }

    @AfterEach
    void tearDown() {
        inventoryRepository.deleteAll();
        warehouseRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testConcurrentInventoryReservation() throws InterruptedException {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger failedReservations = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    // Try to reserve exactly 1 unit
                    boolean success = inventoryService.reserveInventoryAtomic(productId, warehouseId, 1);
                    if (success) {
                        successfulReservations.incrementAndGet();
                    } else {
                        failedReservations.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedReservations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to finish
        latch.await();
        executorService.shutdown();

        // Verification
        Inventory finalInventory = inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId).orElseThrow();

        System.out.println("Successful Reservations: " + successfulReservations.get());
        System.out.println("Failed Reservations: " + failedReservations.get());
        System.out.println("Final Available Quantity: " + finalInventory.getAvailableQuantity());
        System.out.println("Final Reserved Quantity: " + finalInventory.getReservedQuantity());

        // We expect exactly 10 successes and 90 failures
        assertEquals(10, successfulReservations.get(), "Exactly 10 reservations should succeed");
        assertEquals(90, failedReservations.get(), "Exactly 90 reservations should fail");

        // The inventory must never drop below 0
        assertTrue(finalInventory.getAvailableQuantity() >= 0, "Available quantity should never be negative");
        assertEquals(0, finalInventory.getAvailableQuantity(), "Available quantity should be exactly 0 after 10 reservations");
        assertEquals(10, finalInventory.getReservedQuantity(), "Reserved quantity should be exactly 10");
    }
}
