package com.wimn.topology;

import com.wimn.models.*;
import com.wimn.repositories.DeviceRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WarehouseTopology {
    private final DeviceRepository deviceRepo;
    private final Map<String, NetworkDevice> devicesMap;
    private final List<NetworkLink> links;

    public WarehouseTopology(DeviceRepository deviceRepo) {
        this.deviceRepo = deviceRepo;
        this.devicesMap = new ConcurrentHashMap<>();
        this.links = new CopyOnWriteArrayList<>();
        reloadFromRepository();
        buildDefaultLinks();
    }

    public void reloadFromRepository() {
        devicesMap.clear();
        for (NetworkDevice d : deviceRepo.getAllDevices()) {
            devicesMap.put(d.getId(), d);
        }
    }

    private void buildDefaultLinks() {
        links.clear();
        // Core Backbone Links (Fiber 10Gbps, ~0.5ms latency)
        addLink("LNK-RTR-CORE", "RTR-CORE-01", "SW-CORE-01", 10000.0, 0.4, "Fiber Optic 10G");
        addLink("LNK-CORE-SRV", "SW-CORE-01", "SRV-MAIN-01", 10000.0, 0.2, "Fiber Optic 10G");

        // Department Distribution Trunks from Core Switch (Cat6A 10Gbps, ~0.8ms latency)
        addLink("LNK-CORE-RCV", "SW-CORE-01", "SW-RCV-01", 10000.0, 0.8, "Cat6A Shielded");
        addLink("LNK-CORE-STR", "SW-CORE-01", "SW-STR-01", 10000.0, 0.7, "Cat6A Shielded");
        addLink("LNK-CORE-DSP", "SW-CORE-01", "SW-DSP-01", 10000.0, 0.8, "Cat6A Shielded");
        addLink("LNK-CORE-SEC", "SW-CORE-01", "SW-SEC-01", 10000.0, 0.9, "Cat6A Shielded");
        addLink("LNK-CORE-MGR", "SW-CORE-01", "SW-MGR-01", 10000.0, 0.6, "Cat6A Shielded");

        // Receiving Dept Edge Links
        addLink("LNK-RCV-WS", "SW-RCV-01", "WS-RCV-01", 1000.0, 1.2, "Cat6A 1Gbps");
        addLink("LNK-RCV-AP", "SW-RCV-01", "AP-RCV-01", 2500.0, 1.0, "PoE+ 2.5Gbps");
        addLink("LNK-RCV-SCN", "AP-RCV-01", "SCN-RCV-01", 1200.0, 3.5, "Wi-Fi 6 (802.11ax)");

        // Storage Dept Edge Links
        addLink("LNK-STR-AP", "SW-STR-01", "AP-STR-01", 2500.0, 1.1, "PoE+ 2.5Gbps");
        addLink("LNK-STR-RFID", "SW-STR-01", "RFID-STR-01", 1000.0, 1.5, "Cat6A 1Gbps");
        addLink("LNK-STR-IOT", "AP-STR-01", "IOT-STR-01", 50.0, 8.2, "Zigbee Mesh (802.15.4)");

        // Dispatch Dept Edge Links
        addLink("LNK-DSP-WS", "SW-DSP-01", "WS-DSP-01", 1000.0, 1.2, "Cat6A 1Gbps");
        addLink("LNK-DSP-SCN", "SW-DSP-01", "SCN-DSP-01", 1200.0, 3.8, "Wi-Fi 6 (802.11ax)");

        // Security Dept Edge Links
        addLink("LNK-SEC-WS", "SW-SEC-01", "WS-SEC-01", 1000.0, 1.0, "Cat6A 1Gbps");
        addLink("LNK-SEC-CAM", "SW-SEC-01", "CAM-SEC-01", 1000.0, 2.1, "PoE 1Gbps RTSP");

        // Manager Dept Edge Links
        addLink("LNK-MGR-WS", "SW-MGR-01", "WS-MGR-01", 1000.0, 0.9, "Cat6A 1Gbps");

        // Redundant / Alternative Failover Links (Secondary paths for routing fault demonstrations)
        addLink("LNK-BAK-RCV-STR", "SW-RCV-01", "SW-STR-01", 5000.0, 2.0, "Redundant Cat6A Trunk");
        addLink("LNK-BAK-STR-DSP", "SW-STR-01", "SW-DSP-01", 5000.0, 2.0, "Redundant Cat6A Trunk");
        addLink("LNK-BAK-DSP-SEC", "SW-DSP-01", "SW-SEC-01", 5000.0, 2.2, "Redundant Cat6A Trunk");
    }

    private void addLink(String id, String src, String dst, double bw, double lat, String medium) {
        links.add(new NetworkLink(id, src, dst, bw, lat, medium));
    }

    public Collection<NetworkDevice> getDevices() {
        return devicesMap.values();
    }

    public NetworkDevice getDevice(String id) {
        return devicesMap.get(id);
    }

    public List<NetworkLink> getLinks() {
        return links;
    }

    public NetworkLink getLink(String linkId) {
        return links.stream().filter(l -> l.getLinkId().equals(linkId)).findFirst().orElse(null);
    }

    public NetworkLink findLinkBetween(String devA, String devB) {
        return links.stream().filter(l -> l.connects(devA, devB)).findFirst().orElse(null);
    }

    public List<NetworkLink> getConnectedLinks(String deviceId) {
        List<NetworkLink> res = new ArrayList<>();
        for (NetworkLink l : links) {
            if (l.getSourceDeviceId().equals(deviceId) || l.getTargetDeviceId().equals(deviceId)) {
                res.add(l);
            }
        }
        return res;
    }

    public void setLinkStatus(String linkId, LinkStatus status) {
        NetworkLink l = getLink(linkId);
        if (l != null) {
            l.setStatus(status);
        }
    }

    public void setDeviceOnline(String deviceId, boolean online) {
        NetworkDevice d = getDevice(deviceId);
        if (d != null) {
            d.setOnline(online);
            deviceRepo.updateStatus(deviceId, online);
        }
    }

    public int getOnlineDeviceCount() {
        return (int) devicesMap.values().stream().filter(NetworkDevice::isOnline).count();
    }

    public int getTotalDeviceCount() {
        return devicesMap.size();
    }

    public int getActiveLinkCount() {
        return (int) links.stream().filter(l -> l.getStatus() == LinkStatus.UP).count();
    }
}
