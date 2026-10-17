package com.wimn.repositories;

import com.wimn.models.DepartmentType;
import com.wimn.models.InventoryItem;
import com.wimn.models.InventoryTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InventoryRepository {
    private static final Logger LOGGER = Logger.getLogger(InventoryRepository.class.getName());
    private final DatabaseManager db;

    public InventoryRepository() {
        this.db = DatabaseManager.getInstance();
        seedInitialDataIfEmpty();
    }

    public List<InventoryItem> getAllItems() {
        List<InventoryItem> items = new ArrayList<>();
        String sql = "SELECT * FROM inventory_items ORDER BY department_zone, sku";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                InventoryItem item = new InventoryItem(
                        rs.getString("sku"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getInt("min_threshold"),
                        rs.getDouble("unit_price"),
                        rs.getString("rack_location"),
                        DepartmentType.valueOf(rs.getString("department_zone"))
                );
                item.setLastUpdated(rs.getString("last_updated"));
                items.add(item);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading inventory items", e);
        }
        return items;
    }

    public InventoryItem getItemBySku(String sku) {
        String sql = "SELECT * FROM inventory_items WHERE sku = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    InventoryItem item = new InventoryItem(
                            rs.getString("sku"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getInt("quantity"),
                            rs.getInt("min_threshold"),
                            rs.getDouble("unit_price"),
                            rs.getString("rack_location"),
                            DepartmentType.valueOf(rs.getString("department_zone"))
                    );
                    item.setLastUpdated(rs.getString("last_updated"));
                    return item;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching item by sku: " + sku, e);
        }
        return null;
    }

    public boolean saveOrUpdateItem(InventoryItem item) {
        String sql = """
            INSERT INTO inventory_items (sku, name, category, quantity, min_threshold, unit_price, rack_location, department_zone, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(sku) DO UPDATE SET
                name = excluded.name,
                category = excluded.category,
                quantity = excluded.quantity,
                min_threshold = excluded.min_threshold,
                unit_price = excluded.unit_price,
                rack_location = excluded.rack_location,
                department_zone = excluded.department_zone,
                last_updated = excluded.last_updated;
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getSku());
            ps.setString(2, item.getName());
            ps.setString(3, item.getCategory());
            ps.setInt(4, item.getQuantity());
            ps.setInt(5, item.getMinThreshold());
            ps.setDouble(6, item.getUnitPrice());
            ps.setString(7, item.getRackLocation());
            ps.setString(8, item.getDepartmentZone().name());
            ps.setString(9, item.getLastUpdated());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving item: " + item.getSku(), e);
            return false;
        }
    }

    public boolean adjustStock(String sku, int quantityDelta, InventoryTransaction tx) {
        String selectSql = "SELECT quantity FROM inventory_items WHERE sku = ?";
        String updateSql = "UPDATE inventory_items SET quantity = ?, last_updated = ? WHERE sku = ?";

        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psSel = conn.prepareStatement(selectSql)) {
                psSel.setString(1, sku);
                ResultSet rs = psSel.executeQuery();
                if (!rs.next()) {
                    conn.rollback();
                    return false;
                }
                int currentQty = rs.getInt("quantity");
                int newQty = currentQty + quantityDelta;
                if (newQty < 0) {
                    conn.rollback();
                    return false; // Insufficient stock
                }

                try (PreparedStatement psUpd = conn.prepareStatement(updateSql)) {
                    psUpd.setInt(1, newQty);
                    psUpd.setString(2, tx.getTimestamp());
                    psUpd.setString(3, sku);
                    psUpd.executeUpdate();
                }

                // Record transaction
                saveTransactionInternal(conn, tx);

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Transaction rollback for sku: " + sku, ex);
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection error during stock adjust", e);
            return false;
        }
    }

    private void saveTransactionInternal(Connection conn, InventoryTransaction tx) throws SQLException {
        String sql = """
            INSERT INTO inventory_transactions (transaction_id, type, sku, product_name, quantity, party_info, timestamp, network_packet_id, department, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tx.getTransactionId());
            ps.setString(2, tx.getType().name());
            ps.setString(3, tx.getSku());
            ps.setString(4, tx.getProductName());
            ps.setInt(5, tx.getQuantity());
            ps.setString(6, tx.getPartyInfo());
            ps.setString(7, tx.getTimestamp());
            ps.setString(8, tx.getNetworkPacketId());
            ps.setString(9, tx.getDepartment());
            ps.setString(10, tx.getStatus());
            ps.executeUpdate();
        }
    }

    public List<InventoryTransaction> getAllTransactions() {
        List<InventoryTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory_transactions ORDER BY timestamp DESC LIMIT 200";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                InventoryTransaction tx = new InventoryTransaction(
                        rs.getString("transaction_id"),
                        InventoryTransaction.TransactionType.valueOf(rs.getString("type")),
                        rs.getString("sku"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getString("party_info"),
                        rs.getString("network_packet_id"),
                        rs.getString("department"),
                        rs.getString("status")
                );
                tx.setTimestamp(rs.getString("timestamp"));
                list.add(tx);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading inventory transactions", e);
        }
        return list;
    }

    public void seedInitialDataIfEmpty() {
        String countSql = "SELECT COUNT(*) FROM inventory_items";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                LOGGER.info("Seeding realistic warehouse inventory items...");
                List<InventoryItem> seeds = List.of(
                    new InventoryItem("SKU-SRV-01", "Cisco Catalyst 9300 Switch 48P", "Networking Hardware", 18, 5, 2450.0, "Rack-A1-01", DepartmentType.STORAGE),
                    new InventoryItem("SKU-SRV-02", "Dell PowerEdge R750 2U Server", "Computing Servers", 8, 3, 4800.0, "Rack-A1-02", DepartmentType.STORAGE),
                    new InventoryItem("SKU-NET-03", "Ubiquiti UniFi 6 Pro Access Point", "Wireless Systems", 45, 10, 189.0, "Rack-B2-01", DepartmentType.STORAGE),
                    new InventoryItem("SKU-NET-04", "Cat6A Shielded Ethernet Cable 305m", "Cabling Infrastructure", 60, 15, 125.0, "Rack-B2-02", DepartmentType.RECEIVING),
                    new InventoryItem("SKU-SEC-05", "Hikvision 4K Dome IP Camera PoE", "Security Surveillance", 22, 6, 210.0, "Rack-C3-01", DepartmentType.STORAGE),
                    new InventoryItem("SKU-IOT-06", "Zigbee Temperature & Humidity Sensor", "IoT Environmental", 110, 20, 32.5, "Rack-C3-02", DepartmentType.STORAGE),
                    new InventoryItem("SKU-SCN-07", "Zebra TC52x Rugged Android Barcode Scanner", "Mobile Terminals", 14, 4, 890.0, "Rack-D4-01", DepartmentType.RECEIVING),
                    new InventoryItem("SKU-OPT-08", "LC-LC Single-Mode Fiber Patch Cord 10m", "Fiber Optics", 85, 25, 24.0, "Rack-B2-03", DepartmentType.STORAGE),
                    new InventoryItem("SKU-DIS-09", "Heavy Duty Warehouse Pallet 120x100cm", "Material Handling", 150, 30, 45.0, "Bay-DIS-01", DepartmentType.DISPATCH),
                    new InventoryItem("SKU-PWR-10", "APC Smart-UPS RT 3000VA On-Line", "Power & Protection", 6, 2, 1750.0, "Rack-A1-03", DepartmentType.STORAGE),
                    new InventoryItem("SKU-SEC-11", "RFID Ultra-High Frequency Gate Antenna", "RFID Hardware", 12, 4, 620.0, "Rack-C3-03", DepartmentType.SECURITY),
                    new InventoryItem("SKU-PC-12", "HP EliteDesk 800 G9 Manager Workstation", "Office Workstations", 10, 3, 980.0, "Office-MGR-01", DepartmentType.MANAGER)
                );
                for (InventoryItem it : seeds) {
                    saveOrUpdateItem(it);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed seeding inventory items", e);
        }
    }
}
