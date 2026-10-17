package com.wimn.repositories;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_FILE = "warehouse_network.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    private static DatabaseManager instance;

    private DatabaseManager() {
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Enable WAL mode for better concurrency in SQLite
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA synchronous = NORMAL;");

            // Inventory Items Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory_items (
                    sku TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    min_threshold INTEGER NOT NULL,
                    unit_price REAL NOT NULL,
                    rack_location TEXT NOT NULL,
                    department_zone TEXT NOT NULL,
                    last_updated TEXT NOT NULL
                );
            """);

            // Inventory Transactions Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory_transactions (
                    transaction_id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    sku TEXT NOT NULL,
                    product_name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    party_info TEXT,
                    timestamp TEXT NOT NULL,
                    network_packet_id TEXT,
                    department TEXT NOT NULL,
                    status TEXT NOT NULL
                );
            """);

            // Network Devices Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS network_devices (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    device_type TEXT NOT NULL,
                    department TEXT NOT NULL,
                    ipv4 TEXT NOT NULL,
                    ipv6 TEXT,
                    mac_address TEXT NOT NULL,
                    subnet_mask TEXT NOT NULL,
                    default_gateway TEXT NOT NULL,
                    vlan_id INTEGER NOT NULL,
                    connection_type TEXT NOT NULL,
                    is_online INTEGER NOT NULL,
                    connected_switch_id TEXT,
                    utilization_percent REAL NOT NULL,
                    last_comm_time TEXT NOT NULL,
                    pos_x REAL NOT NULL,
                    pos_y REAL NOT NULL,
                    pos_z REAL NOT NULL
                );
            """);

            // Firewall Rules Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS firewall_rules (
                    rule_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    source_subnet TEXT NOT NULL,
                    dest_subnet TEXT NOT NULL,
                    protocol TEXT NOT NULL,
                    dest_port INTEGER NOT NULL,
                    action TEXT NOT NULL,
                    priority INTEGER NOT NULL,
                    is_enabled INTEGER NOT NULL,
                    hit_count INTEGER NOT NULL,
                    description TEXT
                );
            """);

            // Network Events Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS network_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    category TEXT NOT NULL,
                    source_dept TEXT NOT NULL,
                    message TEXT NOT NULL
                );
            """);

            LOGGER.info("SQLite database schema initialized successfully at " + new File(DB_FILE).getAbsolutePath());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database schema", e);
        }
    }
}
