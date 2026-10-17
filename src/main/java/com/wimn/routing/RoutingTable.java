package com.wimn.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoutingTable {
    private final String deviceId;
    private final List<RoutingEntry> entries;

    public RoutingTable(String deviceId) {
        this.deviceId = deviceId;
        this.entries = new ArrayList<>();
    }

    public String getDeviceId() { return deviceId; }
    public List<RoutingEntry> getEntries() { return Collections.unmodifiableList(entries); }

    public void addEntry(RoutingEntry entry) {
        entries.removeIf(e -> e.getDestinationNode().equals(entry.getDestinationNode()));
        entries.add(entry);
    }

    public RoutingEntry lookup(String destinationNode) {
        for (RoutingEntry entry : entries) {
            if (entry.getDestinationNode().equals(destinationNode)) {
                return entry;
            }
        }
        return null;
    }

    public void clear() {
        entries.clear();
    }
}
