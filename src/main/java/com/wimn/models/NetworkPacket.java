package com.wimn.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NetworkPacket {
    public enum PacketStatus {
        QUEUED,
        IN_TRANSIT,
        DELIVERED,
        DROPPED,
        BLOCKED_BY_FIREWALL,
        CORRUPTED_CRC_ERROR
    }

    private String packetId;
    private String sourceDeviceId;
    private String destinationDeviceId;
    private PacketHeader header;
    private String payload;
    private int payloadSizeBytes;
    private PacketStatus status;
    private int currentHopIndex;
    private List<String> routedPath; // List of device IDs the packet traverses
    private String sendTimestamp;
    private String receiveTimestamp;
    private double calculatedLatencyMs;
    private String dropReason;
    private boolean isSimulatedBitFlipped;

    public NetworkPacket(String packetId, String sourceDeviceId, String destinationDeviceId,
                         PacketHeader header, String payload, int payloadSizeBytes) {
        this.packetId = packetId;
        this.sourceDeviceId = sourceDeviceId;
        this.destinationDeviceId = destinationDeviceId;
        this.header = header;
        this.payload = payload;
        this.payloadSizeBytes = payloadSizeBytes;
        this.status = PacketStatus.QUEUED;
        this.currentHopIndex = 0;
        this.routedPath = new ArrayList<>();
        this.sendTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        this.receiveTimestamp = null;
        this.calculatedLatencyMs = 0.0;
        this.dropReason = null;
        this.isSimulatedBitFlipped = false;
    }

    public String getPacketId() { return packetId; }
    public String getSourceDeviceId() { return sourceDeviceId; }
    public String getDestinationDeviceId() { return destinationDeviceId; }
    public PacketHeader getHeader() { return header; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public int getPayloadSizeBytes() { return payloadSizeBytes; }
    public PacketStatus getStatus() { return status; }
    public void setStatus(PacketStatus status) { this.status = status; }
    public int getCurrentHopIndex() { return currentHopIndex; }
    public void setCurrentHopIndex(int currentHopIndex) { this.currentHopIndex = currentHopIndex; }
    public List<String> getRoutedPath() { return routedPath; }
    public void setRoutedPath(List<String> routedPath) { this.routedPath = routedPath; }
    public String getSendTimestamp() { return sendTimestamp; }
    public String getReceiveTimestamp() { return receiveTimestamp; }
    public void setReceiveTimestamp(String receiveTimestamp) { this.receiveTimestamp = receiveTimestamp; }
    public double getCalculatedLatencyMs() { return calculatedLatencyMs; }
    public void setCalculatedLatencyMs(double latencyMs) { this.calculatedLatencyMs = latencyMs; }
    public String getDropReason() { return dropReason; }
    public void setDropReason(String dropReason) { this.dropReason = dropReason; }
    public boolean isSimulatedBitFlipped() { return isSimulatedBitFlipped; }
    public void setSimulatedBitFlipped(boolean bitFlipped) { this.isSimulatedBitFlipped = bitFlipped; }

    public String getCurrentDeviceAtHop() {
        if (routedPath == null || routedPath.isEmpty()) return sourceDeviceId;
        if (currentHopIndex >= routedPath.size()) return routedPath.get(routedPath.size() - 1);
        return routedPath.get(currentHopIndex);
    }

    public boolean advanceHop() {
        if (routedPath != null && currentHopIndex < routedPath.size() - 1) {
            currentHopIndex++;
            header.decrementTtl();
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "[" + packetId + "] " + header.getProtocol() + " " + sourceDeviceId + " -> " + destinationDeviceId + " (" + status + ")";
    }
}
