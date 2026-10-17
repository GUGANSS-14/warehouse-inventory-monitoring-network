package com.wimn.views;

import com.wimn.models.*;
import com.wimn.inventory.InventoryService;
import com.wimn.repositories.EventRepository;
import com.wimn.simulation.SimulationEngine;
import com.wimn.topology.WarehouseTopology;
import com.wimn.utilities.CsvExporter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class WarehouseOpsView extends BorderPane {
    private final InventoryService inventoryService;
    private final WarehouseTopology topology;
    private final SimulationEngine simEngine;
    private final EventRepository eventRepo;

    // Receiving Tab controls
    private final TextField rcvSkuField = new TextField("SKU-NET-04");
    private final TextField rcvNameField = new TextField("Cat6A Shielded Ethernet Cable 305m");
    private final ComboBox<String> rcvCatCombo = new ComboBox<>();
    private final TextField rcvQtyField = new TextField("25");
    private final TextField rcvPriceField = new TextField("125.0");
    private final TextField rcvRackField = new TextField("Rack-B2-02");
    private final TextField rcvSupplierField = new TextField("Nexans Infrastructure Ltd");
    private final TextArea rcvNetConsole = new TextArea();

    // Storage Tab controls
    private final TableView<InventoryItem> storageTable = new TableView<>();
    private final TableView<InventoryItem> lowStockTable = new TableView<>();
    private final TextArea storageRfidConsole = new TextArea();

    // Dispatch Tab controls
    private final ComboBox<InventoryItem> dspSkuCombo = new ComboBox<>();
    private final TextField dspQtyField = new TextField("5");
    private final TextField dspCustomerField = new TextField("Global Logistics Hub - Bay 4");
    private final TextArea dspNetConsole = new TextArea();
    private final TableView<InventoryTransaction> dspTransactionsTable = new TableView<>();

    // Security Tab controls
    private final TextArea secAlertConsole = new TextArea();

    // Manager Tab controls
    private final Label mgrTotalValueLabel = new Label("$0.00");
    private final Label mgrTotalUnitsLabel = new Label("0 units");
    private final Label mgrLowStockLabel = new Label("0");
    private final TableView<InventoryItem> mgrSummaryTable = new TableView<>();

    public WarehouseOpsView(InventoryService inventoryService, WarehouseTopology topology,
                            SimulationEngine simEngine, EventRepository eventRepo) {
        this.inventoryService = inventoryService;
        this.topology = topology;
        this.simEngine = simEngine;
        this.eventRepo = eventRepo;

        setPadding(new Insets(15));
        setCenter(createTabPane());

        refreshAllTables();
    }

    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("tab-pane");

        tabPane.getTabs().add(createReceivingTab());
        tabPane.getTabs().add(createStorageTab());
        tabPane.getTabs().add(createDispatchTab());
        tabPane.getTabs().add(createSecurityTab());
        tabPane.getTabs().add(createManagerTab());

        return tabPane;
    }

    // =========================================================================
    // 1. RECEIVING DEPARTMENT TAB
    // =========================================================================
    private Tab createReceivingTab() {
        Tab tab = new Tab("1. Receiving Department");
        tab.setClosable(false);

        VBox box = new VBox(15);
        box.setPadding(new Insets(15));

        Label header = new Label("INBOUND GOODS RECEIVING & SCANNER NETWORK LINK");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(12);
        form.getStyleClass().add("metric-card");

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(140);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(140);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(col0, col1, col2, col3);

        rcvCatCombo.setItems(FXCollections.observableArrayList(
                "Networking Hardware", "Computing Servers", "Wireless Systems",
                "Cabling Infrastructure", "Security Surveillance", "Mobile Terminals", "Material Handling"
        ));
        rcvCatCombo.setValue("Cabling Infrastructure");

        rcvSkuField.setMaxWidth(Double.MAX_VALUE);
        rcvNameField.setMaxWidth(Double.MAX_VALUE);
        rcvCatCombo.setMaxWidth(Double.MAX_VALUE);
        rcvQtyField.setMaxWidth(Double.MAX_VALUE);
        rcvPriceField.setMaxWidth(Double.MAX_VALUE);
        rcvRackField.setMaxWidth(Double.MAX_VALUE);
        rcvSupplierField.setMaxWidth(Double.MAX_VALUE);

        form.add(new Label("Product SKU:"), 0, 0);
        form.add(rcvSkuField, 1, 0);
        form.add(new Label("Product Name:"), 2, 0);
        form.add(rcvNameField, 3, 0);

        form.add(new Label("Category:"), 0, 1);
        form.add(rcvCatCombo, 1, 1);
        form.add(new Label("Quantity Received:"), 2, 1);
        form.add(rcvQtyField, 3, 1);

        form.add(new Label("Unit Price ($):"), 0, 2);
        form.add(rcvPriceField, 1, 2);
        form.add(new Label("Assigned Rack:"), 2, 2);
        form.add(rcvRackField, 3, 2);

        form.add(new Label("Supplier / Carrier:"), 0, 3);
        form.add(rcvSupplierField, 1, 3);

        Button btnReceive = new Button("Simulate Barcode Scan & Register Inbound Stock");
        btnReceive.getStyleClass().add("btn-primary");
        btnReceive.setMaxWidth(Double.MAX_VALUE);
        btnReceive.setOnAction(e -> handleReceivingSubmit());
        form.add(btnReceive, 2, 3, 2, 1);

        // Network Transmission Console
        VBox consoleBox = new VBox(8);
        consoleBox.getStyleClass().add("metric-card");
        Label consoleTitle = new Label("BARCODE SCANNER WIRELESS PACKET FLOW (SCN-RCV-01 -> SW-RCV-01 -> SRV-MAIN-01)");
        consoleTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        rcvNetConsole.setEditable(false);
        rcvNetConsole.setPrefHeight(150);
        rcvNetConsole.setMaxWidth(Double.MAX_VALUE);
        rcvNetConsole.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        rcvNetConsole.setText("[IDLE] Waiting for barcode scanner trigger on VLAN 10 (192.168.10.21)...");

        consoleBox.getChildren().addAll(consoleTitle, rcvNetConsole);

        box.getChildren().addAll(header, form, consoleBox);
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        tab.setContent(sp);
        return tab;
    }

    private void handleReceivingSubmit() {
        try {
            String sku = rcvSkuField.getText().trim();
            String name = rcvNameField.getText().trim();
            String cat = rcvCatCombo.getValue();
            int qty = Integer.parseInt(rcvQtyField.getText().trim());
            double price = Double.parseDouble(rcvPriceField.getText().trim());
            String rack = rcvRackField.getText().trim();
            String supplier = rcvSupplierField.getText().trim();

            InventoryService.OperationResult res = inventoryService.receiveProduct(sku, name, cat, qty, price, rack, supplier);

            if (res.success()) {
                // Simulate Packet Transmission
                PacketHeader hdr = new PacketHeader("192.168.10.21", "192.168.1.50", 49200, 8080, "TCP", 64, 1500, 3000, 10);
                hdr.setTcpFlags("PSH-ACK");
                NetworkPacket pkt = new NetworkPacket(res.packetId(), "SCN-RCV-01", "SRV-MAIN-01", hdr, "INBOUND_INTAKE:SKU=" + sku + ";QTY=" + qty, 148);
                simEngine.transmitPacket(pkt);

                rcvNetConsole.appendText("\n\n" + "=".repeat(60));
                rcvNetConsole.appendText("\n[TX SUCCESS] Barcode Scanned by Handheld Terminal: " + sku);
                rcvNetConsole.appendText("\n[LAYER 2] Source MAC: 00:1A:2B:10:00:21 -> AP-RCV-01 (Wi-Fi 6 SSID: WIMN-RCV-WIFI)");
                rcvNetConsole.appendText("\n[LAYER 3] Packet Routed: 192.168.10.21 -> 192.168.10.2 -> 192.168.1.2 -> 192.168.1.50 (VLAN 10)");
                rcvNetConsole.appendText("\n[LAYER 4] TCP Segment Seq: 1500, Ack: 3000, Status: DELIVERED (RTT: " + String.format("%.2f", pkt.getCalculatedLatencyMs()) + " ms)");
                rcvNetConsole.appendText("\n[DATABASE] Transaction Committed: " + res.transactionId() + " (+ " + qty + " units to " + rack + ")");
                rcvNetConsole.appendText("\n[CROSS-DEPT] Storage Department notified to assign put-away aisle.");

                showNotification("Inbound Stock Registered", res.message(), Alert.AlertType.INFORMATION);
                refreshAllTables();
            } else {
                showNotification("Receiving Failed", res.message(), Alert.AlertType.ERROR);
            }
        } catch (NumberFormatException ex) {
            showNotification("Validation Error", "Quantity and Unit Price must be valid numbers.", Alert.AlertType.WARNING);
        }
    }

    // =========================================================================
    // 2. STORAGE DEPARTMENT TAB
    // =========================================================================
    private Tab createStorageTab() {
        Tab tab = new Tab("2. Storage Department");
        tab.setClosable(false);

        VBox box = new VBox(15);
        box.setPadding(new Insets(15));

        Label header = new Label("WAREHOUSE STORAGE AISLES, RACK LOCATIONS & RFID MONITOR");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #10B981;");

        // Action bar with RFID Trigger
        HBox rfidBar = new HBox(15);
        rfidBar.setAlignment(Pos.CENTER_LEFT);
        rfidBar.getStyleClass().add("metric-card");

        Button btnRfidScan = new Button("Simulate Fixed RFID Gate Scan (Aisle A1)");
        btnRfidScan.getStyleClass().add("btn-success");
        btnRfidScan.setOnAction(e -> simulateRfidGateScan());

        Label rfidStatus = new Label("RFID Fixed Reader RFID-STR-01: ONLINE (865MHz EPC Gen2)");
        rfidStatus.setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");

        rfidBar.getChildren().addAll(btnRfidScan, rfidStatus);

        // Inventory Stock Table
        VBox tableBox = new VBox(8);
        tableBox.getStyleClass().add("metric-card");
        Label tableTitle = new Label("CURRENT WAREHOUSE STOCK BY RACK LOCATION");
        tableTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

        setupStorageTable();
        tableBox.getChildren().addAll(tableTitle, storageTable);

        // Low-stock alert table
        VBox alertBox = new VBox(8);
        alertBox.getStyleClass().add("metric-card");
        Label alertTitle = new Label("LOW STOCK WARNINGS (QUANTITY <= MIN THRESHOLD)");
        alertTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #F87171;");

        setupLowStockTable();
        alertBox.getChildren().addAll(alertTitle, lowStockTable);

        box.getChildren().addAll(header, rfidBar, tableBox, alertBox);
        ScrollPane spStorage = new ScrollPane(box);
        spStorage.setFitToWidth(true);
        tab.setContent(spStorage);
        return tab;
    }

    private void simulateRfidGateScan() {
        PacketHeader hdr = new PacketHeader("192.168.20.31", "192.168.1.50", 49190, 8080, "TCP", 64, 4010, 5010, 20);
        NetworkPacket pkt = new NetworkPacket("PKT-RFID-" + (System.currentTimeMillis() % 10000), "RFID-STR-01", "SRV-MAIN-01", hdr, "RFID_TAG_PALLET_AUDIT_PASS", 64);
        simEngine.transmitPacket(pkt);

        eventRepo.addEvent(new NetworkEvent(
                "EVT-RFID-" + System.currentTimeMillis() % 1000,
                NetworkEvent.Severity.SUCCESS, "INVENTORY", "Storage Dept",
                "RFID Gate Reader scanned active pallet tag at Rack A1. Telemetry reported to Central Server (RTT: " + String.format("%.2f", pkt.getCalculatedLatencyMs()) + " ms)."
        ));
        showNotification("RFID Audit Scan", "RFID Gate Reader verified active aisle tags. Inventory levels confirmed.", Alert.AlertType.INFORMATION);
        refreshAllTables();
    }

    private void setupStorageTable() {
        storageTable.setPrefHeight(200);
        storageTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<InventoryItem, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));

        TableColumn<InventoryItem, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<InventoryItem, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<InventoryItem, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<InventoryItem, String> rackCol = new TableColumn<>("Rack Location");
        rackCol.setCellValueFactory(new PropertyValueFactory<>("rackLocation"));

        TableColumn<InventoryItem, Double> priceCol = new TableColumn<>("Price ($)");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        storageTable.getColumns().addAll(skuCol, nameCol, catCol, qtyCol, rackCol, priceCol);
    }

    private void setupLowStockTable() {
        lowStockTable.setPrefHeight(120);
        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<InventoryItem, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));

        TableColumn<InventoryItem, String> nameCol = new TableColumn<>("Item Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<InventoryItem, Integer> qtyCol = new TableColumn<>("Available Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<InventoryItem, Integer> threshCol = new TableColumn<>("Min Threshold");
        threshCol.setCellValueFactory(new PropertyValueFactory<>("minThreshold"));

        TableColumn<InventoryItem, String> rackCol = new TableColumn<>("Rack Location");
        rackCol.setCellValueFactory(new PropertyValueFactory<>("rackLocation"));

        lowStockTable.getColumns().addAll(skuCol, nameCol, qtyCol, threshCol, rackCol);
    }

    // =========================================================================
    // 3. DISPATCH DEPARTMENT TAB
    // =========================================================================
    private Tab createDispatchTab() {
        Tab tab = new Tab("3. Dispatch Department");
        tab.setClosable(false);

        VBox box = new VBox(15);
        box.setPadding(new Insets(15));

        Label header = new Label("OUTBOUND ORDER DISPATCH, STOCK VALIDATION & ROUTING");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #F59E0B;");

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(12);
        form.getStyleClass().add("metric-card");

        ColumnConstraints dcol0 = new ColumnConstraints();
        dcol0.setMinWidth(170);
        ColumnConstraints dcol1 = new ColumnConstraints();
        dcol1.setHgrow(Priority.ALWAYS);
        ColumnConstraints dcol2 = new ColumnConstraints();
        dcol2.setMinWidth(140);
        ColumnConstraints dcol3 = new ColumnConstraints();
        dcol3.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(dcol0, dcol1, dcol2, dcol3);

        dspSkuCombo.setMaxWidth(Double.MAX_VALUE);
        dspQtyField.setMaxWidth(Double.MAX_VALUE);
        dspCustomerField.setMaxWidth(Double.MAX_VALUE);

        form.add(new Label("Select Outbound Product:"), 0, 0);
        form.add(dspSkuCombo, 1, 0);

        form.add(new Label("Dispatch Quantity:"), 2, 0);
        form.add(dspQtyField, 3, 0);

        form.add(new Label("Destination Customer / Bay:"), 0, 1);
        form.add(dspCustomerField, 1, 1);

        Button btnDispatch = new Button("Validate Stock & Dispatch Order");
        btnDispatch.getStyleClass().add("btn-primary");
        btnDispatch.setMaxWidth(Double.MAX_VALUE);
        btnDispatch.setOnAction(e -> handleDispatchSubmit());
        form.add(btnDispatch, 2, 1, 2, 1);

        // Network Packet Trace Console
        VBox consoleBox = new VBox(8);
        consoleBox.getStyleClass().add("metric-card");
        Label consoleTitle = new Label("DISPATCH ORDER PACKET FLOW: WS-DSP-01 -> SW-DSP-01 -> SW-CORE-01 -> SRV-MAIN-01 -> SW-STR-01");
        consoleTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        dspNetConsole.setEditable(false);
        dspNetConsole.setPrefHeight(130);
        dspNetConsole.setMaxWidth(Double.MAX_VALUE);
        dspNetConsole.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        dspNetConsole.setText("[IDLE] Waiting for outbound dispatch order submission...");

        consoleBox.getChildren().addAll(consoleTitle, dspNetConsole);

        // Recent Dispatch Orders Table
        VBox tableBox = new VBox(8);
        tableBox.getStyleClass().add("metric-card");
        Label tableTitle = new Label("OUTBOUND DISPATCH ORDER LEDGER");
        tableTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

        setupDispatchTable();
        tableBox.getChildren().addAll(tableTitle, dspTransactionsTable);

        box.getChildren().addAll(header, form, consoleBox, tableBox);
        ScrollPane spDispatch = new ScrollPane(box);
        spDispatch.setFitToWidth(true);
        tab.setContent(spDispatch);
        return tab;
    }

    private void handleDispatchSubmit() {
        InventoryItem selected = dspSkuCombo.getValue();
        if (selected == null) {
            showNotification("Missing Selection", "Please select a product to dispatch.", Alert.AlertType.WARNING);
            return;
        }

        try {
            int qty = Integer.parseInt(dspQtyField.getText().trim());
            String customer = dspCustomerField.getText().trim();

            InventoryService.OperationResult res = inventoryService.dispatchProduct(selected.getSku(), qty, customer);

            if (res.success()) {
                // Dispatch Packet Routing
                PacketHeader hdr = new PacketHeader("192.168.30.11", "192.168.1.50", 51000, 8080, "TCP", 64, 2100, 4200, 30);
                NetworkPacket pkt = new NetworkPacket(res.packetId(), "WS-DSP-01", "SRV-MAIN-01", hdr, "DISPATCH_RELEASE:SKU=" + selected.getSku() + ";QTY=" + qty, 136);
                simEngine.transmitPacket(pkt);

                dspNetConsole.appendText("\n\n" + "=".repeat(60));
                dspNetConsole.appendText("\n[ORDER COMMITTED] Transaction ID: " + res.transactionId());
                dspNetConsole.appendText("\n[HOP 1] WS-DSP-01 (192.168.30.11) -> SW-DSP-01 (Port 1)");
                dspNetConsole.appendText("\n[HOP 2] SW-DSP-01 -> SW-CORE-01 via 10Gbps Trunk (VLAN 30 Tagged)");
                dspNetConsole.appendText("\n[HOP 3] SW-CORE-01 -> SRV-MAIN-01 Central Inventory (Stock Deducted)");
                dspNetConsole.appendText("\n[HOP 4] Storage Department Pick-List Generated for Aisle: " + selected.getRackLocation());
                dspNetConsole.appendText("\n[RESULT] Packet " + pkt.getPacketId() + " Status: " + pkt.getStatus() + " (Latency: " + String.format("%.2f", pkt.getCalculatedLatencyMs()) + " ms)");

                showNotification("Dispatch Order Successful", res.message(), Alert.AlertType.INFORMATION);
                refreshAllTables();
            } else {
                dspNetConsole.appendText("\n[DISPATCH REJECTED]: " + res.message());
                showNotification("Dispatch Denied", res.message(), Alert.AlertType.ERROR);
            }
        } catch (NumberFormatException ex) {
            showNotification("Invalid Quantity", "Please enter a valid numeric dispatch quantity.", Alert.AlertType.WARNING);
        }
    }

    private void setupDispatchTable() {
        dspTransactionsTable.setPrefHeight(180);
        dspTransactionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<InventoryTransaction, String> idCol = new TableColumn<>("Tx ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("transactionId"));

        TableColumn<InventoryTransaction, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        TableColumn<InventoryTransaction, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));

        TableColumn<InventoryTransaction, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<InventoryTransaction, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<InventoryTransaction, String> custCol = new TableColumn<>("Destination / Customer");
        custCol.setCellValueFactory(new PropertyValueFactory<>("partyInfo"));

        TableColumn<InventoryTransaction, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        dspTransactionsTable.getColumns().addAll(idCol, timeCol, skuCol, nameCol, qtyCol, custCol, statusCol);
    }

    // =========================================================================
    // 4. SECURITY OFFICE TAB
    // =========================================================================
    private Tab createSecurityTab() {
        Tab tab = new Tab("4. Security Office");
        tab.setClosable(false);

        VBox box = new VBox(15);
        box.setPadding(new Insets(15));

        Label header = new Label("SECURITY SURVEILLANCE, CCTV STREAMS & ACCESS CONTROL (VLAN 40)");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");

        // CCTV Camera Feeds Simulated Grid
        GridPane cctvGrid = new GridPane();
        cctvGrid.setHgap(15);
        cctvGrid.setVgap(15);

        cctvGrid.add(createCameraFeedCard("CAM-01: Main Warehouse Gate", "192.168.40.21", "1080p @ 30 FPS", "4.5 Mbps RTSP/UDP"), 0, 0);
        cctvGrid.add(createCameraFeedCard("CAM-02: Inbound Loading Bay", "192.168.40.22", "1080p @ 30 FPS", "4.2 Mbps RTSP/UDP"), 1, 0);
        cctvGrid.add(createCameraFeedCard("CAM-03: Storage High-Bay Aisle", "192.168.40.23", "4K @ 20 FPS", "7.8 Mbps RTSP/UDP"), 0, 1);
        cctvGrid.add(createCameraFeedCard("CAM-04: Outbound Dispatch Bay", "192.168.40.24", "1080p @ 30 FPS", "4.4 Mbps RTSP/UDP"), 1, 1);

        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            cctvGrid.getColumnConstraints().add(cc);
        }

        // Unauthorized Cross-VLAN Test Box
        VBox testBox = new VBox(10);
        testBox.getStyleClass().add("metric-card");

        Label testTitle = new Label("SECURITY SEGMENTATION TEST: ATTEMPT UNAUTHORIZED CROSS-VLAN ACCESS");
        testTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

        Label testDesc = new Label("Test whether CCTV IP Cameras (VLAN 40) can connect directly to Dispatch Workstations (VLAN 30), violating firewall rule FW-30.");
        testDesc.setStyle("-fx-text-fill: #94A3B8;");

        Button btnTestViolation = new Button("Simulate Blocked Cross-VLAN Connection (CAM-SEC-01 -> WS-DSP-01)");
        btnTestViolation.getStyleClass().add("btn-danger");
        btnTestViolation.setOnAction(e -> testUnauthorizedAccess());

        secAlertConsole.setEditable(false);
        secAlertConsole.setPrefHeight(100);
        secAlertConsole.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        secAlertConsole.setText("[SECURITY AUDIT] All access control devices and VLAN 40 segmentation active.");

        testBox.getChildren().addAll(testTitle, testDesc, btnTestViolation, secAlertConsole);

        box.getChildren().addAll(header, cctvGrid, testBox);
        ScrollPane spSecurity = new ScrollPane(box);
        spSecurity.setFitToWidth(true);
        tab.setContent(spSecurity);
        return tab;
    }

    private VBox createCameraFeedCard(String camName, String ip, String res, String bitrate) {
        VBox card = new VBox(6);
        card.getStyleClass().add("metric-card");

        Label name = new Label(camName);
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #F8FAFC; -fx-font-size: 13px;");

        HBox liveTag = new HBox(8);
        liveTag.setAlignment(Pos.CENTER_LEFT);
        Label recDot = new Label("● LIVE STREAM");
        recDot.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 11px;");
        Label ipLbl = new Label("IP: " + ip + " | VLAN 40");
        ipLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        liveTag.getChildren().addAll(recDot, ipLbl);

        // Simulated camera video preview placeholder
        Pane videoPreview = new Pane();
        videoPreview.setPrefHeight(110);
        videoPreview.setStyle("-fx-background-color: #0A0F1D; -fx-border-color: #1E293B; -fx-border-radius: 4;");

        Label feedOverlay = new Label(res + " | " + bitrate + "\nSimulated RTSP Stream (Port 554)");
        feedOverlay.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-padding: 8;");
        videoPreview.getChildren().add(feedOverlay);

        card.getChildren().addAll(name, liveTag, videoPreview);
        return card;
    }

    private void testUnauthorizedAccess() {
        PacketHeader hdr = new PacketHeader("192.168.40.21", "192.168.30.11", 5004, 80, "TCP", 64, 9999, 0, 40);
        NetworkPacket pkt = new NetworkPacket("PKT-ATTEMPT-UNAUTH", "CAM-SEC-01", "WS-DSP-01", hdr, "UNAUTHORIZED_CROSS_VLAN_PROBE", 64);
        NetworkPacket result = simEngine.transmitPacket(pkt);

        secAlertConsole.appendText("\n\n" + "!".repeat(60));
        secAlertConsole.appendText("\n[FIREWALL ENFORCEMENT]: Connection packet " + result.getPacketId() + " intercepted!");
        secAlertConsole.appendText("\n[POLICY TRIGGERED]: Rule FW-30 (Block CCTV Cross-Talk to Dispatch Stations)");
        secAlertConsole.appendText("\n[DECISION]: " + result.getStatus() + " - " + result.getDropReason());
        secAlertConsole.appendText("\n[ACTION]: Unauthorized packet dropped at Core Switch L3 ACL filter.");

        showNotification("Firewall Block Event", "Unauthorized connection from CCTV to Dispatch Station was DENIED by warehouse firewall.", Alert.AlertType.WARNING);
    }

    // =========================================================================
    // 5. MANAGER OFFICE TAB
    // =========================================================================
    private Tab createManagerTab() {
        Tab tab = new Tab("5. Manager Office");
        tab.setClosable(false);

        VBox box = new VBox(15);
        box.setPadding(new Insets(15));

        Label header = new Label("EXECUTIVE MANAGEMENT CONSOLE, AUDIT REPORTS & CSV EXPORT");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #818CF8;");

        // KPI Summary
        HBox kpiRow = new HBox(15);
        kpiRow.getChildren().addAll(
                createMgrKpi("TOTAL INVENTORY VALUATION", mgrTotalValueLabel, "#10B981"),
                createMgrKpi("TOTAL STOCK UNITS", mgrTotalUnitsLabel, "#38BDF8"),
                createMgrKpi("LOW STOCK ALERTS", mgrLowStockLabel, "#F87171")
        );

        // Action Export Bar
        HBox exportBar = new HBox(15);
        exportBar.setAlignment(Pos.CENTER_LEFT);
        exportBar.getStyleClass().add("metric-card");

        Button btnExportInv = new Button("Export Inventory Catalog to CSV");
        btnExportInv.getStyleClass().add("btn-primary");
        btnExportInv.setOnAction(e -> handleExportInventory());

        Button btnExportTx = new Button("Export Transaction Ledger to CSV");
        btnExportTx.getStyleClass().add("btn-secondary");
        btnExportTx.setOnAction(e -> handleExportTransactions());

        Button btnExportDev = new Button("Export Network Devices to CSV");
        btnExportDev.getStyleClass().add("btn-secondary");
        btnExportDev.setOnAction(e -> handleExportDevices());

        exportBar.getChildren().addAll(btnExportInv, btnExportTx, btnExportDev);

        // Summary Table
        VBox tableBox = new VBox(8);
        tableBox.getStyleClass().add("metric-card");
        Label tableTitle = new Label("CONSOLIDATED WAREHOUSE INVENTORY OVERVIEW");
        tableTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

        setupManagerTable();
        tableBox.getChildren().addAll(tableTitle, mgrSummaryTable);

        box.getChildren().addAll(header, kpiRow, exportBar, tableBox);
        ScrollPane spManager = new ScrollPane(box);
        spManager.setFitToWidth(true);
        tab.setContent(spManager);
        return tab;
    }

    private VBox createMgrKpi(String title, Label valLbl, String color) {
        VBox card = new VBox(6);
        card.getStyleClass().add("metric-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label t = new Label(title);
        t.getStyleClass().add("card-title");

        valLbl.getStyleClass().add("card-value");
        valLbl.setStyle("-fx-text-fill: " + color + ";");

        card.getChildren().addAll(t, valLbl);
        return card;
    }

    private void setupManagerTable() {
        mgrSummaryTable.setPrefHeight(220);
        mgrSummaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<InventoryItem, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));

        TableColumn<InventoryItem, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<InventoryItem, String> deptCol = new TableColumn<>("Department Zone");
        deptCol.setCellValueFactory(new PropertyValueFactory<>("departmentZone"));

        TableColumn<InventoryItem, Integer> qtyCol = new TableColumn<>("In Stock");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<InventoryItem, Double> priceCol = new TableColumn<>("Unit Price ($)");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<InventoryItem, String> rackCol = new TableColumn<>("Location Rack");
        rackCol.setCellValueFactory(new PropertyValueFactory<>("rackLocation"));

        mgrSummaryTable.getColumns().addAll(skuCol, nameCol, deptCol, qtyCol, priceCol, rackCol);
    }

    private void handleExportInventory() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Warehouse Inventory CSV");
        fc.setInitialFileName("warehouse_inventory_export.csv");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                CsvExporter.exportInventoryToCsv(inventoryService.getAllItems(), file);
                showNotification("Export Successful", "Saved inventory to: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showNotification("Export Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void handleExportTransactions() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Transaction Ledger CSV");
        fc.setInitialFileName("warehouse_transactions_export.csv");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                CsvExporter.exportTransactionsToCsv(inventoryService.getTransactions(), file);
                showNotification("Export Successful", "Saved transactions to: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showNotification("Export Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void handleExportDevices() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Network Devices CSV");
        fc.setInitialFileName("network_devices_export.csv");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                CsvExporter.exportDevicesToCsv(topology.getDevices().stream().toList(), file);
                showNotification("Export Successful", "Saved devices to: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showNotification("Export Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    public void refreshAllTables() {
        List<InventoryItem> allItems = inventoryService.getAllItems();
        List<InventoryItem> lowStock = inventoryService.getLowStockItems();
        List<InventoryTransaction> allTx = inventoryService.getTransactions();

        storageTable.setItems(FXCollections.observableArrayList(allItems));
        lowStockTable.setItems(FXCollections.observableArrayList(lowStock));
        dspTransactionsTable.setItems(FXCollections.observableArrayList(allTx));
        mgrSummaryTable.setItems(FXCollections.observableArrayList(allItems));

        dspSkuCombo.setItems(FXCollections.observableArrayList(allItems));
        if (!allItems.isEmpty() && dspSkuCombo.getValue() == null) {
            dspSkuCombo.setValue(allItems.get(0));
        }

        Map<String, Object> summary = inventoryService.getConsolidatedWarehouseSummary();
        mgrTotalValueLabel.setText(String.format("$%,.2f", (Double) summary.get("totalValuationUsd")));
        mgrTotalUnitsLabel.setText(summary.get("totalStockUnits") + " Units");
        mgrLowStockLabel.setText(String.valueOf(summary.get("lowStockAlertsCount")));
    }

    private void showNotification(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
