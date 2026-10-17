package com.wimn.simulation;

import com.wimn.models.*;
import com.wimn.repositories.EventRepository;
import com.wimn.routing.RoutingEngine;
import com.wimn.security.FirewallEngine;
import com.wimn.topology.WarehouseTopology;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimulationEngine {
    private static final Logger LOGGER = Logger.getLogger(SimulationEngine.class.getName());

    private final WarehouseTopology topology;
    private final RoutingEngine routingEngine;
    private final FirewallEngine firewallEngine;
    private final EventRepository eventRepo;
    private final SimulationMetrics metrics;

    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> simulationTask;
    private volatile boolean isRunning = false;

    private final List<Consumer<NetworkPacket>> packetListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> tickListeners = new CopyOnWriteArrayList<>();

    private int simulationClockSeconds = 3600; // Simulated warehouse operating time

    public SimulationEngine(WarehouseTopology topology, RoutingEngine routingEngine,
                            FirewallEngine firewallEngine, EventRepository eventRepo) {
        this.topology = topology;
        this.routingEngine = routingEngine;
        this.firewallEngine = firewallEngine;
        this.eventRepo = eventRepo;
        this.metrics = new SimulationMetrics();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WIMN-Sim-Engine");
            t.setDaemon(true);
            return t;
        });

        // Start running by default as requested by specifications
        start();
    }

    public synchronized void start() {
        if (!isRunning) {
            isRunning = true;
            simulationTask = scheduler.scheduleAtFixedRate(this::simulationTick, 1, 2, TimeUnit.SECONDS);
            eventRepo.addEvent(new NetworkEvent(
                    "EVT-SIM-START-" + System.currentTimeMillis() % 1000,
                    NetworkEvent.Severity.SUCCESS, "SIMULATION", "Simulation Engine",
                    "Simulation started. Network traffic generation active."
            ));
        }
    }

    public synchronized void pause() {
        if (isRunning) {
            isRunning = false;
            if (simulationTask != null) {
                simulationTask.cancel(false);
            }
            eventRepo.addEvent(new NetworkEvent(
                    "EVT-SIM-PAUSE-" + System.currentTimeMillis() % 1000,
                    NetworkEvent.Severity.WARNING, "SIMULATION", "Simulation Engine",
                    "Simulation paused. Traffic generation halted."
            ));
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public SimulationMetrics getMetrics() {
        return metrics;
    }

    public int getSimulationClockSeconds() {
        return simulationClockSeconds;
    }

    public String getFormattedSimulationClock() {
        int hours = (simulationClockSeconds / 3600) % 24;
        int mins = (simulationClockSeconds / 60) % 60;
        int secs = simulationClockSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    public void addPacketListener(Consumer<NetworkPacket> listener) {
        packetListeners.add(listener);
    }

    public void addTickListener(Runnable listener) {
        tickListeners.add(listener);
    }

    /**
     * Core packet transmission logic traversing firewall, routing, and links
     */
    public NetworkPacket transmitPacket(NetworkPacket packet) {
        metrics.incrementSent();

        NetworkDevice src = topology.getDevice(packet.getSourceDeviceId());
        NetworkDevice dst = topology.getDevice(packet.getDestinationDeviceId());

        if (src == null || dst == null) {
            packet.setStatus(NetworkPacket.PacketStatus.DROPPED);
            packet.setDropReason("Unknown source or destination device ID");
            metrics.incrementDropped();
            notifyPacket(packet);
            return packet;
        }

        // 1. Firewall Evaluation
        FirewallEngine.FirewallDecision decision = firewallEngine.evaluatePacket(packet);
        if (decision.action() == FirewallRule.Action.DENY) {
            packet.setStatus(NetworkPacket.PacketStatus.BLOCKED_BY_FIREWALL);
            packet.setDropReason(decision.reason());
            metrics.incrementBlocked();
            notifyPacket(packet);
            return packet;
        }

        // 2. Routing Evaluation via Dijkstra Shortest Path
        RoutingEngine.DijkstraResult routingResult = routingEngine.findShortestPath(src.getId(), dst.getId());
        if (!routingResult.pathFound()) {
            packet.setStatus(NetworkPacket.PacketStatus.DROPPED);
            packet.setDropReason("Destination unreachable. Link failure or offline node in path.");
            metrics.incrementDropped();
            eventRepo.addEvent(new NetworkEvent(
                    "EVT-PKT-DROP-" + packet.getPacketId(),
                    NetworkEvent.Severity.ALERT, "NETWORK", src.getDepartment().getDisplayName(),
                    "Packet " + packet.getPacketId() + " dropped: Destination " + dst.getId() + " unreachable."
            ));
            notifyPacket(packet);
            return packet;
        }

        // 3. Set path and calculate transmission characteristics
        packet.setRoutedPath(routingResult.path());
        packet.setStatus(NetworkPacket.PacketStatus.IN_TRANSIT);

        // Check if any link in the path has bit-errors or CRC corruption
        boolean hasBitError = packet.isSimulatedBitFlipped();

        double totalHopLatency = 0.0;
        for (int i = 0; i < routingResult.path().size() - 1; i++) {
            String u = routingResult.path().get(i);
            String v = routingResult.path().get(i + 1);
            NetworkLink link = topology.findLinkBetween(u, v);
            if (link != null) {
                link.incrementPackets();
                totalHopLatency += link.getLatencyMs();
            }
        }

        packet.setCalculatedLatencyMs(totalHopLatency);
        metrics.recordLatency(totalHopLatency);

        if (hasBitError) {
            packet.setStatus(NetworkPacket.PacketStatus.CORRUPTED_CRC_ERROR);
            packet.setDropReason("CRC-32 Checksum mismatch: Bit inversion detected in link framing.");
            metrics.incrementDropped();
            eventRepo.addEvent(new NetworkEvent(
                    "EVT-CRC-ERR-" + packet.getPacketId(),
                    NetworkEvent.Severity.WARNING, "DATA LINK", "Layer 2",
                    "Corrupted frame detected for packet " + packet.getPacketId() + " - Frame discarded."
            ));
        } else {
            packet.setStatus(NetworkPacket.PacketStatus.DELIVERED);
            packet.setReceiveTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
            metrics.incrementDelivered();
            src.touchCommunication();
            dst.touchCommunication();
        }

        notifyPacket(packet);
        return packet;
    }

    private void notifyPacket(NetworkPacket p) {
        for (Consumer<NetworkPacket> listener : packetListeners) {
            try {
                listener.accept(p);
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error in packet listener", ex);
            }
        }
    }

    private void simulationTick() {
        try {
            simulationClockSeconds++;

            // Random warehouse network event simulation
            int roll = ThreadLocalRandom.current().nextInt(100);
            if (roll < 35) {
                // Barcode scanner transmission from Receiving to Central Server
                PacketHeader hdr = new PacketHeader("192.168.10.21", "192.168.1.50", 49152, 8080, "TCP", 64, 1001, 2001, 10);
                hdr.setTcpFlags("PSH-ACK");
                NetworkPacket pkt = new NetworkPacket("PKT-SCAN-" + (System.currentTimeMillis() % 10000), "SCN-RCV-01", "SRV-MAIN-01", hdr, "BARCODE:SKU-NET-03:QTY:5", 128);
                transmitPacket(pkt);
            } else if (roll < 65) {
                // Storage RFID telemetry to Server
                PacketHeader hdr = new PacketHeader("192.168.20.31", "192.168.1.50", 49154, 8080, "TCP", 64, 3045, 4045, 20);
                NetworkPacket pkt = new NetworkPacket("PKT-RFID-" + (System.currentTimeMillis() % 10000), "RFID-STR-01", "SRV-MAIN-01", hdr, "RFID_TAG:RACK-B2-01:STATUS_OK", 96);
                transmitPacket(pkt);
            } else if (roll < 85) {
                // Security CCTV RTSP UDP stream segment
                PacketHeader hdr = new PacketHeader("192.168.40.21", "192.168.40.11", 554, 5004, "UDP", 64, 0, 0, 40);
                NetworkPacket pkt = new NetworkPacket("PKT-CAM-" + (System.currentTimeMillis() % 10000), "CAM-SEC-01", "WS-SEC-01", hdr, "RTSP_H265_FRAME_CHUNK", 1460);
                transmitPacket(pkt);
            } else {
                // Manager telemetry request (HTTP GET)
                PacketHeader hdr = new PacketHeader("192.168.50.11", "192.168.1.50", 52100, 8080, "HTTP", 64, 800, 900, 50);
                NetworkPacket pkt = new NetworkPacket("PKT-MGR-" + (System.currentTimeMillis() % 10000), "WS-MGR-01", "SRV-MAIN-01", hdr, "GET /api/kpi/summary HTTP/1.1", 64);
                transmitPacket(pkt);
            }

            // Fluctuate bandwidth & throughput slightly for realism
            double bwVariation = 30.0 + (ThreadLocalRandom.current().nextDouble() * 20.0);
            metrics.setBandwidthUtilization(bwVariation);
            metrics.setThroughput(280.0 + (ThreadLocalRandom.current().nextDouble() * 120.0));

            for (Runnable r : tickListeners) {
                try {
                    r.run();
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in simulation tick", e);
        }
    }
}
