package com.wimn.views;

import com.wimn.repositories.DatabaseManager;
import com.wimn.utilities.ThemeManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class SettingsAboutView extends BorderPane {

    public record SyllabusMappingRow(String unit, String syllabusTopic, String applicationModule, String mappedCO, String status) {
        public String getUnit() { return unit; }
        public String getSyllabusTopic() { return syllabusTopic; }
        public String getApplicationModule() { return applicationModule; }
        public String getMappedCO() { return mappedCO; }
        public String getStatus() { return status; }
    }

    private final TableView<SyllabusMappingRow> mappingTable = new TableView<>();

    public SettingsAboutView() {
        setPadding(new Insets(15));
        setCenter(createContent());

        setupMappingTable();
    }

    private ScrollPane createContent() {
        VBox root = new VBox(20);
        root.setFillWidth(true);

        Label header = new Label("SETTINGS, ACADEMIC SYLLABUS MAPPING & PROJECT IDENTITY");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        // 1. Project Identity Card
        VBox identityCard = new VBox(8);
        identityCard.getStyleClass().add("metric-card");

        Label projName = new Label("Warehouse Inventory Monitoring Network (WIMN)");
        projName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        Label courseLabel = new Label("Enterprise Logistics Network Architecture | Production Capstone Simulation");
        courseLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #10B981; -fx-font-size: 13px;");

        Label desc = new Label("""
            A scalable computer networking capstone application simulating multi-department warehouse communication:
            Receiving, Storage, Dispatch, Security, and Management. Demonstrates OSI/TCP-IP layering, framing, CRC,
            Dijkstra & Bellman-Ford routing, IPv4/IPv6 subnetting, TCP 3-way handshake, DHCP DORA, DNS, and stateful firewalls.
            """);
        desc.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 12px;");

        identityCard.getChildren().addAll(projName, courseLabel, desc);

        // 2. Theme & Database Configuration Bar
        HBox configBar = new HBox(15);
        configBar.setAlignment(Pos.CENTER_LEFT);
        configBar.getStyleClass().add("metric-card");

        Label themeLabel = new Label("User Interface Theme:");
        themeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

        Button btnDark = new Button("Dark Navy Industrial (Default)");
        btnDark.getStyleClass().add("btn-primary");
        btnDark.setOnAction(e -> ThemeManager.applyTheme(getScene(), ThemeManager.Theme.DARK_NAVY));

        Button btnLight = new Button("Light Enterprise Mode");
        btnLight.getStyleClass().add("btn-secondary");
        btnLight.setOnAction(e -> ThemeManager.applyTheme(getScene(), ThemeManager.Theme.LIGHT_ENTERPRISE));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnResetDb = new Button("Reset Demo SQLite Database");
        btnResetDb.getStyleClass().add("btn-danger");
        btnResetDb.setOnAction(e -> {
            DatabaseManager.getInstance().initDatabase();
            Alert a = new Alert(Alert.AlertType.INFORMATION, "SQLite database schema verified and initialized.");
            a.showAndWait();
        });

        configBar.getChildren().addAll(themeLabel, btnDark, btnLight, spacer, btnResetDb);

        // 3. Course Outcomes (CO1 to CO5) Cards
        VBox coBox = new VBox(10);
        coBox.getStyleClass().add("metric-card");
        Label coTitle = new Label("COURSE OUTCOMES (CO) ALIGNMENT MATRIX");
        coTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #A78BFA;");

        GridPane coGrid = new GridPane();
        coGrid.setHgap(15);
        coGrid.setVgap(8);

        coGrid.add(createCoCard("CO1: Network Infrastructure Models", "Applied via 2D & 3D Hierarchical Star Warehouse Network Architecture, Edge/Core separation."), 0, 0);
        coGrid.add(createCoCard("CO2: Link-Layer Services & Design", "Applied via HDLC Bit-Stuffing, CRC-16/32 error detection, Sliding Window flow control, MAC learning."), 1, 0);
        coGrid.add(createCoCard("CO3: Routing Algorithms & Forwarding", "Evaluated via Dijkstra Link-State and Bellman-Ford Distance-Vector simulations with fault recovery."), 0, 1);
        coGrid.add(createCoCard("CO4: Transport & Network Protocols", "Described via TCP 3-way handshake, congestion control, UDP datagrams, IPv4 CIDR subnetting."), 1, 1);
        coGrid.add(createCoCard("CO5: Application Services & Security", "Managed via DHCP DORA, DNS resolution, DASH video streaming, and stateful Firewall ACL rules."), 0, 2, 2, 1);

        coBox.getChildren().addAll(coTitle, coGrid);

        // 4. Academic Syllabus Mapping Table
        VBox mapBox = new VBox(10);
        mapBox.getStyleClass().add("metric-card");
        Label mapTitle = new Label("COMPLETE COMPUTER NETWORKS SYLLABUS-TO-IMPLEMENTATION MAPPING TABLE (UNITS I - V)");
        mapTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #34D399;");
        mapBox.getChildren().addAll(mapTitle, mappingTable);

        root.getChildren().addAll(header, identityCard, configBar, coBox, mapBox);
        return new ScrollPane(root);
    }

    private VBox createCoCard(String title, String desc) {
        VBox box = new VBox(4);
        box.setStyle("-fx-background-color: #0F172A; -fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #1E293B; -fx-border-radius: 6;");
        Label t = new Label(title);
        t.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #38BDF8;");
        Label d = new Label(desc);
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 11px; -fx-text-fill: #CBD5E1;");
        box.getChildren().addAll(t, d);
        return box;
    }

    private void setupMappingTable() {
        mappingTable.setPrefHeight(380);
        mappingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<SyllabusMappingRow, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setMaxWidth(80);

        TableColumn<SyllabusMappingRow, String> topicCol = new TableColumn<>("Syllabus Topic");
        topicCol.setCellValueFactory(new PropertyValueFactory<>("syllabusTopic"));

        TableColumn<SyllabusMappingRow, String> modCol = new TableColumn<>("Implemented Module / Laboratory");
        modCol.setCellValueFactory(new PropertyValueFactory<>("applicationModule"));

        TableColumn<SyllabusMappingRow, String> coCol = new TableColumn<>("Course Outcome");
        coCol.setCellValueFactory(new PropertyValueFactory<>("mappedCO"));
        coCol.setMaxWidth(120);

        TableColumn<SyllabusMappingRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setMaxWidth(110);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String st, boolean empty) {
                super.updateItem(st, empty);
                if (empty || st == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(st);
                    setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            }
        });

        mappingTable.getColumns().addAll(unitCol, topicCol, modCol, coCol, statusCol);

        mappingTable.setItems(FXCollections.observableArrayList(
                new SyllabusMappingRow("Unit-I", "Building a Network & Edge/Core", "2D/3D Network Topology & Department Views", "CO1", "COMPLETE"),
                new SyllabusMappingRow("Unit-I", "Layering, Encapsulation & OSI/TCP-IP", "Packet Inspector Tool (Layer 2, 3, 4, 7)", "CO1", "COMPLETE"),
                new SyllabusMappingRow("Unit-I", "Physical Medium & Performance Metrics", "Delay & Capacity Calculator (Latency/Throughput)", "CO1", "COMPLETE"),
                new SyllabusMappingRow("Unit-I", "Signal Encoding (NRZ, Manchester)", "Signal Wave & Digital Encoding Demonstrator", "CO1", "COMPLETE"),
                new SyllabusMappingRow("Unit-II", "Framing (Bit & Byte Stuffing)", "Data Link Lab (HDLC Bit-Stuffing Visualizer)", "CO2", "COMPLETE"),
                new SyllabusMappingRow("Unit-II", "Flow Control (Stop & Wait, Sliding Window)", "Data Link Lab (Go-Back-N & Selective Repeat)", "CO2", "COMPLETE"),
                new SyllabusMappingRow("Unit-II", "Error Control (CRC-16, CRC-32, Checksum)", "CRC Generator & Simulated Bit-Flip Inversion", "CO2", "COMPLETE"),
                new SyllabusMappingRow("Unit-II", "Media Access & Ethernet MAC Learning", "Switch Dynamic MAC Table Learning Simulation", "CO2", "COMPLETE"),
                new SyllabusMappingRow("Unit-II", "Wireless LAN (802.11), Bluetooth, Zigbee", "Storage IoT & Handheld Scanner Integration", "CO2", "COMPLETE"),
                new SyllabusMappingRow("Unit-III", "IPv4 Addressing & Classless CIDR", "Interactive Subnet Calculator (/0 to /32)", "CO3", "COMPLETE"),
                new SyllabusMappingRow("Unit-III", "Subnetting Edge Cases (/31, /32)", "Subnet Calculator RFC 3021 P2P Links", "CO3", "COMPLETE"),
                new SyllabusMappingRow("Unit-III", "Distance Vector Routing (Bellman-Ford)", "Routing Lab Bellman-Ford Convergence Cycles", "CO3", "COMPLETE"),
                new SyllabusMappingRow("Unit-III", "Link State Routing (Dijkstra Shortest Path)", "Routing Lab Dijkstra Shortest Path Engine", "CO3", "COMPLETE"),
                new SyllabusMappingRow("Unit-III", "IPv6 Addressing & Header Architecture", "IPv6 RFC 5952 Compressor & Zero Compression", "CO3", "COMPLETE"),
                new SyllabusMappingRow("Unit-IV", "TCP 3-Way Handshake & Reliable Byte Stream", "Transport Lab TCP Handshake (SYN/ACK/ISN)", "CO4", "COMPLETE"),
                new SyllabusMappingRow("Unit-IV", "Flow Control & Congestion Control (CWND)", "Transport Lab Slow Start & Fast Retransmit", "CO4", "COMPLETE"),
                new SyllabusMappingRow("Unit-IV", "UDP Datagrams & Best-Effort Transmission", "Transport Lab UDP Transmission Simulator", "CO4", "COMPLETE"),
                new SyllabusMappingRow("Unit-IV", "SCTP Multi-Streaming & Multi-Homing", "Transport Lab SCTP Conceptual Association", "CO4", "COMPLETE"),
                new SyllabusMappingRow("Unit-IV", "TCP vs. UDP Performance Comparison", "Interactive Benchmark (Throughput/Latency/Loss)", "CO4", "COMPLETE"),
                new SyllabusMappingRow("Unit-V", "Web & HTTP Request/Response (REST API)", "Application Lab Local HTTP JSON Simulator", "CO5", "COMPLETE"),
                new SyllabusMappingRow("Unit-V", "DHCP Address Allocation (DORA Process)", "Application Lab DORA Four-Step DHCP Simulator", "CO5", "COMPLETE"),
                new SyllabusMappingRow("Unit-V", "DNS Domain Resolution & Local Cache", "Application Lab Local DNS Cache & Resolver", "CO5", "COMPLETE"),
                new SyllabusMappingRow("Unit-V", "DASH Dynamic Adaptive Video Streaming", "Application Lab Adaptive Surveillance Bitrate", "CO5", "COMPLETE"),
                new SyllabusMappingRow("Unit-V", "QUIC Protocol (HTTP/3 over UDP)", "Application Lab 0-RTT & Stream Multiplexing", "CO5", "COMPLETE"),
                new SyllabusMappingRow("Unit-V", "Firewall Access Control & Packet Filtering", "Security Lab Stateful ALLOW/DENY Rules Engine", "CO5", "COMPLETE"),
                new SyllabusMappingRow("Practicals", "Java TCP Socket Chat Program", "Practicals Lab Real ServerSocket & Socket Chat", "CO4", "COMPLETE"),
                new SyllabusMappingRow("Practicals", "UDP Socket DNS Query Program", "Practicals Lab Real DatagramSocket DNS Lookup", "CO4", "COMPLETE"),
                new SyllabusMappingRow("Practicals", "Network CLI Commands (ping, ipconfig, etc.)", "Practicals Lab Interactive Command Tutor", "CO1", "COMPLETE")
        ));
    }
}
