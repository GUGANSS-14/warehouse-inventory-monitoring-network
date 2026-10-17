package com.wimn.devices;

import com.wimn.models.DepartmentType;
import com.wimn.models.DeviceType;
import com.wimn.models.NetworkDevice;

public class TerminalDevice extends NetworkDevice {
    private String currentUser;
    private String terminalRole;

    public TerminalDevice(String id, String name, DepartmentType department, String ip,
                          String mac, String gateway, int vlanId, String connectedSwitchId,
                          String currentUser, String terminalRole, double x, double y, double z) {
        super(id, name, DeviceType.WORKSTATION, department, ip, "2001:db8:1::" + id.hashCode() % 100,
                mac, "255.255.255.0", gateway, vlanId, "Ethernet 1Gbps",
                connectedSwitchId, x, y, z);
        this.currentUser = currentUser;
        this.terminalRole = terminalRole;
    }

    public String getCurrentUser() { return currentUser; }
    public void setCurrentUser(String currentUser) { this.currentUser = currentUser; }

    public String getTerminalRole() { return terminalRole; }
    public void setTerminalRole(String terminalRole) { this.terminalRole = terminalRole; }
}
