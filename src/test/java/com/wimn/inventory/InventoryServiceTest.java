package com.wimn.inventory;

import com.wimn.models.InventoryItem;
import com.wimn.repositories.EventRepository;
import com.wimn.repositories.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryServiceTest {
    private InventoryService inventoryService;
    private InventoryRepository inventoryRepo;

    @BeforeEach
    void setUp() {
        EventRepository eventRepo = new EventRepository();
        inventoryRepo = new InventoryRepository();
        inventoryService = new InventoryService(inventoryRepo, eventRepo);
    }

    @Test
    @DisplayName("Test Receiving Product increases inventory stock")
    void testReceiveProductIncreasesStock() {
        InventoryItem before = inventoryRepo.getItemBySku("SKU-SRV-01");
        int initialQty = before != null ? before.getQuantity() : 0;

        InventoryService.OperationResult res = inventoryService.receiveProduct(
                "SKU-SRV-01", "Cisco Catalyst 9300 Switch 48P", "Networking Hardware",
                10, 2450.0, "Rack-A1-01", "Cisco Direct");

        assertTrue(res.success(), "Receiving transaction should succeed");
        InventoryItem after = inventoryRepo.getItemBySku("SKU-SRV-01");
        assertNotNull(after);
        assertEquals(initialQty + 10, after.getQuantity());
    }

    @Test
    @DisplayName("Test Dispatching greater than available stock is rejected")
    void testDispatchOverStockRejected() {
        InventoryItem item = inventoryRepo.getItemBySku("SKU-SRV-02");
        assertNotNull(item);

        int excessiveQty = item.getQuantity() + 999;
        InventoryService.OperationResult res = inventoryService.dispatchProduct(
                "SKU-SRV-02", excessiveQty, "Acme Logistics");

        assertFalse(res.success(), "Dispatch exceeding available stock must be rejected");
        assertTrue(res.message().contains("Insufficient stock"));
    }
}
