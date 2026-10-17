package com.wimn.models;

public class NetworkLink {
    private String linkId;
    private String sourceDeviceId;
    private String targetDeviceId;
    private double bandwidthMbps;
    private double latencyMs;
    private double packetLossRate;
    private LinkStatus status;
    private long packetsTransferred;
    private String mediumType; // "Fiber Optic", "Cat6A Copper", "Wi-Fi 6", "Bluetooth", "Zigbee"

    public NetworkLink(String linkId, String sourceDeviceId, String targetDeviceId,
                       double bandwidthMbps, double latencyMs, String mediumType) {
        this.linkId = linkId;
        this.sourceDeviceId = sourceDeviceId;
        this.targetDeviceId = targetDeviceId;
        this.bandwidthMbps = bandwidthMbps;
        this.latencyMs = latencyMs;
        this.packetLossRate = 0.001; // 0.1% baseline
        this.status = LinkStatus.UP;
        this.packetsTransferred = 0;
        this.mediumType = mediumType;
    }

    public String getLinkId() { return linkId; }
    public String getSourceDeviceId() { return sourceDeviceId; }
    public String getTargetDeviceId() { return targetDeviceId; }
    public double getBandwidthMbps() { return bandwidthMbps; }
    public void setBandwidthMbps(double bandwidthMbps) { this.bandwidthMbps = bandwidthMbps; }
    public double getLatencyMs() { return latencyMs; }
    public void setLatencyMs(double latencyMs) { this.latencyMs = latencyMs; }
    public double getPacketLossRate() { return packetLossRate; }
    public void setPacketLossRate(double packetLossRate) { this.packetLossRate = packetLossRate; }
    public LinkStatus getStatus() { return status; }
    public void setStatus(LinkStatus status) { this.status = status; }
    public long getPacketsTransferred() { return packetsTransferred; }
    public void incrementPackets() { this.packetsTransferred++; }
    public String getMediumType() { return mediumType; }
    public void setMediumType(String mediumType) { this.mediumType = mediumType; }

    public boolean connects(String devA, String devB) {
        return (sourceDeviceId.equals(devA) && targetDeviceId.equals(devB)) ||
               (sourceDeviceId.equals(devB) && targetDeviceId.equals(devA));
    }

    public String getOtherEnd(String deviceId) {
        if (sourceDeviceId.equals(deviceId)) return targetDeviceId;
        if (targetDeviceId.equals(deviceId)) return sourceDeviceId;
        return null;
    }
}
