package com.wimn.repositories;

import com.wimn.models.DepartmentType;
import com.wimn.models.DeviceType;
import com.wimn.models.NetworkDevice;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeviceRepository {
    private static final Logger LOGGER = Logger.getLogger(DeviceRepository.class.getName());
    private final DatabaseManager db;

    public DeviceRepository() {
        this.db = DatabaseManager.getInstance();
        seedInitialDevicesIfEmpty();
    }

    public List<NetworkDevice> getAllDevices() {
        List<NetworkDevice> list = new ArrayList<>();
        String sql = "SELECT * FROM network_devices ORDER BY department, id";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NetworkDevice dev = new NetworkDevice(
                        rs.getString("id"),
                        rs.getString("name"),
                        DeviceType.valueOf(rs.getString("device_type")),
                        DepartmentType.valueOf(rs.getString("department")),
                        rs.getString("ipv4"),
                        rs.getString("ipv6"),
                        rs.getString("mac_address"),
                        rs.getString("subnet_mask"),
                        rs.getString("default_gateway"),
                        rs.getInt("vlan_id"),
                        rs.getString("connection_type"),
                        rs.getString("connected_switch_id"),
                        rs.getDouble("pos_x"),
                        rs.getDouble("pos_y"),
                        rs.getDouble("pos_z")
                );
                dev.setOnline(rs.getInt("is_online") == 1);
                dev.setUtilizationPercent(rs.getDouble("utilization_percent"));
                dev.setLastCommunicationTime(rs.getString("last_comm_time"));
                list.add(dev);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching network devices", e);
        }
        return list;
    }

    public NetworkDevice getDeviceById(String id) {
        String sql = "SELECT * FROM network_devices WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    NetworkDevice dev = new NetworkDevice(
                            rs.getString("id"),
                            rs.getString("name"),
                            DeviceType.valueOf(rs.getString("device_type")),
                            DepartmentType.valueOf(rs.getString("department")),
                            rs.getString("ipv4"),
                            rs.getString("ipv6"),
                            rs.getString("mac_address"),
                            rs.getString("subnet_mask"),
                            rs.getString("default_gateway"),
                            rs.getInt("vlan_id"),
                            rs.getString("connection_type"),
                            rs.getString("connected_switch_id"),
                            rs.getDouble("pos_x"),
                            rs.getDouble("pos_y"),
                            rs.getDouble("pos_z")
                    );
                    dev.setOnline(rs.getInt("is_online") == 1);
                    dev.setUtilizationPercent(rs.getDouble("utilization_percent"));
                    dev.setLastCommunicationTime(rs.getString("last_comm_time"));
                    return dev;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching device: " + id, e);
        }
        return null;
    }

    public boolean saveOrUpdateDevice(NetworkDevice dev) {
        String sql = """
            INSERT INTO network_devices (id, name, device_type, department, ipv4, ipv6, mac_address, subnet_mask, default_gateway, vlan_id, connection_type, is_online, connected_switch_id, utilization_percent, last_comm_time, pos_x, pos_y, pos_z)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                device_type = excluded.device_type,
                department = excluded.department,
                ipv4 = excluded.ipv4,
                ipv6 = excluded.ipv6,
                mac_address = excluded.mac_address,
                subnet_mask = excluded.subnet_mask,
                default_gateway = excluded.default_gateway,
                vlan_id = excluded.vlan_id,
                connection_type = excluded.connection_type,
                is_online = excluded.is_online,
                connected_switch_id = excluded.connected_switch_id,
                utilization_percent = excluded.utilization_percent,
                last_comm_time = excluded.last_comm_time,
                pos_x = excluded.pos_x,
                pos_y = excluded.pos_y,
                pos_z = excluded.pos_z;
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dev.getId());
            ps.setString(2, dev.getName());
            ps.setString(3, dev.getDeviceType().name());
            ps.setString(4, dev.getDepartment().name());
            ps.setString(5, dev.getIpv4());
            ps.setString(6, dev.getIpv6());
            ps.setString(7, dev.getMacAddress());
            ps.setString(8, dev.getSubnetMask());
            ps.setString(9, dev.getDefaultGateway());
            ps.setInt(10, dev.getVlanId());
            ps.setString(11, dev.getConnectionType());
            ps.setInt(12, dev.isOnline() ? 1 : 0);
            ps.setString(13, dev.getConnectedSwitchId());
            ps.setDouble(14, dev.getUtilizationPercent());
            ps.setString(15, dev.getLastCommunicationTime());
            ps.setDouble(16, dev.getPosX());
            ps.setDouble(17, dev.getPosY());
            ps.setDouble(18, dev.getPosZ());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving device: " + dev.getId(), e);
            return false;
        }
    }

    public boolean deleteDevice(String id) {
        String sql = "DELETE FROM network_devices WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting device: " + id, e);
            return false;
        }
    }

    public boolean updateStatus(String id, boolean online) {
        String sql = "UPDATE network_devices SET is_online = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, online ? 1 : 0);
            ps.setString(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating status for: " + id, e);
            return false;
        }
    }

    public void seedInitialDevicesIfEmpty() {
        String countSql = "SELECT COUNT(*) FROM network_devices";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                LOGGER.info("Seeding network devices for hierarchical star warehouse topology...");
                List<NetworkDevice> devices = List.of(
                    // Core Infrastructure
                    new NetworkDevice("RTR-CORE-01", "Enterprise Edge Gateway Router", DeviceType.ROUTER, DepartmentType.CORE_INFRA, "192.168.1.1", "2001:db8:1::1", "00:1A:2B:01:00:01", "255.255.255.0", "10.0.0.1", 1, "Fiber 10Gbps", null, 400, 80, 0),
                    new NetworkDevice("SW-CORE-01", "Core Distribution Switch L3", DeviceType.SWITCH, DepartmentType.CORE_INFRA, "192.168.1.2", "2001:db8:1::2", "00:1A:2B:01:00:02", "255.255.255.0", "192.168.1.1", 1, "Fiber 10Gbps", "RTR-CORE-01", 400, 180, 0),
                    new NetworkDevice("SRV-MAIN-01", "Central Inventory & DNS Server", DeviceType.SERVER, DepartmentType.CORE_INFRA, "192.168.1.50", "2001:db8:1::50", "00:1A:2B:01:00:50", "255.255.255.0", "192.168.1.1", 1, "Fiber 10Gbps", "SW-CORE-01", 580, 180, 0),

                    // Receiving Department (VLAN 10)
                    new NetworkDevice("SW-RCV-01", "Receiving Access Switch", DeviceType.SWITCH, DepartmentType.RECEIVING, "192.168.10.2", "2001:db8:10::2", "00:1A:2B:10:00:02", "255.255.255.0", "192.168.10.1", 10, "Cat6A 10Gbps", "SW-CORE-01", 150, 320, 0),
                    new NetworkDevice("WS-RCV-01", "Receiving Desk Terminal PC", DeviceType.WORKSTATION, DepartmentType.RECEIVING, "192.168.10.11", "2001:db8:10::11", "00:1A:2B:10:00:11", "255.255.255.0", "192.168.10.1", 10, "Cat6A 1Gbps", "SW-RCV-01", 80, 440, 0),
                    new NetworkDevice("AP-RCV-01", "Receiving Wi-Fi 6 AP", DeviceType.ACCESS_POINT, DepartmentType.RECEIVING, "192.168.10.5", "2001:db8:10::5", "00:1A:2B:10:00:05", "255.255.255.0", "192.168.10.1", 10, "PoE 2.5Gbps", "SW-RCV-01", 150, 440, 0),
                    new NetworkDevice("SCN-RCV-01", "Inbound Zebra Barcode Scanner", DeviceType.BARCODE_SCANNER, DepartmentType.RECEIVING, "192.168.10.21", "2001:db8:10::21", "00:1A:2B:10:00:21", "255.255.255.0", "192.168.10.1", 10, "Wi-Fi 6", "AP-RCV-01", 220, 440, 0),

                    // Storage Department (VLAN 20)
                    new NetworkDevice("SW-STR-01", "Storage Aisle Access Switch", DeviceType.SWITCH, DepartmentType.STORAGE, "192.168.20.2", "2001:db8:20::2", "00:1A:2B:20:00:02", "255.255.255.0", "192.168.20.1", 20, "Cat6A 10Gbps", "SW-CORE-01", 300, 320, 0),
                    new NetworkDevice("AP-STR-01", "Storage High-Bay Wi-Fi AP", DeviceType.ACCESS_POINT, DepartmentType.STORAGE, "192.168.20.5", "2001:db8:20::5", "00:1A:2B:20:00:05", "255.255.255.0", "192.168.20.1", 20, "PoE 2.5Gbps", "SW-STR-01", 300, 440, 0),
                    new NetworkDevice("RFID-STR-01", "Aisle A1 RFID Gate Reader", DeviceType.RFID_READER, DepartmentType.STORAGE, "192.168.20.31", "2001:db8:20::31", "00:1A:2B:20:00:31", "255.255.255.0", "192.168.20.1", 20, "Cat6A 1Gbps", "SW-STR-01", 360, 440, 0),
                    new NetworkDevice("IOT-STR-01", "Environmental Zigbee Gateway", DeviceType.IOT_SENSOR, DepartmentType.STORAGE, "192.168.20.41", "2001:db8:20::41", "00:1A:2B:20:00:41", "255.255.255.0", "192.168.20.1", 20, "Zigbee Mesh", "AP-STR-01", 300, 520, 0),

                    // Dispatch Department (VLAN 30)
                    new NetworkDevice("SW-DSP-01", "Dispatch Bay Access Switch", DeviceType.SWITCH, DepartmentType.DISPATCH, "192.168.30.2", "2001:db8:30::2", "00:1A:2B:30:00:02", "255.255.255.0", "192.168.30.1", 30, "Cat6A 10Gbps", "SW-CORE-01", 450, 320, 0),
                    new NetworkDevice("WS-DSP-01", "Dispatch Outbound Station PC", DeviceType.WORKSTATION, DepartmentType.DISPATCH, "192.168.30.11", "2001:db8:30::11", "00:1A:2B:30:00:11", "255.255.255.0", "192.168.30.1", 30, "Cat6A 1Gbps", "SW-DSP-01", 450, 440, 0),
                    new NetworkDevice("SCN-DSP-01", "Dispatch Bay Handheld Scanner", DeviceType.BARCODE_SCANNER, DepartmentType.DISPATCH, "192.168.30.21", "2001:db8:30::21", "00:1A:2B:30:00:21", "255.255.255.0", "192.168.30.1", 30, "Wi-Fi 6", "SW-DSP-01", 510, 440, 0),

                    // Security Office (VLAN 40)
                    new NetworkDevice("SW-SEC-01", "Security Surveillance Switch PoE", DeviceType.SWITCH, DepartmentType.SECURITY, "192.168.40.2", "2001:db8:40::2", "00:1A:2B:40:00:02", "255.255.255.0", "192.168.40.1", 40, "Cat6A 10Gbps", "SW-CORE-01", 600, 320, 0),
                    new NetworkDevice("WS-SEC-01", "Security Monitor Console PC", DeviceType.WORKSTATION, DepartmentType.SECURITY, "192.168.40.11", "2001:db8:40::11", "00:1A:2B:40:00:11", "255.255.255.0", "192.168.40.1", 40, "Cat6A 1Gbps", "SW-SEC-01", 580, 440, 0),
                    new NetworkDevice("CAM-SEC-01", "Warehouse Perimeter CCTV Camera", DeviceType.CCTV_CAMERA, DepartmentType.SECURITY, "192.168.40.21", "2001:db8:40::21", "00:1A:2B:40:00:21", "255.255.255.0", "192.168.40.1", 40, "PoE 1Gbps", "SW-SEC-01", 640, 440, 0),

                    // Manager Office (VLAN 50)
                    new NetworkDevice("SW-MGR-01", "Manager Office Access Switch", DeviceType.SWITCH, DepartmentType.MANAGER, "192.168.50.2", "2001:db8:50::2", "00:1A:2B:50:00:02", "255.255.255.0", "192.168.50.1", 50, "Cat6A 10Gbps", "SW-CORE-01", 750, 320, 0),
                    new NetworkDevice("WS-MGR-01", "General Manager Workstation PC", DeviceType.WORKSTATION, DepartmentType.MANAGER, "192.168.50.11", "2001:db8:50::11", "00:1A:2B:50:00:11", "255.255.255.0", "192.168.50.1", 50, "Cat6A 1Gbps", "SW-MGR-01", 750, 440, 0)
                );

                for (NetworkDevice dev : devices) {
                    saveOrUpdateDevice(dev);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed seeding network devices", e);
        }
    }
}
