package com.wimn.models;

public class SubnetInfo {
    private String inputIp;
    private int prefixLength;
    private String networkAddress;
    private String subnetMask;
    private String broadcastAddress;
    private String firstUsableHost;
    private String lastUsableHost;
    private long totalAddresses;
    private long usableHosts;
    private String ipClass;
    private String binarySubnetMask;
    private String binaryIp;
    private String cidrNotation;
    private String wildCardMask;

    public SubnetInfo() {}

    public String getInputIp() { return inputIp; }
    public void setInputIp(String inputIp) { this.inputIp = inputIp; }
    public int getPrefixLength() { return prefixLength; }
    public void setPrefixLength(int prefixLength) { this.prefixLength = prefixLength; }
    public String getNetworkAddress() { return networkAddress; }
    public void setNetworkAddress(String networkAddress) { this.networkAddress = networkAddress; }
    public String getSubnetMask() { return subnetMask; }
    public void setSubnetMask(String subnetMask) { this.subnetMask = subnetMask; }
    public String getBroadcastAddress() { return broadcastAddress; }
    public void setBroadcastAddress(String broadcastAddress) { this.broadcastAddress = broadcastAddress; }
    public String getFirstUsableHost() { return firstUsableHost; }
    public void setFirstUsableHost(String firstUsableHost) { this.firstUsableHost = firstUsableHost; }
    public String getLastUsableHost() { return lastUsableHost; }
    public void setLastUsableHost(String lastUsableHost) { this.lastUsableHost = lastUsableHost; }
    public long getTotalAddresses() { return totalAddresses; }
    public void setTotalAddresses(long totalAddresses) { this.totalAddresses = totalAddresses; }
    public long getUsableHosts() { return usableHosts; }
    public void setUsableHosts(long usableHosts) { this.usableHosts = usableHosts; }
    public String getIpClass() { return ipClass; }
    public void setIpClass(String ipClass) { this.ipClass = ipClass; }
    public String getBinarySubnetMask() { return binarySubnetMask; }
    public void setBinarySubnetMask(String binarySubnetMask) { this.binarySubnetMask = binarySubnetMask; }
    public String getBinaryIp() { return binaryIp; }
    public void setBinaryIp(String binaryIp) { this.binaryIp = binaryIp; }
    public String getCidrNotation() { return cidrNotation; }
    public void setCidrNotation(String cidrNotation) { this.cidrNotation = cidrNotation; }
    public String getWildCardMask() { return wildCardMask; }
    public void setWildCardMask(String wildCardMask) { this.wildCardMask = wildCardMask; }
}
