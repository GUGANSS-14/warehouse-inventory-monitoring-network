package com.wimn.models;

public class PacketHeader {
    private String sourceIp;
    private String destinationIp;
    private int sourcePort;
    private int destinationPort;
    private String protocol; // "TCP", "UDP", "ICMP", "HTTP", "DNS", "DHCP", "ARP"
    private int ttl;
    private long sequenceNumber;
    private long ackNumber;
    private int vlanId;
    private String tcpFlags; // "SYN", "SYN-ACK", "ACK", "PSH-ACK", "FIN-ACK"

    public PacketHeader(String sourceIp, String destinationIp, int sourcePort, int destinationPort,
                        String protocol, int ttl, long sequenceNumber, long ackNumber, int vlanId) {
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.protocol = protocol;
        this.ttl = ttl;
        this.sequenceNumber = sequenceNumber;
        this.ackNumber = ackNumber;
        this.vlanId = vlanId;
        this.tcpFlags = "";
    }

    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getDestinationIp() { return destinationIp; }
    public void setDestinationIp(String destinationIp) { this.destinationIp = destinationIp; }
    public int getSourcePort() { return sourcePort; }
    public void setSourcePort(int sourcePort) { this.sourcePort = sourcePort; }
    public int getDestinationPort() { return destinationPort; }
    public void setDestinationPort(int destinationPort) { this.destinationPort = destinationPort; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public int getTtl() { return ttl; }
    public void setTtl(int ttl) { this.ttl = ttl; }
    public long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(long sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public long getAckNumber() { return ackNumber; }
    public void setAckNumber(long ackNumber) { this.ackNumber = ackNumber; }
    public int getVlanId() { return vlanId; }
    public void setVlanId(int vlanId) { this.vlanId = vlanId; }
    public String getTcpFlags() { return tcpFlags; }
    public void setTcpFlags(String tcpFlags) { this.tcpFlags = tcpFlags; }

    public void decrementTtl() {
        if (ttl > 0) ttl--;
    }
}
