package com.wimn.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InventoryItem {
    private String sku;
    private String name;
    private String category;
    private int quantity;
    private int minThreshold;
    private double unitPrice;
    private String rackLocation;
    private DepartmentType departmentZone;
    private String lastUpdated;

    public InventoryItem() {}

    public InventoryItem(String sku, String name, String category, int quantity,
                         int minThreshold, double unitPrice, String rackLocation,
                         DepartmentType departmentZone) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.minThreshold = minThreshold;
        this.unitPrice = unitPrice;
        this.rackLocation = rackLocation;
        this.departmentZone = departmentZone;
        this.lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    public int getMinThreshold() { return minThreshold; }
    public void setMinThreshold(int minThreshold) { this.minThreshold = minThreshold; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public String getRackLocation() { return rackLocation; }
    public void setRackLocation(String rackLocation) { this.rackLocation = rackLocation; }
    public DepartmentType getDepartmentZone() { return departmentZone; }
    public void setDepartmentZone(DepartmentType departmentZone) { this.departmentZone = departmentZone; }
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public boolean isLowStock() {
        return quantity <= minThreshold;
    }
}
