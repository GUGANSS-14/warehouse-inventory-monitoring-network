package com.wimn.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InventoryTransaction {
    public enum TransactionType {
        RECEIVING("Inbound Receiving"),
        DISPATCH("Outbound Dispatch"),
        TRANSFER("Internal Rack Transfer"),
        AUDIT_ADJUSTMENT("Inventory Audit");

        private final String title;
        TransactionType(String title) { this.title = title; }
        public String getTitle() { return title; }
    }

    private String transactionId;
    private TransactionType type;
    private String sku;
    private String productName;
    private int quantity;
    private String partyInfo; // Supplier name or Destination customer
    private String timestamp;
    private String networkPacketId;
    private String department;
    private String status;

    public InventoryTransaction(String transactionId, TransactionType type, String sku,
                                String productName, int quantity, String partyInfo,
                                String networkPacketId, String department, String status) {
        this.transactionId = transactionId;
        this.type = type;
        this.sku = sku;
        this.productName = productName;
        this.quantity = quantity;
        this.partyInfo = partyInfo;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.networkPacketId = networkPacketId;
        this.department = department;
        this.status = status;
    }

    public String getTransactionId() { return transactionId; }
    public TransactionType getType() { return type; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public String getPartyInfo() { return partyInfo; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getNetworkPacketId() { return networkPacketId; }
    public String getDepartment() { return department; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
