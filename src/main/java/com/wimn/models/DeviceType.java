package com.wimn.models;

public enum DeviceType {
    ROUTER("Router", "Gateway & Routing Engine"),
    SWITCH("Switch", "Layer 2/3 Packet Switching"),
    SERVER("Server", "Central Inventory & Services"),
    ACCESS_POINT("Access Point", "802.11 Wi-Fi Gateway"),
    WORKSTATION("Workstation", "Department PC Terminal"),
    BARCODE_SCANNER("Barcode Scanner", "Handheld Wi-Fi / Bluetooth Scanner"),
    RFID_READER("RFID Reader", "Fixed Warehouse Gate Reader"),
    CCTV_CAMERA("CCTV Camera", "IP Surveillance Streamer"),
    IOT_SENSOR("IoT Sensor", "Zigbee / Low-Power Environment Node");

    private final String title;
    private final String description;

    DeviceType(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }

    @Override
    public String toString() { return title; }
}
