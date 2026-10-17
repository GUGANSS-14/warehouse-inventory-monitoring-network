package com.wimn.simulation;

import java.util.concurrent.atomic.AtomicLong;

public class SimulationMetrics {
    private final AtomicLong packetsSent = new AtomicLong(1280);
    private final AtomicLong packetsDelivered = new AtomicLong(1262);
    private final AtomicLong packetsDropped = new AtomicLong(12);
    private final AtomicLong packetsBlocked = new AtomicLong(6);
    private double currentLatencyMs = 2.45;
    private double currentThroughputMbps = 345.8;
    private double bandwidthUtilizationPercent = 38.5;
    private double jitterMs = 0.42;

    public long getPacketsSent() { return packetsSent.get(); }
    public long getPacketsDelivered() { return packetsDelivered.get(); }
    public long getPacketsDropped() { return packetsDropped.get(); }
    public long getPacketsBlocked() { return packetsBlocked.get(); }

    public void incrementSent() { packetsSent.incrementAndGet(); }
    public void incrementDelivered() { packetsDelivered.incrementAndGet(); }
    public void incrementDropped() { packetsDropped.incrementAndGet(); }
    public void incrementBlocked() { packetsBlocked.incrementAndGet(); }

    public double getPacketDeliveryRatio() {
        long s = packetsSent.get();
        if (s == 0) return 100.0;
        return ((double) packetsDelivered.get() / s) * 100.0;
    }

    public double getCurrentLatencyMs() { return currentLatencyMs; }
    public void recordLatency(double latency) {
        this.jitterMs = Math.abs(latency - this.currentLatencyMs);
        this.currentLatencyMs = (this.currentLatencyMs * 0.85) + (latency * 0.15);
    }

    public double getCurrentThroughputMbps() { return currentThroughputMbps; }
    public void setThroughput(double mbps) {
        this.currentThroughputMbps = (this.currentThroughputMbps * 0.9) + (mbps * 0.1);
    }

    public double getBandwidthUtilizationPercent() { return bandwidthUtilizationPercent; }
    public void setBandwidthUtilization(double percent) {
        this.bandwidthUtilizationPercent = (this.bandwidthUtilizationPercent * 0.9) + (percent * 0.1);
    }

    public double getJitterMs() { return jitterMs; }
}
