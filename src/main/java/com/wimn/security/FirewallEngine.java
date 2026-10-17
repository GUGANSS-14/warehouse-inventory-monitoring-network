package com.wimn.security;

import com.wimn.models.FirewallRule;
import com.wimn.models.NetworkEvent;
import com.wimn.models.NetworkPacket;
import com.wimn.repositories.EventRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FirewallEngine {
    public record FirewallDecision(FirewallRule.Action action, FirewallRule matchedRule, String reason) {}

    private final List<FirewallRule> rules;
    private final EventRepository eventRepo;

    public FirewallEngine(EventRepository eventRepo) {
        this.eventRepo = eventRepo;
        this.rules = new CopyOnWriteArrayList<>();
        initDefaultRules();
    }

    private void initDefaultRules() {
        rules.clear();
        // Priority 5: Restrict CCTV Surveillance stream from touching Dispatch or Receiving directly
        rules.add(new FirewallRule(
                "FW-30", "Block CCTV Cross-Talk to Dispatch Stations",
                "192.168.40.0/24", "192.168.30.0/24", "ANY", 0,
                FirewallRule.Action.DENY, 5,
                "Security policy: Surveillance cameras must not connect directly to dispatch workstations."
        ));

        // Priority 6: Block direct unauthorized external Telnet/Insecure ports
        rules.add(new FirewallRule(
                "FW-40", "Block Insecure Telnet Port 23",
                "ANY", "ANY", "TCP", 23,
                FirewallRule.Action.DENY, 6,
                "Deny plaintext telnet access across all warehouse subnets."
        ));

        // Priority 20: Allow Core Inventory, DNS and DHCP services to Central Server
        rules.add(new FirewallRule(
                "FW-20", "Allow Central Server Service Access",
                "192.168.0.0/16", "192.168.1.50/32", "ANY", 8080,
                FirewallRule.Action.ALLOW, 20,
                "Allow department terminals and scanners to query Central Inventory on Port 8080."
        ));
        rules.add(new FirewallRule(
                "FW-21", "Allow DNS Name Resolution",
                "ANY", "192.168.1.50/32", "UDP", 53,
                FirewallRule.Action.ALLOW, 21,
                "Permit local DNS lookups to warehouse server."
        ));

        // Priority 50: Allow intra-VLAN traffic (same subnet communication)
        rules.add(new FirewallRule(
                "FW-10", "Allow Intra-VLAN Department Traffic",
                "192.168.0.0/16", "192.168.0.0/16", "ANY", 0,
                FirewallRule.Action.ALLOW, 50,
                "Permit internal department devices communicating within the warehouse intranet."
        ));
    }

    public List<FirewallRule> getRules() {
        List<FirewallRule> copy = new ArrayList<>(rules);
        copy.sort(Comparator.comparingInt(FirewallRule::getPriority));
        return copy;
    }

    public void addRule(FirewallRule rule) {
        rules.add(rule);
    }

    public void removeRule(String ruleId) {
        rules.removeIf(r -> r.getRuleId().equals(ruleId));
    }

    public FirewallDecision evaluatePacket(NetworkPacket packet) {
        String srcIp = packet.getHeader().getSourceIp();
        String dstIp = packet.getHeader().getDestinationIp();
        String protocol = packet.getHeader().getProtocol();
        int dstPort = packet.getHeader().getDestinationPort();

        List<FirewallRule> sortedRules = getRules();

        for (FirewallRule rule : sortedRules) {
            if (!rule.isEnabled()) continue;

            if (matches(rule, srcIp, dstIp, protocol, dstPort)) {
                rule.incrementHitCount();
                if (rule.getAction() == FirewallRule.Action.DENY) {
                    String reason = "Firewall DENY [" + rule.getRuleId() + ": " + rule.getName() + "] for " + srcIp + " -> " + dstIp + ":" + dstPort + " (" + protocol + ")";
                    eventRepo.addEvent(new NetworkEvent(
                            "EVT-FW-BLOCK-" + System.currentTimeMillis() % 10000,
                            NetworkEvent.Severity.ALERT, "SECURITY", "Firewall", reason
                    ));
                    return new FirewallDecision(FirewallRule.Action.DENY, rule, reason);
                } else {
                    return new FirewallDecision(FirewallRule.Action.ALLOW, rule, "Matched ALLOW rule " + rule.getRuleId());
                }
            }
        }

        // Default Policy: ALLOW internal known subnets
        return new FirewallDecision(FirewallRule.Action.ALLOW, null, "Default Implicit Allow");
    }

    private boolean matches(FirewallRule rule, String srcIp, String dstIp, String protocol, int dstPort) {
        // Match Protocol
        if (!rule.getProtocol().equalsIgnoreCase("ANY") && !rule.getProtocol().equalsIgnoreCase(protocol)) {
            return false;
        }

        // Match Port
        if (rule.getDestinationPort() > 0 && rule.getDestinationPort() != dstPort) {
            return false;
        }

        // Match Subnet / IP
        if (!matchSubnetOrIp(rule.getSourceSubnet(), srcIp)) {
            return false;
        }
        if (!matchSubnetOrIp(rule.getDestinationSubnet(), dstIp)) {
            return false;
        }

        return true;
    }

    private boolean matchSubnetOrIp(String pattern, String ip) {
        if (pattern.equalsIgnoreCase("ANY")) return true;
        if (pattern.equals(ip) || pattern.startsWith(ip + "/")) return true;

        // Subnet check e.g. 192.168.40.0/24 or 192.168.0.0/16
        if (pattern.contains("/")) {
            String[] parts = pattern.split("/");
            String base = parts[0];
            int prefix = Integer.parseInt(parts[1]);
            if (prefix == 24) {
                String sub = base.substring(0, base.lastIndexOf('.'));
                return ip.startsWith(sub + ".");
            } else if (prefix == 16) {
                String[] oct = base.split("\\.");
                return ip.startsWith(oct[0] + "." + oct[1] + ".");
            } else if (prefix == 32) {
                return base.equals(ip);
            }
        }
        return false;
    }
}
