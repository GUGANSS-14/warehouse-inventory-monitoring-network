package com.wimn.devices;

import com.wimn.models.DepartmentType;
import com.wimn.models.DeviceType;
import com.wimn.models.NetworkDevice;

import java.util.ArrayList;
import java.util.List;

public class ServerDevice extends NetworkDevice {
    private final List<String> hostedServices;
    private double cpuLoadPercent;
    private double memoryUsagePercent;
    private long totalRequestsHandled;

    public ServerDevice(String id, String name, String ip, String mac,
                        String defaultGateway, String connectedSwitchId, double x, double y, double z) {
        super(id, name, DeviceType.SERVER, DepartmentType.CORE_INFRA, ip, "2001:db8:1::50",
                mac, "255.255.255.0", defaultGateway, 1, "Fiber 10Gbps",
                connectedSwitchId, x, y, z);
        this.hostedServices = new ArrayList<>();
        this.hostedServices.add("Central Inventory Service (Port 8080)");
        this.hostedServices.add("DNS Resolver (Port 53)");
        this.hostedServices.add("DHCP Server (Port 67)");
        this.hostedServices.add("SQLite Database Engine (JDBC)");
        this.hostedServices.add("Notification Service (Port 9090)");
        this.cpuLoadPercent = 24.5;
        this.memoryUsagePercent = 48.2;
        this.totalRequestsHandled = 1420;
    }

    public List<String> getHostedServices() { return hostedServices; }
    public double getCpuLoadPercent() { return cpuLoadPercent; }
    public void setCpuLoadPercent(double cpuLoadPercent) { this.cpuLoadPercent = cpuLoadPercent; }
    public double getMemoryUsagePercent() { return memoryUsagePercent; }
    public void setMemoryUsagePercent(double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }
    public long getTotalRequestsHandled() { return totalRequestsHandled; }
    public void incrementRequests() { this.totalRequestsHandled++; }
}
