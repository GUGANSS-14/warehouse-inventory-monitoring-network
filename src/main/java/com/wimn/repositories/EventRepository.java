package com.wimn.repositories;

import com.wimn.models.NetworkEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventRepository {
    private static final Logger LOGGER = Logger.getLogger(EventRepository.class.getName());
    private static final int MAX_MEMORY_EVENTS = 500;
    private final List<NetworkEvent> memoryEvents;
    private final DatabaseManager db;

    public EventRepository() {
        this.memoryEvents = new CopyOnWriteArrayList<>();
        this.db = DatabaseManager.getInstance();
        seedDefaultEvents();
    }

    public void addEvent(NetworkEvent event) {
        if (memoryEvents.size() >= MAX_MEMORY_EVENTS) {
            memoryEvents.remove(0);
        }
        memoryEvents.add(event);

        // Async write to DB
        Thread.ofVirtual().start(() -> {
            String sql = "INSERT INTO network_events (event_id, timestamp, severity, category, source_dept, message) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, event.getEventId());
                ps.setString(2, event.getTimestamp());
                ps.setString(3, event.getSeverity().name());
                ps.setString(4, event.getCategory());
                ps.setString(5, event.getSourceDepartment());
                ps.setString(6, event.getMessage());
                ps.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.FINE, "Failed saving event to SQLite", e);
            }
        });
    }

    public List<NetworkEvent> getRecentEvents(int limit) {
        List<NetworkEvent> copy = new ArrayList<>(memoryEvents);
        Collections.reverse(copy);
        if (copy.size() > limit) {
            return copy.subList(0, limit);
        }
        return copy;
    }

    public List<NetworkEvent> getAllEvents() {
        List<NetworkEvent> copy = new ArrayList<>(memoryEvents);
        Collections.reverse(copy);
        return copy;
    }

    private void seedDefaultEvents() {
        addEvent(new NetworkEvent("EVT-INIT-01", NetworkEvent.Severity.SUCCESS, "NETWORK", "Core Infrastructure", "Core Gateway Router RTR-CORE-01 online (192.168.1.1)"));
        addEvent(new NetworkEvent("EVT-INIT-02", NetworkEvent.Severity.SUCCESS, "NETWORK", "Core Infrastructure", "Core Switch SW-CORE-01 established 10Gbps trunks to 5 department switches"));
        addEvent(new NetworkEvent("EVT-INIT-03", NetworkEvent.Severity.INFO, "SECURITY", "Security Office", "VLAN segmentation active: VLANs 1, 10, 20, 30, 40, 50"));
        addEvent(new NetworkEvent("EVT-INIT-04", NetworkEvent.Severity.INFO, "INVENTORY", "Storage Dept", "Initial warehouse catalog loaded: 12 SKUs, 6 racks populated"));
        addEvent(new NetworkEvent("EVT-INIT-05", NetworkEvent.Severity.SUCCESS, "SIMULATION", "Core Services", "DHCP/DNS/HTTP services operational on SRV-MAIN-01"));
    }
}
