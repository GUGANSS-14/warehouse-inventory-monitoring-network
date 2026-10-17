package com.wimn.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NetworkDevice {
    private String id;
    private String name;
    private DeviceType deviceType;
    private DepartmentType department;
    private String ipv4;
    private String ipv6;
    private String macAddress;
    private String subnetMask;
    private String defaultGateway;
    private int vlanId;
    private String connectionType;
    private boolean isOnline;
    private String connectedSwitchId;
    private double utilizationPercent;
    private String lastCommunicationTime;
    
    // Position coordinates for 2D and 3D visualizers
    private double posX;
    private double posY;
    private double posZ;

    public NetworkDevice() {
        this.isOnline = true;
        this.lastCommunicationTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public NetworkDevice(String id, String name, DeviceType deviceType, DepartmentType department,
                         String ipv4, String ipv6, String macAddress, String subnetMask,
                         String defaultGateway, int vlanId, String connectionType,
                         String connectedSwitchId, double posX, double posY, double posZ) {
        this.id = id;
        this.name = name;
        this.deviceType = deviceType;
        this.department = department;
        this.ipv4 = ipv4;
        this.ipv6 = ipv6;
        this.macAddress = macAddress;
        this.subnetMask = subnetMask;
        this.defaultGateway = defaultGateway;
        this.vlanId = vlanId;
        this.connectionType = connectionType;
        this.isOnline = true;
        this.connectedSwitchId = connectedSwitchId;
        this.utilizationPercent = 15.0 + (Math.random() * 25.0);
        this.lastCommunicationTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }

    public DepartmentType getDepartment() { return department; }
    public void setDepartment(DepartmentType department) { this.department = department; }

    public String getIpv4() { return ipv4; }
    public void setIpv4(String ipv4) { this.ipv4 = ipv4; }

    public String getIpv6() { return ipv6; }
    public void setIpv6(String ipv6) { this.ipv6 = ipv6; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getSubnetMask() { return subnetMask; }
    public void setSubnetMask(String subnetMask) { this.subnetMask = subnetMask; }

    public String getDefaultGateway() { return defaultGateway; }
    public void setDefaultGateway(String defaultGateway) { this.defaultGateway = defaultGateway; }

    public int getVlanId() { return vlanId; }
    public void setVlanId(int vlanId) { this.vlanId = vlanId; }

    public String getConnectionType() { return connectionType; }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public String getConnectedSwitchId() { return connectedSwitchId; }
    public void setConnectedSwitchId(String connectedSwitchId) { this.connectedSwitchId = connectedSwitchId; }

    public double getUtilizationPercent() { return utilizationPercent; }
    public void setUtilizationPercent(double utilizationPercent) { this.utilizationPercent = utilizationPercent; }

    public String getLastCommunicationTime() { return lastCommunicationTime; }
    public void setLastCommunicationTime(String lastCommunicationTime) { this.lastCommunicationTime = lastCommunicationTime; }

    public double getPosX() { return posX; }
    public void setPosX(double posX) { this.posX = posX; }

    public double getPosY() { return posY; }
    public void setPosY(double posY) { this.posY = posY; }

    public double getPosZ() { return posZ; }
    public void setPosZ(double posZ) { this.posZ = posZ; }

    public void touchCommunication() {
        this.lastCommunicationTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Override
    public String toString() {
        return name + " (" + ipv4 + ")";
    }
}
