package com.wimn.views;

import com.wimn.networkservices.DASHOptimizer;
import com.wimn.networkservices.DHCPSimulator;
import com.wimn.networkservices.DNSSimulator;
import com.wimn.networkservices.HTTPSimulator;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

public class AppServicesLabView extends BorderPane {
    private final DHCPSimulator dhcpSim = new DHCPSimulator();
    private final DNSSimulator dnsSim = new DNSSimulator();

    // HTTP Simulator Controls
    private final ComboBox<String> httpMethodCombo = new ComboBox<>();
    private final TextField httpEndpointField = new TextField("/api/inventory/summary");
    private final TextArea httpResponseArea = new TextArea();

    // DHCP Simulator Controls
    private final TextField dhcpMacField = new TextField("00:1A:2B:10:00:88");
    private final TextField dhcpHostField = new TextField("Zebra-Scanner-Bay3");
    private final TableView<DHCPSimulator.DORAStep> dhcpTable = new TableView<>();

    // DNS Simulator Controls
    private final TextField dnsQueryField = new TextField("inventory.warehouse.local");
    private final Label dnsResultLabel = new Label("-");
    private final Label dnsCacheStatusLabel = new Label("-");
    private final TextArea dnsLogArea = new TextArea();

    // DASH Streaming Controls
    private final Slider dashBwSlider = new Slider(500, 15000, 5000); // 500 kbps to 15 Mbps
    private final Label dashCurrentBwLabel = new Label("5.0 Mbps");
    private final Label dashResolutionLabel = new Label("720p HD");
    private final Label dashBitrateLabel = new Label("4000 kbps");
    private final Label dashCodecLabel = new Label("H.264 / AVC");

    public AppServicesLabView() {
        setPadding(new Insets(15));
        setCenter(createContent());

        setupDhcpTable();
        executeHttpRequest();
        runDhcpSimulation();
        executeDnsLookup();
        updateDashQuality();
    }

    private ScrollPane createContent() {
        VBox root = new VBox(20);
        root.setFillWidth(true);

        Label header = new Label("UNIT-V: APPLICATION LAYER SERVICES & PROTOCOLS LAB");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        // 1. HTTP / REST API Simulation
        VBox httpBox = new VBox(10);
        httpBox.getStyleClass().add("metric-card");
        Label httpTitle = new Label("1. WEB & HTTP PROTOCOL SIMULATOR (METHODS, STATUS CODES, HEADERS & JSON)");
        httpTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10B981;");

        HBox httpRow = new HBox(12);
        httpRow.setAlignment(Pos.CENTER_LEFT);

        httpMethodCombo.setItems(FXCollections.observableArrayList("GET", "POST"));
        httpMethodCombo.setValue("GET");
        httpEndpointField.setPrefWidth(280);

        Button btnSendHttp = new Button("Execute HTTP Request");
        btnSendHttp.getStyleClass().add("btn-primary");
        btnSendHttp.setOnAction(e -> executeHttpRequest());

        httpRow.getChildren().addAll(new Label("Method:"), httpMethodCombo, new Label("URL:"), httpEndpointField, btnSendHttp);

        httpResponseArea.setEditable(false);
        httpResponseArea.setPrefHeight(120);
        httpResponseArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

        httpBox.getChildren().addAll(httpTitle, httpRow, httpResponseArea);

        // 2. DHCP DORA Protocol Simulation
        VBox dhcpBox = new VBox(10);
        dhcpBox.getStyleClass().add("metric-card");
        Label dhcpTitle = new Label("2. DYNAMIC HOST CONFIGURATION PROTOCOL (DHCP DORA PROCESS)");
        dhcpTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #38BDF8;");

        HBox dhcpRow = new HBox(12);
        dhcpRow.setAlignment(Pos.CENTER_LEFT);
        dhcpMacField.setPrefWidth(180);
        dhcpHostField.setPrefWidth(180);

        Button btnRunDhcp = new Button("Request New IP Lease");
        btnRunDhcp.getStyleClass().add("btn-primary");
        btnRunDhcp.setOnAction(e -> runDhcpSimulation());

        dhcpRow.getChildren().addAll(new Label("Client MAC:"), dhcpMacField, new Label("Hostname:"), dhcpHostField, btnRunDhcp);

        dhcpBox.getChildren().addAll(dhcpTitle, dhcpRow, dhcpTable);

        // 3. DNS Domain Name Resolution Simulation
        VBox dnsBox = new VBox(10);
        dnsBox.getStyleClass().add("metric-card");
        Label dnsTitle = new Label("3. DOMAIN NAME SYSTEM (DNS LOCAL CACHE & AUTHORITATIVE RESOLUTION)");
        dnsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #F59E0B;");

        HBox dnsRow = new HBox(12);
        dnsRow.setAlignment(Pos.CENTER_LEFT);
        dnsQueryField.setPrefWidth(260);

        Button btnResolveDns = new Button("Resolve Hostname");
        btnResolveDns.getStyleClass().add("btn-primary");
        btnResolveDns.setOnAction(e -> executeDnsLookup());

        dnsRow.getChildren().addAll(new Label("Domain Name:"), dnsQueryField, btnResolveDns);

        HBox dnsResultsRow = new HBox(25);
        dnsResultsRow.setAlignment(Pos.CENTER_LEFT);
        dnsResultLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38BDF8; -fx-font-size: 14px;");
        dnsCacheStatusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #10B981; -fx-font-size: 14px;");

        dnsResultsRow.getChildren().addAll(
                new HBox(8, new Label("Resolved IPv4:"), dnsResultLabel),
                new HBox(8, new Label("Cache Status:"), dnsCacheStatusLabel)
        );

        dnsLogArea.setEditable(false);
        dnsLogArea.setPrefHeight(100);
        dnsLogArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

        dnsBox.getChildren().addAll(dnsTitle, dnsRow, dnsResultsRow, dnsLogArea);

        // 4. DASH Dynamic Adaptive Video Streaming
        VBox dashBox = new VBox(10);
        dashBox.getStyleClass().add("metric-card");
        Label dashTitle = new Label("4. DASH: DYNAMIC ADAPTIVE STREAMING OVER HTTP (VIDEO SURVEILLANCE FEED)");
        dashTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #A78BFA;");

        HBox dashRow = new HBox(15);
        dashRow.setAlignment(Pos.CENTER_LEFT);

        dashBwSlider.setPrefWidth(300);
        dashBwSlider.setShowTickLabels(true);
        dashBwSlider.setShowTickMarks(true);
        dashBwSlider.setMajorTickUnit(5000);
        dashBwSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateDashQuality());

        dashRow.getChildren().addAll(new Label("Simulated Available Bandwidth:"), dashBwSlider, dashCurrentBwLabel);

        GridPane dashGrid = new GridPane();
        dashGrid.setHgap(20);
        dashGrid.setVgap(8);

        dashResolutionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34D399; -fx-font-size: 15px;");
        dashBitrateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38BDF8; -fx-font-size: 15px;");
        dashCodecLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #F59E0B; -fx-font-size: 15px;");

        dashGrid.add(createField("Adaptive Stream Resolution:", dashResolutionLabel), 0, 0);
        dashGrid.add(createField("Stream Target Bitrate:", dashBitrateLabel), 1, 0);
        dashGrid.add(createField("Video Compression Codec:", dashCodecLabel), 2, 0);

        dashBox.getChildren().addAll(dashTitle, dashRow, dashGrid);

        // 5. QUIC Protocol Overview Card
        VBox quicBox = new VBox(8);
        quicBox.getStyleClass().add("metric-card");
        Label quicTitle = new Label("5. QUIC PROTOCOL (RFC 9000 - HTTP/3 OVER UDP)");
        quicTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #60A5FA;");

        Label quicDesc = new Label("""
            • Zero-RTT Connection Establishment: Combines transport and cryptographic TLS 1.3 handshakes into a single round trip.
            • Elimination of Head-of-Line Blocking: Multi-stream multiplexing occurs natively in user-space UDP datagrams.
            • Connection Migration: Connections survive client IP changes (e.g. warehouse scanner roaming between Wi-Fi APs) using Connection IDs.
            """);
        quicDesc.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 12px;");
        quicBox.getChildren().addAll(quicTitle, quicDesc);

        root.getChildren().addAll(header, httpBox, dhcpBox, dnsBox, dashBox, quicBox);
        return new ScrollPane(root);
    }

    private VBox createField(String title, Label val) {
        VBox v = new VBox(4);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");
        v.getChildren().addAll(t, val);
        return v;
    }

    private void executeHttpRequest() {
        String method = httpMethodCombo.getValue();
        String endpoint = httpEndpointField.getText().trim();
        HTTPSimulator.HTTPResponse resp = HTTPSimulator.executeRequest(method, endpoint, "");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("HTTP/1.1 %d %s\n", resp.statusCode(), resp.statusText()));
        resp.headers().forEach((k, v) -> sb.append(String.format("%s: %s\n", k, v)));
        sb.append("\n");
        sb.append(resp.body());

        httpResponseArea.setText(sb.toString());
    }

    private void setupDhcpTable() {
        dhcpTable.setPrefHeight(150);
        dhcpTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DHCPSimulator.DORAStep, String> typeCol = new TableColumn<>("DHCP Message");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("messageType"));
        typeCol.setMaxWidth(140);

        TableColumn<DHCPSimulator.DORAStep, String> srcCol = new TableColumn<>("Source Socket");
        srcCol.setCellValueFactory(new PropertyValueFactory<>("source"));

        TableColumn<DHCPSimulator.DORAStep, String> dstCol = new TableColumn<>("Destination Socket");
        dstCol.setCellValueFactory(new PropertyValueFactory<>("destination"));

        TableColumn<DHCPSimulator.DORAStep, String> descCol = new TableColumn<>("Protocol Transmission Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        dhcpTable.getColumns().addAll(typeCol, srcCol, dstCol, descCol);
    }

    private void runDhcpSimulation() {
        String mac = dhcpMacField.getText().trim();
        String host = dhcpHostField.getText().trim();
        List<DHCPSimulator.DORAStep> steps = dhcpSim.requestLease(mac, host);
        dhcpTable.setItems(FXCollections.observableArrayList(steps));
    }

    private void executeDnsLookup() {
        String query = dnsQueryField.getText().trim();
        DNSSimulator.DNSResolutionResult res = dnsSim.resolve(query);

        dnsResultLabel.setText(res.resolvedValue());
        dnsCacheStatusLabel.setText(res.isCacheHit() ? "HIT (0ms Cache)" : "MISS (Authoritative Query)");
        dnsCacheStatusLabel.setStyle(res.isCacheHit() ? "-fx-text-fill: #10B981; -fx-font-weight: bold;" : "-fx-text-fill: #F59E0B; -fx-font-weight: bold;");

        dnsLogArea.clear();
        for (String step : res.lookupSteps()) {
            dnsLogArea.appendText(step + "\n");
        }
    }

    private void updateDashQuality() {
        double kbps = dashBwSlider.getValue();
        dashCurrentBwLabel.setText(String.format("%.1f Mbps", kbps / 1000.0));
        DASHOptimizer.VideoRepresentation rep = DASHOptimizer.adaptBitrate(kbps);

        dashResolutionLabel.setText(rep.qualityLabel() + " (" + rep.resolutionWidth() + "x" + rep.resolutionHeight() + ")");
        dashBitrateLabel.setText(rep.targetBitrateKbps() + " kbps");
        dashCodecLabel.setText(rep.codec());
    }
}
