package com.wimn.inventory;

import com.wimn.models.DepartmentType;
import com.wimn.models.InventoryItem;
import com.wimn.models.InventoryTransaction;
import com.wimn.models.NetworkEvent;
import com.wimn.repositories.EventRepository;
import com.wimn.repositories.InventoryRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class InventoryService {
    private final InventoryRepository inventoryRepo;
    private final EventRepository eventRepo;

    public InventoryService(InventoryRepository inventoryRepo, EventRepository eventRepo) {
        this.inventoryRepo = inventoryRepo;
        this.eventRepo = eventRepo;
    }

    public List<InventoryItem> getAllItems() {
        return inventoryRepo.getAllItems();
    }

    public List<InventoryItem> getLowStockItems() {
        return inventoryRepo.getAllItems().stream()
                .filter(InventoryItem::isLowStock)
                .collect(Collectors.toList());
    }

    public List<InventoryTransaction> getTransactions() {
        return inventoryRepo.getAllTransactions();
    }

    public record OperationResult(boolean success, String message, String transactionId, String packetId) {}

    public OperationResult receiveProduct(String sku, String name, String category, int quantity,
                                          double unitPrice, String rackLocation, String supplier) {
        if (quantity <= 0) {
            return new OperationResult(false, "Quantity received must be greater than zero.", null, null);
        }
        if (sku == null || sku.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            return new OperationResult(false, "SKU and Product Name cannot be empty.", null, null);
        }

        String txId = "TX-RCV-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
        String packetId = "PKT-RCV-" + UUID.randomUUID().toString().substring(0, 8);

        InventoryItem existing = inventoryRepo.getItemBySku(sku);
        InventoryItem itemToSave;
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            if (rackLocation != null && !rackLocation.trim().isEmpty()) {
                existing.setRackLocation(rackLocation);
            }
            itemToSave = existing;
        } else {
            itemToSave = new InventoryItem(sku, name, category, quantity, 10, unitPrice,
                    rackLocation.isEmpty() ? "Rack-A1-01" : rackLocation, DepartmentType.STORAGE);
        }

        InventoryTransaction tx = new InventoryTransaction(
                txId, InventoryTransaction.TransactionType.RECEIVING, sku, name, quantity,
                supplier, packetId, "Receiving Dept", "COMPLETED"
        );

        boolean saved = inventoryRepo.adjustStock(sku, quantity, tx);
        if (!saved && existing == null) {
            // New item insert
            inventoryRepo.saveOrUpdateItem(itemToSave);
            saved = true;
        }

        if (saved) {
            eventRepo.addEvent(new NetworkEvent(
                    "EVT-" + txId, NetworkEvent.Severity.SUCCESS, "INVENTORY",
                    "Receiving Dept",
                    "Received " + quantity + " units of " + name + " (" + sku + ") from " + supplier + ". Transmitted to SRV-MAIN-01 via " + packetId
            ));
            eventRepo.addEvent(new NetworkEvent(
                    "EVT-NOTIF-" + txId, NetworkEvent.Severity.INFO, "NETWORK",
                    "Storage Dept",
                    "Notification received: Stock for " + sku + " ready for put-away at " + itemToSave.getRackLocation()
            ));
            return new OperationResult(true, "Successfully received " + quantity + " units of " + name, txId, packetId);
        } else {
            return new OperationResult(false, "Database transaction failed during stock intake.", null, null);
        }
    }

    public OperationResult dispatchProduct(String sku, int quantity, String customerDestination) {
        if (quantity <= 0) {
            return new OperationResult(false, "Dispatch quantity must be greater than zero.", null, null);
        }
        InventoryItem item = inventoryRepo.getItemBySku(sku);
        if (item == null) {
            return new OperationResult(false, "Item SKU '" + sku + "' not found in warehouse.", null, null);
        }
        if (item.getQuantity() < quantity) {
            String msg = "Dispatch denied: Insufficient stock. Requested: " + quantity + ", Available: " + item.getQuantity();
            eventRepo.addEvent(new NetworkEvent(
                    "EVT-DSP-FAIL-" + System.currentTimeMillis() % 10000,
                    NetworkEvent.Severity.ALERT, "INVENTORY", "Dispatch Dept", msg
            ));
            return new OperationResult(false, msg, null, null);
        }

        String txId = "TX-DSP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
        String packetId = "PKT-DSP-" + UUID.randomUUID().toString().substring(0, 8);

        InventoryTransaction tx = new InventoryTransaction(
                txId, InventoryTransaction.TransactionType.DISPATCH, sku, item.getName(),
                quantity, customerDestination, packetId, "Dispatch Dept", "COMPLETED"
        );

        boolean adjusted = inventoryRepo.adjustStock(sku, -quantity, tx);
        if (adjusted) {
            eventRepo.addEvent(new NetworkEvent(
                    "EVT-" + txId, NetworkEvent.Severity.SUCCESS, "INVENTORY", "Dispatch Dept",
                    "Dispatched " + quantity + " units of " + item.getName() + " to " + customerDestination + ". Packet " + packetId + " routed to Storage & Server."
            ));

            // Check if stock now below threshold
            InventoryItem updated = inventoryRepo.getItemBySku(sku);
            if (updated != null && updated.isLowStock()) {
                eventRepo.addEvent(new NetworkEvent(
                        "EVT-LOW-" + sku, NetworkEvent.Severity.WARNING, "INVENTORY", "Storage Dept",
                        "LOW STOCK ALERT: " + updated.getName() + " (" + sku + ") remaining: " + updated.getQuantity() + " (Threshold: " + updated.getMinThreshold() + ")"
                ));
            }

            return new OperationResult(true, "Successfully dispatched " + quantity + " units to " + customerDestination, txId, packetId);
        } else {
            return new OperationResult(false, "Database update failed during dispatch.", null, null);
        }
    }

    public Map<String, Object> getConsolidatedWarehouseSummary() {
        List<InventoryItem> items = inventoryRepo.getAllItems();
        int totalItemsCount = items.stream().mapToInt(InventoryItem::getQuantity).sum();
        double totalInventoryValue = items.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum();
        long lowStockCount = items.stream().filter(InventoryItem::isLowStock).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalUniqueSkus", items.size());
        summary.put("totalStockUnits", totalItemsCount);
        summary.put("totalValuationUsd", totalInventoryValue);
        summary.put("lowStockAlertsCount", lowStockCount);
        return summary;
    }
}
