package com.wimn.views;

import com.wimn.models.NetworkDevice;
import com.wimn.models.NetworkEvent;
import com.wimn.inventory.InventoryService;
import com.wimn.repositories.EventRepository;
import com.wimn.simulation.SimulationEngine;
import com.wimn.simulation.SimulationMetrics;
import com.wimn.topology.WarehouseTopology;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

public class DashboardView extends BorderPane {
    private final SimulationEngine simEngine;
    private final WarehouseTopology topology;
    private final InventoryService inventoryService;
    private final EventRepository eventRepo;

    // UI Labels to refresh
    private final Label clockLabel = new Label("00:00:00");
    private final Label totalDevicesLabel = new Label("0");
    private final Label onlineDevicesLabel = new Label("0 / 0");
    private final Label activeLinksLabel = new Label("0");
    private final Label availabilityLabel = new Label("100.0%");
    private final Label packetsSentLabel = new Label("0");
    private final Label packetsDeliveredLabel = new Label("0");
    private final Label packetsLostLabel = new Label("0");
    private final Label pdrLabel = new Label("100.0%");
    private final Label latencyLabel = new Label("0.0 ms");
    private final Label bandwidthLabel = new Label("0.0%");
    private final Label totalInventoryLabel = new Label("0 units");
    private final Label lowStockAlertsLabel = new Label("0 alerts");

    private final Button toggleSimButton = new Button();
    private final TableView<NetworkEvent> recentEventTable = new TableView<>();
    private final XYChart.Series<String, Number> latencySeries = new XYChart.Series<>();
    private PieChart deviceStatusChart;

    public DashboardView(SimulationEngine simEngine, WarehouseTopology topology,
                         InventoryService inventoryService, EventRepository eventRepo) {
        this.simEngine = simEngine;
        this.topology = topology;
        this.inventoryService = inventoryService;
        this.eventRepo = eventRepo;

        setPadding(new Insets(20));
        setCenter(createMainContent());

        // Register tick listener to refresh UI without freezing
        simEngine.addTickListener(() -> Platform.runLater(this::refreshMetrics));
        refreshMetrics();
    }

    private ScrollPane createMainContent() {
        VBox rootBox = new VBox(20);
        rootBox.setFillWidth(true);

        // 1. Top Controls Bar: Simulation Status, Clock, Start/Pause Button
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("metric-card");

        Label simTitle = new Label("SIMULATION ENGINE CLOCK:");
        simTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #94A3B8; -fx-font-size: 13px;");

        clockLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toggleSimButton.getStyleClass().add("btn-primary");
        updateSimButtonState();
        toggleSimButton.setOnAction(e -> {
            if (simEngine.isRunning()) {
                simEngine.pause();
            } else {
                simEngine.start();
            }
            updateSimButtonState();
        });

        Label statusTag = new Label("LIVE INDUSTRIAL NETWORK");
        statusTag.getStyleClass().add("header-badge");

        topBar.getChildren().addAll(simTitle, clockLabel, spacer, statusTag, toggleSimButton);

        // 2. Metric KPI Cards Row 1: Network Metrics
        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(15);
        kpiGrid.setVgap(15);

        kpiGrid.add(createCard("TOTAL CONNECTED DEVICES", totalDevicesLabel, "Warehouse Endpoints & Trunks", "#38BDF8"), 0, 0);
        kpiGrid.add(createCard("ONLINE STATUS", onlineDevicesLabel, "Operational Network Nodes", "#10B981"), 1, 0);
        kpiGrid.add(createCard("ACTIVE NETWORK LINKS", activeLinksLabel, "Fiber, Cat6A & Wireless Mesh", "#818CF8"), 2, 0);
        kpiGrid.add(createCard("NETWORK AVAILABILITY", availabilityLabel, "SLA Metric (Target: 99.9%)", "#10B981"), 3, 0);

        // KPI Row 2: Performance & Inventory Metrics
        kpiGrid.add(createCard("AVG PACKET LATENCY", latencyLabel, "Rolling End-to-End Delay", "#FBBF24"), 0, 1);
        kpiGrid.add(createCard("PACKET DELIVERY RATIO", pdrLabel, "Successful Packet Transmissions", "#34D399"), 1, 1);
        kpiGrid.add(createCard("CURRENT INVENTORY", totalInventoryLabel, "Items Across All Storage Racks", "#60A5FA"), 2, 1);
        kpiGrid.add(createCard("LOW-STOCK ALERTS", lowStockAlertsLabel, "Items Below Minimum Threshold", "#F87171"), 3, 1);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            kpiGrid.getColumnConstraints().add(cc);
        }

        // 3. Middle Section: Charts (Traffic Latency Trend & Device Health Doughnut)
        HBox chartsRow = new HBox(15);
        chartsRow.setFillHeight(true);

        // Latency Chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Simulation Samples");
        NumberAxis yAxis = new NumberAxis(0, 15, 2);
        yAxis.setLabel("Latency (ms)");
        LineChart<String, Number> latencyChart = new LineChart<>(xAxis, yAxis);
        latencyChart.setTitle("Real-Time Latency & Jitter Monitor");
        latencyChart.setLegendVisible(false);
        latencyChart.setAnimated(false);
        latencySeries.setName("Latency");
        latencyChart.getData().add(latencySeries);
        latencyChart.getStyleClass().add("metric-card");
        HBox.setHgrow(latencyChart, Priority.ALWAYS);

        // Device Health Pie Chart
        deviceStatusChart = new PieChart();
        deviceStatusChart.setTitle("Device Health Status");
        deviceStatusChart.getStyleClass().add("metric-card");
        deviceStatusChart.setPrefWidth(340);
        deviceStatusChart.setLegendVisible(true);

        chartsRow.getChildren().addAll(latencyChart, deviceStatusChart);

        // 4. Bottom Section: Recent Activity / Packet & Event Stream
        VBox tableBox = new VBox(10);
        tableBox.getStyleClass().add("metric-card");

        Label tableTitle = new Label("RECENT NETWORK & INVENTORY ACTIVITY LOGS");
        tableTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #F8FAFC;");

        setupRecentEventTable();
        tableBox.getChildren().addAll(tableTitle, recentEventTable);

        rootBox.getChildren().addAll(topBar, kpiGrid, chartsRow, tableBox);

        ScrollPane scroll = new ScrollPane(rootBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox createCard(String title, Label valueLabel, String subtitle, String accentColor) {
        VBox card = new VBox(6);
        card.getStyleClass().add("metric-card");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("card-title");

        valueLabel.getStyleClass().add("card-value");
        valueLabel.setStyle("-fx-text-fill: " + accentColor + ";");

        Label subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("card-subtitle");

        card.getChildren().addAll(titleLbl, valueLabel, subLbl);
        return card;
    }

    private void setupRecentEventTable() {
        recentEventTable.setPrefHeight(220);
        recentEventTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<NetworkEvent, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeCol.setMaxWidth(90);
        timeCol.setMinWidth(80);

        TableColumn<NetworkEvent, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setMaxWidth(110);
        catCol.setMinWidth(90);

        TableColumn<NetworkEvent, String> deptCol = new TableColumn<>("Department");
        deptCol.setCellValueFactory(new PropertyValueFactory<>("sourceDepartment"));
        deptCol.setMaxWidth(160);
        deptCol.setMinWidth(130);

        TableColumn<NetworkEvent, String> msgCol = new TableColumn<>("Event Description");
        msgCol.setCellValueFactory(new PropertyValueFactory<>("message"));

        recentEventTable.getColumns().addAll(timeCol, catCol, deptCol, msgCol);
    }

    private void updateSimButtonState() {
        if (simEngine.isRunning()) {
            toggleSimButton.setText("Pause Simulation");
            toggleSimButton.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: #1E293B;");
        } else {
            toggleSimButton.setText("Start Simulation");
            toggleSimButton.setStyle("-fx-background-color: #10B981; -fx-text-fill: #FFFFFF;");
        }
    }

    private int sampleCount = 0;

    public void refreshMetrics() {
        SimulationMetrics m = simEngine.getMetrics();
        clockLabel.setText(simEngine.getFormattedSimulationClock());

        int totalDev = topology.getTotalDeviceCount();
        int onlineDev = topology.getOnlineDeviceCount();
        totalDevicesLabel.setText(String.valueOf(totalDev));
        onlineDevicesLabel.setText(onlineDev + " / " + totalDev + " Online");
        activeLinksLabel.setText(topology.getActiveLinkCount() + " / " + topology.getLinks().size());

        double availability = totalDev > 0 ? ((double) onlineDev / totalDev) * 100.0 : 100.0;
        availabilityLabel.setText(String.format("%.1f%%", availability));

        packetsSentLabel.setText(String.valueOf(m.getPacketsSent()));
        packetsDeliveredLabel.setText(String.valueOf(m.getPacketsDelivered()));
        packetsLostLabel.setText(String.valueOf(m.getPacketsDropped() + m.getPacketsBlocked()));
        pdrLabel.setText(String.format("%.2f%%", m.getPacketDeliveryRatio()));

        latencyLabel.setText(String.format("%.2f ms", m.getCurrentLatencyMs()));
        bandwidthLabel.setText(String.format("%.1f%%", m.getBandwidthUtilizationPercent()));

        Map<String, Object> invSummary = inventoryService.getConsolidatedWarehouseSummary();
        totalInventoryLabel.setText(invSummary.get("totalStockUnits") + " Units");
        lowStockAlertsLabel.setText(invSummary.get("lowStockAlertsCount") + " Low Stock");

        // Update Latency Chart
        sampleCount++;
        latencySeries.getData().add(new XYChart.Data<>("S" + sampleCount, m.getCurrentLatencyMs()));
        if (latencySeries.getData().size() > 15) {
            latencySeries.getData().remove(0);
        }

        // Update Device Health Doughnut
        int offlineDev = totalDev - onlineDev;
        deviceStatusChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Online (" + onlineDev + ")", onlineDev),
                new PieChart.Data("Offline (" + offlineDev + ")", offlineDev)
        ));

        // Update Events
        List<NetworkEvent> recent = eventRepo.getRecentEvents(12);
        recentEventTable.setItems(FXCollections.observableArrayList(recent));
    }
}
