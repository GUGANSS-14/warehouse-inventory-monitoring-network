package com.wimn.models;

public class FirewallRule {
    public enum Action {
        ALLOW,
        DENY
    }

    private String ruleId;
    private String name;
    private String sourceSubnet;      // e.g. "192.168.40.0/24" or "ANY"
    private String destinationSubnet; // e.g. "192.168.1.0/24" or "ANY"
    private String protocol;          // "TCP", "UDP", "ICMP", "ANY"
    private int destinationPort;      // port number or 0 for ANY
    private Action action;
    private int priority;
    private boolean isEnabled;
    private long hitCount;
    private String description;

    public FirewallRule(String ruleId, String name, String sourceSubnet, String destinationSubnet,
                        String protocol, int destinationPort, Action action, int priority, String description) {
        this.ruleId = ruleId;
        this.name = name;
        this.sourceSubnet = sourceSubnet;
        this.destinationSubnet = destinationSubnet;
        this.protocol = protocol;
        this.destinationPort = destinationPort;
        this.action = action;
        this.priority = priority;
        this.isEnabled = true;
        this.hitCount = 0;
        this.description = description;
    }

    public String getRuleId() { return ruleId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSourceSubnet() { return sourceSubnet; }
    public void setSourceSubnet(String sourceSubnet) { this.sourceSubnet = sourceSubnet; }
    public String getDestinationSubnet() { return destinationSubnet; }
    public void setDestinationSubnet(String destinationSubnet) { this.destinationSubnet = destinationSubnet; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public int getDestinationPort() { return destinationPort; }
    public void setDestinationPort(int destinationPort) { this.destinationPort = destinationPort; }
    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
    public long getHitCount() { return hitCount; }
    public void incrementHitCount() { this.hitCount++; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
