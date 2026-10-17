package com.wimn.utilities;

import com.wimn.models.InventoryItem;
import com.wimn.models.InventoryTransaction;
import com.wimn.models.NetworkDevice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class CsvExporter {

    public static File exportInventoryToCsv(List<InventoryItem> items, File targetFile) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(targetFile))) {
            pw.println("SKU,Product Name,Category,Quantity,Min Threshold,Unit Price ($),Rack Location,Department Zone,Last Updated");
            for (InventoryItem it : items) {
                pw.printf("\"%s\",\"%s\",\"%s\",%d,%d,%.2f,\"%s\",\"%s\",\"%s\"%n",
                        it.getSku(), it.getName(), it.getCategory(), it.getQuantity(),
                        it.getMinThreshold(), it.getUnitPrice(), it.getRackLocation(),
                        it.getDepartmentZone().getDisplayName(), it.getLastUpdated());
            }
        }
        return targetFile;
    }

    public static File exportTransactionsToCsv(List<InventoryTransaction> transactions, File targetFile) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(targetFile))) {
            pw.println("Transaction ID,Type,SKU,Product Name,Quantity,Party / Destination,Network Packet ID,Department,Timestamp,Status");
            for (InventoryTransaction tx : transactions) {
                pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        tx.getTransactionId(), tx.getType().name(), tx.getSku(), tx.getProductName(),
                        tx.getQuantity(), tx.getPartyInfo(), tx.getNetworkPacketId(),
                        tx.getDepartment(), tx.getTimestamp(), tx.getStatus());
            }
        }
        return targetFile;
    }

    public static File exportDevicesToCsv(List<NetworkDevice> devices, File targetFile) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(targetFile))) {
            pw.println("Device ID,Name,Device Type,Department,IPv4 Address,MAC Address,Default Gateway,VLAN,Connection Type,Status,Connected To");
            for (NetworkDevice d : devices) {
                pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\"%n",
                        d.getId(), d.getName(), d.getDeviceType().name(), d.getDepartment().name(),
                        d.getIpv4(), d.getMacAddress(), d.getDefaultGateway(), d.getVlanId(),
                        d.getConnectionType(), d.isOnline() ? "ONLINE" : "OFFLINE",
                        d.getConnectedSwitchId() != null ? d.getConnectedSwitchId() : "NONE");
            }
        }
        return targetFile;
    }
}
