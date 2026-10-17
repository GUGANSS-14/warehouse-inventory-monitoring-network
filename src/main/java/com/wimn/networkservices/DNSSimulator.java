package com.wimn.networkservices;

import java.util.*;

public class DNSSimulator {
    public record DNSEntry(String domain, String recordType, String resolvedValue, int ttlSeconds) {}
    public record DNSResolutionResult(
            boolean isFound,
            String queryDomain,
            String resolvedValue,
            boolean isCacheHit,
            long resolutionTimeMs,
            List<String> lookupSteps
    ) {}

    private final Map<String, DNSEntry> zoneRecords = new HashMap<>();
    private final Map<String, DNSEntry> cache = new HashMap<>();

    public DNSSimulator() {
        // Warehouse DNS Zone
        zoneRecords.put("inventory.warehouse.local", new DNSEntry("inventory.warehouse.local", "A", "192.168.1.50", 3600));
        zoneRecords.put("core.router.warehouse.local", new DNSEntry("core.router.warehouse.local", "A", "192.168.1.1", 7200));
        zoneRecords.put("cctv.security.warehouse.local", new DNSEntry("cctv.security.warehouse.local", "A", "192.168.40.21", 1800));
        zoneRecords.put("dispatch.warehouse.local", new DNSEntry("dispatch.warehouse.local", "A", "192.168.30.11", 3600));
        zoneRecords.put("receiving.warehouse.local", new DNSEntry("receiving.warehouse.local", "A", "192.168.10.11", 3600));
        zoneRecords.put("manager.warehouse.local", new DNSEntry("manager.warehouse.local", "A", "192.168.50.11", 3600));

        // Pre-populate one cache hit
        cache.put("inventory.warehouse.local", zoneRecords.get("inventory.warehouse.local"));
    }

    public DNSResolutionResult resolve(String domain) {
        String query = domain.toLowerCase().trim();
        List<String> steps = new ArrayList<>();
        steps.add("Query received for: '" + query + "' (Type A)");

        // 1. Check local cache
        if (cache.containsKey(query)) {
            DNSEntry hit = cache.get(query);
            steps.add("Cache HIT! Resolved from local client DNS cache.");
            return new DNSResolutionResult(true, query, hit.resolvedValue(), true, 1, steps);
        }

        steps.add("Cache MISS. Querying Authoritative Warehouse DNS Server (192.168.1.50:53)...");

        // 2. Authoritative lookup
        if (zoneRecords.containsKey(query)) {
            DNSEntry entry = zoneRecords.get(query);
            cache.put(query, entry);
            steps.add("Authoritative response received: " + entry.domain() + " IN A " + entry.resolvedValue() + " (TTL: " + entry.ttlSeconds() + "s)");
            steps.add("Record stored in local resolver cache.");
            return new DNSResolutionResult(true, query, entry.resolvedValue(), false, 14, steps);
        } else {
            steps.add("Authoritative server returned NXDOMAIN (Non-Existent Domain). Hostname not found in warehouse intranet.");
            return new DNSResolutionResult(false, query, "NXDOMAIN", false, 18, steps);
        }
    }

    public Map<String, DNSEntry> getZoneRecords() {
        return Collections.unmodifiableMap(zoneRecords);
    }
}
