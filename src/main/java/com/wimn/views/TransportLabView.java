package com.wimn.views;

import com.wimn.protocols.TCPSession;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

public class TransportLabView extends BorderPane {
    private final TCPSession tcpSession = new TCPSession();

    private final TableView<TCPSession.TCPExchange> tcpTable = new TableView<>();
    private final Label tcpStateLabel = new Label("CLOSED");
    private final Label cwndLabel = new Label("1 MSS");

    // TCP vs UDP Comparison Controls
    private final Slider compPayloadSlider = new Slider(64, 4096, 1024);
    private final Slider compLossSlider = new Slider(0, 20, 2);
    private final Label tcpThroughputLbl = new Label("0.0 Mbps");
    private final Label udpThroughputLbl = new Label("0.0 Mbps");
    private final Label tcpLatencyLbl = new Label("0.0 ms");
    private final Label udpLatencyLbl = new Label("0.0 ms");
    private final Label tcpDeliveryLbl = new Label("100.0% (Reliable)");
    private final Label udpDeliveryLbl = new Label("0.0%");

    public TransportLabView() {
        setPadding(new Insets(15));
        setCenter(createContent());

        setupTcpTable();
        runHandshake();
        runComparisonBenchmark();
    }

    private ScrollPane createContent() {
        VBox root = new VBox(20);
        root.setFillWidth(true);

        Label header = new Label("UNIT-IV: TRANSPORT LAYER PROTOCOLS & CONGESTION LAB");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        // 1. TCP 3-Way Handshake & Byte Stream Simulation
        VBox tcpBox = new VBox(12);
        tcpBox.getStyleClass().add("metric-card");

        Label tcpTitle = new Label("1. TCP PROTOCOL: 3-WAY HANDSHAKE, RELIABLE BYTE-STREAM & CONGESTION CONTROL");
        tcpTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10B981;");

        HBox tcpActionBar = new HBox(12);
        tcpActionBar.setAlignment(Pos.CENTER_LEFT);

        Button btnHandshake = new Button("1. Execute 3-Way Handshake (SYN → SYN-ACK → ACK)");
        btnHandshake.getStyleClass().add("btn-primary");
        btnHandshake.setOnAction(e -> runHandshake());

        Button btnDataTransfer = new Button("2. Send 6 Data Segments with Loss at Seg 3");
        btnDataTransfer.getStyleClass().add("btn-success");
        btnDataTransfer.setOnAction(e -> runDataTransferWithLoss());

        Button btnTeardown = new Button("3. Terminate Connection (FIN 4-Way Teardown)");
        btnTeardown.getStyleClass().add("btn-danger");
        btnTeardown.setOnAction(e -> runTeardown());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox stateBox = new HBox(8, new Label("TCP State:"), tcpStateLabel, new Label("| CWND:"), cwndLabel);
        stateBox.setAlignment(Pos.CENTER_LEFT);
        tcpStateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38BDF8;");
        cwndLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34D399;");

        tcpActionBar.getChildren().addAll(btnHandshake, btnDataTransfer, btnTeardown, spacer, stateBox);

        tcpBox.getChildren().addAll(tcpTitle, tcpActionBar, tcpTable);

        // 2. TCP vs UDP Live Benchmark Tool
        VBox compBox = new VBox(12);
        compBox.getStyleClass().add("metric-card");

        Label compTitle = new Label("2. TCP VS. UDP PERFORMANCE COMPARISON BENCHMARK TOOL");
        compTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #F59E0B;");

        GridPane compInputs = new GridPane();
        compInputs.setHgap(15);
        compInputs.setVgap(10);

        compInputs.add(new Label("Message Payload Size (Bytes):"), 0, 0);
        compPayloadSlider.setShowTickLabels(true);
        compPayloadSlider.setShowTickMarks(true);
        compPayloadSlider.setMajorTickUnit(1024);
        compInputs.add(compPayloadSlider, 1, 0);

        compInputs.add(new Label("Simulated Packet Loss Rate (%):"), 2, 0);
        compLossSlider.setShowTickLabels(true);
        compLossSlider.setShowTickMarks(true);
        compLossSlider.setMajorTickUnit(5);
        compInputs.add(compLossSlider, 3, 0);

        Button btnBenchmark = new Button("Run Live Comparison");
        btnBenchmark.getStyleClass().add("btn-primary");
        btnBenchmark.setOnAction(e -> runComparisonBenchmark());
        compInputs.add(btnBenchmark, 4, 0);

        // Comparison Output Cards Grid
        GridPane resultsGrid = new GridPane();
        resultsGrid.setHgap(20);
        resultsGrid.setVgap(12);
        resultsGrid.setPadding(new Insets(10, 0, 0, 0));

        resultsGrid.add(createCompCard("TCP THROUGHPUT", tcpThroughputLbl, "Flow & Congestion Limited", "#38BDF8"), 0, 0);
        resultsGrid.add(createCompCard("UDP THROUGHPUT", udpThroughputLbl, "Full Unthrottled Wire Speed", "#10B981"), 1, 0);
        resultsGrid.add(createCompCard("TCP LATENCY (RTT)", tcpLatencyLbl, "Includes ACKs & Retransmit", "#F59E0B"), 0, 1);
        resultsGrid.add(createCompCard("UDP LATENCY (RTT)", udpLatencyLbl, "Minimal One-Way Overhead", "#34D399"), 1, 1);
        resultsGrid.add(createCompCard("TCP RELIABILITY", tcpDeliveryLbl, "Guaranteed Ordered Delivery", "#10B981"), 0, 2);
        resultsGrid.add(createCompCard("UDP DELIVERY RATIO", udpDeliveryLbl, "Unreliable / Loss Tolerant", "#F87171"), 1, 2);

        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            resultsGrid.getColumnConstraints().add(cc);
        }

        compBox.getChildren().addAll(compTitle, compInputs, new Separator(), resultsGrid);

        // 3. SCTP Multi-Streaming Explanation Card
        VBox sctpBox = new VBox(8);
        sctpBox.getStyleClass().add("metric-card");
        Label sctpTitle = new Label("3. STREAM CONTROL TRANSMISSION PROTOCOL (SCTP RFC 4960)");
        sctpTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #A78BFA;");

        Label sctpDesc = new Label("""
            • Multi-Streaming: Transmits independent message streams over a single SCTP association, preventing Head-of-Line (HoL) blocking.
            • Multi-Homing: A single SCTP endpoint can bind to multiple warehouse network interfaces (e.g. Cat6A primary + Wi-Fi backup).
            • 4-Way Cookie-Based Handshake: Protects the central warehouse inventory server from SYN-flood denial of service attacks.
            """);
        sctpDesc.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 12px;");

        sctpBox.getChildren().addAll(sctpTitle, sctpDesc);

        root.getChildren().addAll(header, tcpBox, compBox, sctpBox);
        return new ScrollPane(root);
    }

    private VBox createCompCard(String title, Label valLbl, String desc, String colorHex) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: #0F172A; -fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #1E293B; -fx-border-radius: 6;");

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");

        valLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");

        Label d = new Label(desc);
        d.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        card.getChildren().addAll(t, valLbl, d);
        return card;
    }

    private void setupTcpTable() {
        tcpTable.setPrefHeight(200);
        tcpTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TCPSession.TCPExchange, Integer> stepCol = new TableColumn<>("Step");
        stepCol.setCellValueFactory(new PropertyValueFactory<>("step"));
        stepCol.setMaxWidth(50);

        TableColumn<TCPSession.TCPExchange, String> dirCol = new TableColumn<>("Direction");
        dirCol.setCellValueFactory(new PropertyValueFactory<>("direction"));

        TableColumn<TCPSession.TCPExchange, String> flagCol = new TableColumn<>("TCP Flags");
        flagCol.setCellValueFactory(new PropertyValueFactory<>("flags"));

        TableColumn<TCPSession.TCPExchange, Long> seqCol = new TableColumn<>("Seq Num");
        seqCol.setCellValueFactory(new PropertyValueFactory<>("seqNum"));

        TableColumn<TCPSession.TCPExchange, Long> ackCol = new TableColumn<>("Ack Num");
        ackCol.setCellValueFactory(new PropertyValueFactory<>("ackNum"));

        TableColumn<TCPSession.TCPExchange, Integer> cwndCol = new TableColumn<>("CWND");
        cwndCol.setCellValueFactory(new PropertyValueFactory<>("cwnd"));
        cwndCol.setMaxWidth(60);

        TableColumn<TCPSession.TCPExchange, String> descCol = new TableColumn<>("Protocol State Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        tcpTable.getColumns().addAll(stepCol, dirCol, flagCol, seqCol, ackCol, cwndCol, descCol);
    }

    private void runHandshake() {
        List<TCPSession.TCPExchange> exchanges = tcpSession.simulateThreeWayHandshake();
        tcpTable.setItems(FXCollections.observableArrayList(exchanges));
        tcpStateLabel.setText(tcpSession.getCurrentState().name());
        cwndLabel.setText(tcpSession.getCwnd() + " MSS");
    }

    private void runDataTransferWithLoss() {
        List<TCPSession.TCPExchange> exchanges = tcpSession.simulateDataTransferWithCongestionControl(6, 3);
        tcpTable.setItems(FXCollections.observableArrayList(exchanges));
        tcpStateLabel.setText(tcpSession.getCurrentState().name());
        cwndLabel.setText(tcpSession.getCwnd() + " MSS");
    }

    private void runTeardown() {
        List<TCPSession.TCPExchange> exchanges = tcpSession.simulateTeardown();
        tcpTable.setItems(FXCollections.observableArrayList(exchanges));
        tcpStateLabel.setText(tcpSession.getCurrentState().name());
        cwndLabel.setText("1 MSS");
    }

    private void runComparisonBenchmark() {
        int payload = (int) compPayloadSlider.getValue();
        double lossPercent = compLossSlider.getValue();

        // Calculate simulated TCP metrics
        double tcpLatency = 2.4 + (lossPercent * 1.8);
        double tcpThroughput = Math.max(10.0, 480.0 - (lossPercent * 22.0));
        tcpThroughputLbl.setText(String.format("%.1f Mbps", tcpThroughput));
        tcpLatencyLbl.setText(String.format("%.2f ms", tcpLatency));
        tcpDeliveryLbl.setText("100.0% (Reliable Byte Stream)");

        // Calculate simulated UDP metrics
        double udpLatency = 0.8;
        double udpThroughput = 520.0;
        double udpDelivery = Math.max(0.0, 100.0 - lossPercent);
        udpThroughputLbl.setText(String.format("%.1f Mbps", udpThroughput));
        udpLatencyLbl.setText(String.format("%.2f ms", udpLatency));
        udpDeliveryLbl.setText(String.format("%.1f%% (Best-Effort)", udpDelivery));
    }
}
