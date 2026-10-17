package com.wimn.devices;

import com.wimn.models.DepartmentType;
import com.wimn.models.DeviceType;
import com.wimn.models.NetworkDevice;

public class ScannerDevice extends NetworkDevice {
    private String scannerTechnology; // "Wi-Fi 6", "Bluetooth 5.2", "RFID 865MHz"
    private int batteryPercentage;
    private String lastScannedCode;
    private long totalScansCount;

    public ScannerDevice(String id, String name, DepartmentType department, String ip,
                         String mac, String gateway, int vlanId, String connectedAPId,
                         String scannerTechnology, double x, double y, double z) {
        super(id, name, scannerTechnology.contains("RFID") ? DeviceType.RFID_READER : DeviceType.BARCODE_SCANNER,
                department, ip, "2001:db8:1::" + Math.abs(id.hashCode() % 250),
                mac, "255.255.255.0", gateway, vlanId, scannerTechnology,
                connectedAPId, x, y, z);
        this.scannerTechnology = scannerTechnology;
        this.batteryPercentage = 88;
        this.lastScannedCode = "SKU-AUTO-001";
        this.totalScansCount = 312;
    }

    public String getScannerTechnology() { return scannerTechnology; }
    public int getBatteryPercentage() { return batteryPercentage; }
    public void setBatteryPercentage(int batteryPercentage) { this.batteryPercentage = batteryPercentage; }

    public String getLastScannedCode() { return lastScannedCode; }
    public void recordScan(String code) {
        this.lastScannedCode = code;
        this.totalScansCount++;
        touchCommunication();
    }

    public long getTotalScansCount() { return totalScansCount; }
}
