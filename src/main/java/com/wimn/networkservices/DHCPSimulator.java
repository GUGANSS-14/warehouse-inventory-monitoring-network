package com.wimn.networkservices;

import java.util.*;

public class DHCPSimulator {
    public record DHCPLease(String mac, String ip, String gateway, String mask, String dns, long leaseSeconds) {}
    public record DORAStep(String messageType, String source, String destination, String description) {}

    private final Map<String, DHCPLease> activeLeases = new LinkedHashMap<>();
    private int currentHostId = 100;
    private final String subnetPrefix = "192.168.10.";

    public List<DORAStep> requestLease(String clientMac, String hostname) {
        List<DORAStep> steps = new ArrayList<>();

        // 1. DISCOVER
        steps.add(new DORAStep(
                "DHCP DISCOVER", "0.0.0.0 (Port 68)", "255.255.255.255 (Port 67)",
                "Client " + clientMac + " (" + hostname + ") broadcasts DISCOVER requesting IP parameters."
        ));

        // 2. OFFER
        String offeredIp = subnetPrefix + currentHostId;
        steps.add(new DORAStep(
                "DHCP OFFER", "192.168.1.50 (Port 67)", "255.255.255.255 (Port 68)",
                "DHCP Server offers IP " + offeredIp + " with Subnet Mask 255.255.255.0 and Gateway 192.168.10.1"
        ));

        // 3. REQUEST
        steps.add(new DORAStep(
                "DHCP REQUEST", "0.0.0.0 (Port 68)", "255.255.255.255 (Port 67)",
                "Client requests to lease offered IP " + offeredIp + " from server 192.168.1.50"
        ));

        // 4. ACKNOWLEDGE
        DHCPLease lease = new DHCPLease(clientMac, offeredIp, "192.168.10.1", "255.255.255.0", "192.168.1.50", 86400);
        activeLeases.put(clientMac, lease);
        currentHostId++;

        steps.add(new DORAStep(
                "DHCP ACK", "192.168.1.50 (Port 67)", "192.168.10." + (currentHostId - 1) + " (Port 68)",
                "DHCP Server acknowledges lease for " + offeredIp + " (Lease Duration: 24 Hours / 86400s)."
        ));

        return steps;
    }

    public Map<String, DHCPLease> getActiveLeases() {
        return Collections.unmodifiableMap(activeLeases);
    }
}
