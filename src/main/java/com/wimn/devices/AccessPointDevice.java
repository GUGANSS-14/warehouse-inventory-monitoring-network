package com.wimn.devices;

import com.wimn.models.DepartmentType;
import com.wimn.models.DeviceType;
import com.wimn.models.NetworkDevice;

public class AccessPointDevice extends NetworkDevice {
    private String ssid;
    private String frequencyBand; // "2.4GHz / 5GHz / 6GHz"
    private int channelNumber;
    private int connectedClientsCount;

    public AccessPointDevice(String id, String name, DepartmentType department, String ip,
                             String mac, String gateway, int vlanId, String connectedSwitchId,
                             String ssid, int channelNumber, double x, double y, double z) {
        super(id, name, DeviceType.ACCESS_POINT, department, ip, "2001:db8:1::" + Math.abs(id.hashCode() % 250),
                mac, "255.255.255.0", gateway, vlanId, "Ethernet PoE 2.5Gbps",
                connectedSwitchId, x, y, z);
        this.ssid = ssid;
        this.frequencyBand = "Dual-Band Wi-Fi 6 (802.11ax)";
        this.channelNumber = channelNumber;
        this.connectedClientsCount = 4;
    }

    public String getSsid() { return ssid; }
    public String getFrequencyBand() { return frequencyBand; }
    public int getChannelNumber() { return channelNumber; }
    public int getConnectedClientsCount() { return connectedClientsCount; }
    public void setConnectedClientsCount(int count) { this.connectedClientsCount = count; }
}
