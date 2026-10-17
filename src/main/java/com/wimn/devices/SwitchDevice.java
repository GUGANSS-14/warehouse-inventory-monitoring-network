package com.wimn.devices;

import com.wimn.models.DepartmentType;
import com.wimn.models.DeviceType;
import com.wimn.models.NetworkDevice;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SwitchDevice extends NetworkDevice {
    private boolean isCoreSwitch;
    private int totalPorts;
    private final Map<String, Integer> macAddressTable; // MAC -> Port number

    public SwitchDevice(String id, String name, DepartmentType department, String ip,
                        String mac, int vlanId, boolean isCoreSwitch, int totalPorts,
                        String connectedSwitchId, double x, double y, double z) {
        super(id, name, DeviceType.SWITCH, department, ip, "2001:db8:1::" + (isCoreSwitch ? "2" : "10"),
                mac, "255.255.255.0", "192.168.1.1", vlanId, "Ethernet 10Gbps",
                connectedSwitchId, x, y, z);
        this.isCoreSwitch = isCoreSwitch;
        this.totalPorts = totalPorts;
        this.macAddressTable = new ConcurrentHashMap<>();
    }

    public boolean isCoreSwitch() { return isCoreSwitch; }
    public int getTotalPorts() { return totalPorts; }
    public Map<String, Integer> getMacAddressTable() { return macAddressTable; }

    public void learnMac(String mac, int port) {
        macAddressTable.put(mac, port);
    }

    public Integer lookupMac(String mac) {
        return macAddressTable.get(mac);
    }
}
