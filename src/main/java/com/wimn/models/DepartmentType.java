package com.wimn.models;

public enum DepartmentType {
    RECEIVING("Receiving Dept", "VLAN 10", "192.168.10.0/24"),
    STORAGE("Storage Dept", "VLAN 20", "192.168.20.0/24"),
    DISPATCH("Dispatch Dept", "VLAN 30", "192.168.30.0/24"),
    SECURITY("Security Office", "VLAN 40", "192.168.40.0/24"),
    MANAGER("Manager Office", "VLAN 50", "192.168.50.0/24"),
    CORE_INFRA("Core Network / Server Room", "VLAN 1", "192.168.1.0/24");

    private final String displayName;
    private final String vlan;
    private final String subnet;

    DepartmentType(String displayName, String vlan, String subnet) {
        this.displayName = displayName;
        this.vlan = vlan;
        this.subnet = subnet;
    }

    public String getDisplayName() { return displayName; }
    public String getVlan() { return vlan; }
    public String getSubnet() { return subnet; }

    @Override
    public String toString() { return displayName; }
}
