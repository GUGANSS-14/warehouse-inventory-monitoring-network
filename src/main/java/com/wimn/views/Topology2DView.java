package com.wimn.views;

import com.wimn.models.*;
import com.wimn.routing.RoutingEngine;
import com.wimn.simulation.SimulationEngine;
import com.wimn.topology.WarehouseTopology;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.*;

public class Topology2DView extends BorderPane {
    private final WarehouseTopology topology;
    private final RoutingEngine routingEngine;
    private final SimulationEngine simEngine;

    private final Canvas canvas = new Canvas(920, 620);
    private final ComboBox<NetworkDevice> srcCombo = new ComboBox<>();
    private final ComboBox<NetworkDevice> dstCombo = new ComboBox<>();
    private final ComboBox<String> protocolCombo = new ComboBox<>();
    private final TextArea traceConsole = new TextArea();

    // Device Inspector Labels
    private final Label inspectName = new Label("Select a node on canvas");
    private final Label inspectIp = new Label("-");
    private final Label inspectMac = new Label("-");
    private final Label inspectVlan = new Label("-");
    private final Label inspectGateway = new Label("-");
    private final Label inspectType = new Label("-");
    private final Label inspectStatus = new Label("-");
    private final Label inspectUtil = new Label("-");
    private NetworkDevice selectedDevice = null;

    // Active packet animation particles
    private final List<AnimatedParticle> activeParticles = new ArrayList<>();

    public Topology2DView(WarehouseTopology topology, RoutingEngine routingEngine, SimulationEngine simEngine) {
        this.topology = topology;
        this.routingEngine = routingEngine;
        this.simEngine = simEngine;

        setPadding(new Insets(15));
        setTop(createControlBar());
        setCenter(createCanvasArea());
        setRight(createInspectorSidebar());

        // Listen for packets from simulation engine to animate
        simEngine.addPacketListener(this::handlePacketAnimation);

        setupCanvasInteractions();
        startRenderLoop();
    }

    private HBox createControlBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 12, 0));
        bar.getStyleClass().add("metric-card");

        Label srcLbl = new Label("Source:");
        srcLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: bold;");
        srcCombo.setPrefWidth(220);

        Label dstLbl = new Label("Destination:");
        dstLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: bold;");
        dstCombo.setPrefWidth(220);

        protocolCombo.setItems(FXCollections.observableArrayList("TCP (Port 8080)", "UDP (Port 5004)", "ICMP (Ping)", "HTTP GET"));
        protocolCombo.setValue("TCP (Port 8080)");

        Button btnSend = new Button("Send Test Packet");
        btnSend.getStyleClass().add("btn-primary");
        btnSend.setOnAction(e -> sendInteractivePacket());

        Button btnReset = new Button("Reset Topology");
        btnReset.getStyleClass().add("btn-secondary");
        btnReset.setOnAction(e -> {
            for (NetworkLink l : topology.getLinks()) l.setStatus(LinkStatus.UP);
            for (NetworkDevice d : topology.getDevices()) d.setOnline(true);
            traceConsole.setText("[RESET] All devices and links restored to healthy status.");
        });

        populateDeviceCombos();

        bar.getChildren().addAll(srcLbl, srcCombo, dstLbl, dstCombo, protocolCombo, btnSend, btnReset);
        return bar;
    }

    private Pane createCanvasArea() {
        StackPane pane = new StackPane();
        pane.setStyle("-fx-background-color: #0B132B; -fx-border-color: #1E293B; -fx-border-radius: 8;");
        pane.getChildren().add(canvas);

        // Bind canvas resizing
        pane.widthProperty().addListener((obs, oldV, newV) -> {
            canvas.setWidth(newV.doubleValue() - 10);
            render();
        });
        pane.heightProperty().addListener((obs, oldV, newV) -> {
            canvas.setHeight(newV.doubleValue() - 10);
            render();
        });

        return pane;
    }

    private VBox createInspectorSidebar() {
        VBox box = new VBox(15);
        box.setPrefWidth(320);
        box.setPadding(new Insets(0, 0, 0, 15));

        VBox inspectorCard = new VBox(8);
        inspectorCard.getStyleClass().add("metric-card");

        Label cardTitle = new Label("DEVICE INSPECTION PANEL");
        cardTitle.getStyleClass().add("card-title");

        inspectName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);

        grid.add(new Label("Type:"), 0, 0); grid.add(inspectType, 1, 0);
        grid.add(new Label("IPv4:"), 0, 1); grid.add(inspectIp, 1, 1);
        grid.add(new Label("MAC:"), 0, 2); grid.add(inspectMac, 1, 2);
        grid.add(new Label("VLAN:"), 0, 3); grid.add(inspectVlan, 1, 3);
        grid.add(new Label("Gateway:"), 0, 4); grid.add(inspectGateway, 1, 4);
        grid.add(new Label("Status:"), 0, 5); grid.add(inspectStatus, 1, 5);
        grid.add(new Label("Load:"), 0, 6); grid.add(inspectUtil, 1, 6);

        inspectorCard.getChildren().addAll(cardTitle, inspectName, grid);

        // Console for traces
        VBox traceBox = new VBox(8);
        traceBox.getStyleClass().add("metric-card");
        Label traceTitle = new Label("PACKET TRANSMISSION & ROUTE TRACE");
        traceTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        traceConsole.setEditable(false);
        traceConsole.setPrefHeight(250);
        traceConsole.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        traceConsole.setText("Click 'Send Test Packet' or tap any node to inspect telemetry.");

        traceBox.getChildren().addAll(traceTitle, traceConsole);
        box.getChildren().addAll(inspectorCard, traceBox);
        return box;
    }

    private void populateDeviceCombos() {
        List<NetworkDevice> devs = new ArrayList<>(topology.getDevices());
        devs.sort(Comparator.comparing(NetworkDevice::getId));
        srcCombo.setItems(FXCollections.observableArrayList(devs));
        dstCombo.setItems(FXCollections.observableArrayList(devs));

        if (devs.size() >= 2) {
            srcCombo.setValue(devs.stream().filter(d -> d.getId().equals("WS-RCV-01")).findFirst().orElse(devs.get(0)));
            dstCombo.setValue(devs.stream().filter(d -> d.getId().equals("SRV-MAIN-01")).findFirst().orElse(devs.get(1)));
        }
    }

    private void sendInteractivePacket() {
        NetworkDevice src = srcCombo.getValue();
        NetworkDevice dst = dstCombo.getValue();
        if (src == null || dst == null) return;

        String protoChoice = protocolCombo.getValue();
        String proto = protoChoice.startsWith("TCP") ? "TCP" : protoChoice.startsWith("UDP") ? "UDP" : protoChoice.startsWith("HTTP") ? "HTTP" : "ICMP";
        int dPort = proto.equals("TCP") ? 8080 : proto.equals("UDP") ? 5004 : 80;

        PacketHeader hdr = new PacketHeader(src.getIpv4(), dst.getIpv4(), 52000, dPort, proto, 64, 100, 200, src.getVlanId());
        NetworkPacket pkt = new NetworkPacket("PKT-USER-" + (System.currentTimeMillis() % 10000), src.getId(), dst.getId(), hdr, "USER_TEST_PAYLOAD", 128);

        simEngine.transmitPacket(pkt);

        traceConsole.setText("=".repeat(50));
        traceConsole.appendText("\n[TRANSMISSION INITIATED] " + pkt.getPacketId());
        traceConsole.appendText("\nSource: " + src.getId() + " (" + src.getIpv4() + ")");
        traceConsole.appendText("\nDestination: " + dst.getId() + " (" + dst.getIpv4() + ")");
        traceConsole.appendText("\nProtocol: " + proto + " | Port: " + dPort);
        traceConsole.appendText("\nStatus: " + pkt.getStatus());

        if (pkt.getRoutedPath() != null && !pkt.getRoutedPath().isEmpty()) {
            traceConsole.appendText("\nPath Hops (" + pkt.getRoutedPath().size() + " nodes):");
            for (int i = 0; i < pkt.getRoutedPath().size(); i++) {
                traceConsole.appendText("\n  [" + (i + 1) + "] " + pkt.getRoutedPath().get(i));
            }
            traceConsole.appendText("\nCalculated Latency: " + String.format("%.2f ms", pkt.getCalculatedLatencyMs()));
        } else if (pkt.getDropReason() != null) {
            traceConsole.appendText("\nDrop Reason: " + pkt.getDropReason());
        }
    }

    private void handlePacketAnimation(NetworkPacket pkt) {
        if (pkt.getRoutedPath() != null && pkt.getRoutedPath().size() >= 2) {
            List<NetworkDevice> pathNodes = new ArrayList<>();
            for (String devId : pkt.getRoutedPath()) {
                NetworkDevice d = topology.getDevice(devId);
                if (d != null) pathNodes.add(d);
            }
            if (pathNodes.size() >= 2) {
                Color color = pkt.getStatus() == NetworkPacket.PacketStatus.DELIVERED ? Color.web("#00F5D4") :
                              pkt.getStatus() == NetworkPacket.PacketStatus.BLOCKED_BY_FIREWALL ? Color.web("#EF4444") : Color.web("#F59E0B");
                synchronized (activeParticles) {
                    activeParticles.add(new AnimatedParticle(pathNodes, color));
                }
            }
        }
    }

    private void setupCanvasInteractions() {
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            double mx = e.getX();
            double my = e.getY();
            for (NetworkDevice d : topology.getDevices()) {
                double dx = d.getPosX() - mx;
                double dy = d.getPosY() - my;
                if (dx * dx + dy * dy <= 22 * 22) { // 22px click radius
                    selectDevice(d);
                    break;
                }
            }
        });
    }

    private void selectDevice(NetworkDevice d) {
        selectedDevice = d;
        inspectName.setText(d.getName());
        inspectType.setText(d.getDeviceType().getTitle());
        inspectIp.setText(d.getIpv4() + " (" + d.getDepartment().getDisplayName() + ")");
        inspectMac.setText(d.getMacAddress());
        inspectVlan.setText("VLAN " + d.getVlanId() + " (" + d.getSubnetMask() + ")");
        inspectGateway.setText(d.getDefaultGateway());
        inspectStatus.setText(d.isOnline() ? "ONLINE (Active)" : "OFFLINE (Link Disrupted)");
        inspectStatus.setStyle(d.isOnline() ? "-fx-text-fill: #10B981; -fx-font-weight: bold;" : "-fx-text-fill: #EF4444; -fx-font-weight: bold;");
        inspectUtil.setText(String.format("%.1f%% Bandwidth", d.getUtilizationPercent()));
        render();
    }

    private void startRenderLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateParticles();
                render();
            }
        };
        timer.start();
    }

    private void updateParticles() {
        synchronized (activeParticles) {
            Iterator<AnimatedParticle> it = activeParticles.iterator();
            while (it.hasNext()) {
                AnimatedParticle p = it.next();
                p.progress += 0.025;
                if (p.progress >= 1.0) {
                    p.currentSegment++;
                    p.progress = 0.0;
                    if (p.currentSegment >= p.path.size() - 1) {
                        it.remove();
                    }
                }
            }
        }
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.clearRect(0, 0, w, h);

        // Draw Links
        for (NetworkLink link : topology.getLinks()) {
            NetworkDevice src = topology.getDevice(link.getSourceDeviceId());
            NetworkDevice dst = topology.getDevice(link.getTargetDeviceId());
            if (src == null || dst == null) continue;

            Color linkColor = switch (link.getStatus()) {
                case UP -> Color.web(link.getMediumType().contains("Redundant") ? "#6366F1" : "#10B981", 0.7);
                case CONGESTED -> Color.web("#F59E0B", 0.9);
                case DOWN -> Color.web("#EF4444", 0.9);
            };

            gc.setStroke(linkColor);
            gc.setLineWidth(link.getMediumType().contains("10G") ? 3.5 : 2.0);
            if (link.getMediumType().contains("Wi-Fi") || link.getMediumType().contains("Zigbee")) {
                gc.setLineDashes(6.0);
            } else {
                gc.setLineDashes(null);
            }
            gc.strokeLine(src.getPosX(), src.getPosY(), dst.getPosX(), dst.getPosY());

            // Draw link latency label
            double midX = (src.getPosX() + dst.getPosX()) / 2.0;
            double midY = (src.getPosY() + dst.getPosY()) / 2.0;
            gc.setFill(Color.web("#94A3B8", 0.6));
            gc.fillText(String.format("%.1fms", link.getLatencyMs()), midX + 4, midY - 4);
        }

        // Draw Animated Particles
        synchronized (activeParticles) {
            for (AnimatedParticle p : activeParticles) {
                if (p.currentSegment < p.path.size() - 1) {
                    NetworkDevice from = p.path.get(p.currentSegment);
                    NetworkDevice to = p.path.get(p.currentSegment + 1);
                    double px = from.getPosX() + (to.getPosX() - from.getPosX()) * p.progress;
                    double py = from.getPosY() + (to.getPosY() - from.getPosY()) * p.progress;

                    gc.setFill(p.color);
                    gc.fillOval(px - 6, py - 6, 12, 12);
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(1.5);
                    gc.strokeOval(px - 6, py - 6, 12, 12);
                }
            }
        }

        // Draw Nodes
        for (NetworkDevice dev : topology.getDevices()) {
            double x = dev.getPosX();
            double y = dev.getPosY();
            double radius = dev.getDeviceType() == DeviceType.SWITCH || dev.getDeviceType() == DeviceType.ROUTER || dev.getDeviceType() == DeviceType.SERVER ? 18 : 14;

            boolean isSelected = dev.equals(selectedDevice);

            // Selection ring
            if (isSelected) {
                gc.setStroke(Color.web("#38BDF8"));
                gc.setLineWidth(3);
                gc.strokeOval(x - radius - 5, y - radius - 5, (radius + 5) * 2, (radius + 5) * 2);
            }

            // Node body
            Color nodeColor = !dev.isOnline() ? Color.web("#DC2626") :
                    switch (dev.getDeviceType()) {
                        case ROUTER -> Color.web("#3B82F6");
                        case SWITCH -> Color.web("#06B6D4");
                        case SERVER -> Color.web("#8B5CF6");
                        case ACCESS_POINT -> Color.web("#10B981");
                        case CCTV_CAMERA -> Color.web("#EF4444");
                        case BARCODE_SCANNER, RFID_READER -> Color.web("#F59E0B");
                        default -> Color.web("#64748B");
                    };

            gc.setFill(nodeColor);
            gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            gc.setStroke(Color.web("#0F172A"));
            gc.setLineWidth(2);
            gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);

            // Node ID & IP Labels
            gc.setFill(Color.web("#F8FAFC"));
            gc.fillText(dev.getId(), x - 20, y - radius - 4);
            gc.setFill(Color.web("#94A3B8"));
            gc.fillText(dev.getIpv4(), x - 26, y + radius + 14);
        }
    }

    private static class AnimatedParticle {
        final List<NetworkDevice> path;
        final Color color;
        int currentSegment = 0;
        double progress = 0.0;

        AnimatedParticle(List<NetworkDevice> path, Color color) {
            this.path = path;
            this.color = color;
        }
    }
}
