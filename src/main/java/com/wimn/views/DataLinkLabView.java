package com.wimn.views;

import com.wimn.protocols.CRCUtils;
import com.wimn.protocols.FlowControlDemo;
import com.wimn.protocols.FramingDemo;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

public class DataLinkLabView extends BorderPane {

    // Framing Controls
    private final TextField bitStreamInput = new TextField("0111111011111001111110");
    private final Label bitStuffedResult = new Label("-");
    private final Label bitDestuffedResult = new Label("-");

    // Error Control Controls
    private final TextField crcDataInput = new TextField("WAREHOUSE-PALLET-09");
    private final Label crc16Label = new Label("-");
    private final Label crc32Label = new Label("-");
    private final Label checksumLabel = new Label("-");
    private final Label corruptedStatusLabel = new Label("Original Frame Valid");
    private String currentPayload = "WAREHOUSE-PALLET-09";

    // Flow Control Controls
    private final ComboBox<FlowControlDemo.FlowType> flowTypeCombo = new ComboBox<>();
    private final Spinner<Integer> windowSizeSpinner = new Spinner<>(1, 8, 4);
    private final TableView<FlowControlDemo.TransmissionStep> flowTable = new TableView<>();

    // Switch MAC Learning Controls
    private final TableView<MacEntry> macTable = new TableView<>();
    private final TextArea macLogArea = new TextArea();

    public DataLinkLabView() {
        setPadding(new Insets(15));
        setCenter(createContent());

        calculateCRC();
        handleBitStuffing();
        runFlowControlSim();
        initMacLearningDemo();
    }

    private ScrollPane createContent() {
        VBox root = new VBox(20);
        root.setFillWidth(true);

        Label header = new Label("UNIT-II: DATA LINK LAYER SERVICES & PROTOCOLS LAB");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        // 1. Framing Lab
        VBox framingBox = new VBox(10);
        framingBox.getStyleClass().add("metric-card");
        Label fTitle = new Label("1. FRAMING: BIT STUFFING & BYTE STUFFING (HDLC RFC 1662)");
        fTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #38BDF8;");

        HBox fInputRow = new HBox(12);
        fInputRow.setAlignment(Pos.CENTER_LEFT);
        bitStreamInput.setPrefWidth(300);

        Button btnStuff = new Button("Apply HDLC Bit Stuffing");
        btnStuff.getStyleClass().add("btn-primary");
        btnStuff.setOnAction(e -> handleBitStuffing());

        fInputRow.getChildren().addAll(new Label("Raw Binary Bitstream:"), bitStreamInput, btnStuff);

        GridPane fGrid = new GridPane();
        fGrid.setHgap(20);
        fGrid.setVgap(8);
        fGrid.add(new Label("Stuffed Output (0 inserted after five 1s):"), 0, 0);
        bitStuffedResult.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #10B981; -fx-font-size: 13px;");
        fGrid.add(bitStuffedResult, 1, 0);

        fGrid.add(new Label("Receiver Destuffed Result:"), 0, 1);
        bitDestuffedResult.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #38BDF8; -fx-font-size: 13px;");
        fGrid.add(bitDestuffedResult, 1, 1);

        framingBox.getChildren().addAll(fTitle, fInputRow, fGrid);

        // 2. Error Control Lab (CRC & Checksum)
        VBox errorBox = new VBox(10);
        errorBox.getStyleClass().add("metric-card");
        Label eTitle = new Label("2. ERROR CONTROL: CYCLIC REDUNDANCY CHECK (CRC-16, CRC-32 & INTERNET CHECKSUM)");
        eTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10B981;");

        HBox eInputRow = new HBox(12);
        eInputRow.setAlignment(Pos.CENTER_LEFT);
        crcDataInput.setPrefWidth(260);

        Button btnCalcCRC = new Button("Generate CRCs");
        btnCalcCRC.getStyleClass().add("btn-primary");
        btnCalcCRC.setOnAction(e -> {
            currentPayload = crcDataInput.getText().trim();
            corruptedStatusLabel.setText("Original Frame Valid");
            corruptedStatusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
            calculateCRC();
        });

        Button btnCorrupt = new Button("Simulate Bit-Flip Error");
        btnCorrupt.getStyleClass().add("btn-danger");
        btnCorrupt.setOnAction(e -> simulateBitFlip());

        eInputRow.getChildren().addAll(new Label("Frame Payload:"), crcDataInput, btnCalcCRC, btnCorrupt);

        GridPane eGrid = new GridPane();
        eGrid.setHgap(20);
        eGrid.setVgap(8);
        eGrid.add(new Label("CRC-16 (Polynomial 0x1021):"), 0, 0);
        crc16Label.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");
        eGrid.add(crc16Label, 1, 0);

        eGrid.add(new Label("CRC-32 (IEEE 802.3 Ethernet FCS):"), 0, 1);
        crc32Label.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #34D399;");
        eGrid.add(crc32Label, 1, 1);

        eGrid.add(new Label("RFC 1071 16-Bit 1's Complement Checksum:"), 0, 2);
        checksumLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #F59E0B;");
        eGrid.add(checksumLabel, 1, 2);

        eGrid.add(new Label("Transmission Integrity Verification:"), 0, 3);
        corruptedStatusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
        eGrid.add(corruptedStatusLabel, 1, 3);

        errorBox.getChildren().addAll(eTitle, eInputRow, eGrid);

        // 3. Flow Control Lab (Stop-and-Wait & Sliding Window)
        VBox flowBox = new VBox(10);
        flowBox.getStyleClass().add("metric-card");
        Label flowTitle = new Label("3. FLOW CONTROL: STOP-AND-WAIT & SLIDING WINDOW (GO-BACK-N / SELECTIVE REPEAT)");
        flowTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #F59E0B;");

        HBox flowControls = new HBox(15);
        flowControls.setAlignment(Pos.CENTER_LEFT);

        flowTypeCombo.setItems(FXCollections.observableArrayList(FlowControlDemo.FlowType.values()));
        flowTypeCombo.setValue(FlowControlDemo.FlowType.SLIDING_WINDOW_GBN);

        Button btnRunFlow = new Button("Simulate Window Transmission");
        btnRunFlow.getStyleClass().add("btn-primary");
        btnRunFlow.setOnAction(e -> runFlowControlSim());

        flowControls.getChildren().addAll(new Label("Protocol:"), flowTypeCombo, new Label("Window Size (N):"), windowSizeSpinner, btnRunFlow);

        setupFlowTable();
        flowBox.getChildren().addAll(flowTitle, flowControls, flowTable);

        // 4. Switch MAC Address Learning Table
        VBox macBox = new VBox(10);
        macBox.getStyleClass().add("metric-card");
        Label macTitle = new Label("4. ETHERNET SWITCHING & DYNAMIC MAC ADDRESS LEARNING TABLE");
        macTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #818CF8;");

        HBox macControls = new HBox(12);
        macControls.setAlignment(Pos.CENTER_LEFT);
        Button btnFrame1 = new Button("Frame from WS-RCV-01 (Port 1)");
        btnFrame1.getStyleClass().add("btn-secondary");
        btnFrame1.setOnAction(e -> simulateFrameArrival("00:1A:2B:10:00:11", 1, "00:1A:2B:01:00:50"));

        Button btnFrame2 = new Button("Frame from SCN-RCV-01 (Port 2)");
        btnFrame2.getStyleClass().add("btn-secondary");
        btnFrame2.setOnAction(e -> simulateFrameArrival("00:1A:2B:10:00:21", 2, "00:1A:2B:10:00:11"));

        Button btnClearMac = new Button("Flush MAC Table");
        btnClearMac.getStyleClass().add("btn-secondary");
        btnClearMac.setOnAction(e -> {
            macTable.getItems().clear();
            macLogArea.setText("[MAC FLUSHED] Switch forwarding table emptied.");
        });

        macControls.getChildren().addAll(btnFrame1, btnFrame2, btnClearMac);

        HBox macDisplay = new HBox(15);
        setupMacTable();
        HBox.setHgrow(macTable, Priority.ALWAYS);

        macLogArea.setEditable(false);
        macLogArea.setPrefHeight(150);
        macLogArea.setPrefWidth(400);
        macLogArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

        macDisplay.getChildren().addAll(macTable, macLogArea);
        macBox.getChildren().addAll(macTitle, macControls, macDisplay);

        root.getChildren().addAll(header, framingBox, errorBox, flowBox, macBox);
        return new ScrollPane(root);
    }

    private void handleBitStuffing() {
        String raw = bitStreamInput.getText().trim();
        String stuffed = FramingDemo.bitStuff(raw);
        String destuffed = FramingDemo.bitDestuff(stuffed);

        bitStuffedResult.setText(stuffed);
        bitDestuffedResult.setText(destuffed + (destuffed.equals(raw) ? " [MATCH CONFIRMED]" : " [MISMATCH]"));
    }

    private void calculateCRC() {
        int c16 = CRCUtils.calculateCRC16(currentPayload);
        long c32 = CRCUtils.calculateCRC32(currentPayload);
        int cSum = CRCUtils.calculateChecksum16(currentPayload);

        crc16Label.setText(String.format("0x%04X (%d)", c16, c16));
        crc32Label.setText(String.format("0x%08X (%d)", c32, c32));
        checksumLabel.setText(String.format("0x%04X", cSum));
    }

    private void simulateBitFlip() {
        currentPayload = CRCUtils.flipBit(currentPayload, 3);
        calculateCRC();
        corruptedStatusLabel.setText("BIT ERROR INJECTED! CRC Checksum Mismatch detected at receiver. Frame DISCARDED.");
        corruptedStatusLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
    }

    private void setupFlowTable() {
        flowTable.setPrefHeight(180);
        flowTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<FlowControlDemo.TransmissionStep, Integer> stepCol = new TableColumn<>("Step");
        stepCol.setCellValueFactory(new PropertyValueFactory<>("stepNumber"));
        stepCol.setMaxWidth(60);

        TableColumn<FlowControlDemo.TransmissionStep, String> actCol = new TableColumn<>("Action");
        actCol.setCellValueFactory(new PropertyValueFactory<>("action"));

        TableColumn<FlowControlDemo.TransmissionStep, String> sndCol = new TableColumn<>("Sender State");
        sndCol.setCellValueFactory(new PropertyValueFactory<>("senderState"));

        TableColumn<FlowControlDemo.TransmissionStep, String> rcvCol = new TableColumn<>("Receiver State");
        rcvCol.setCellValueFactory(new PropertyValueFactory<>("receiverState"));

        TableColumn<FlowControlDemo.TransmissionStep, String> descCol = new TableColumn<>("Mechanism Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        flowTable.getColumns().addAll(stepCol, actCol, sndCol, rcvCol, descCol);
    }

    private void runFlowControlSim() {
        FlowControlDemo.FlowType type = flowTypeCombo.getValue();
        int winSize = windowSizeSpinner.getValue();
        List<FlowControlDemo.TransmissionStep> steps = FlowControlDemo.simulateFlow(type, 8, winSize, 2);
        flowTable.setItems(FXCollections.observableArrayList(steps));
    }

    public record MacEntry(String mac, int port, String vlan, String age) {
        public String getMac() { return mac; }
        public int getPort() { return port; }
        public String getVlan() { return vlan; }
        public String getAge() { return age; }
    }

    private void setupMacTable() {
        macTable.setPrefHeight(150);
        macTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<MacEntry, String> macCol = new TableColumn<>("Learned MAC Address");
        macCol.setCellValueFactory(new PropertyValueFactory<>("mac"));

        TableColumn<MacEntry, Integer> portCol = new TableColumn<>("Switch Port");
        portCol.setCellValueFactory(new PropertyValueFactory<>("port"));

        TableColumn<MacEntry, String> vlanCol = new TableColumn<>("VLAN");
        vlanCol.setCellValueFactory(new PropertyValueFactory<>("vlan"));

        TableColumn<MacEntry, String> ageCol = new TableColumn<>("Aging Status");
        ageCol.setCellValueFactory(new PropertyValueFactory<>("age"));

        macTable.getColumns().addAll(macCol, portCol, vlanCol, ageCol);
    }

    private void initMacLearningDemo() {
        macTable.getItems().add(new MacEntry("00:1A:2B:01:00:02", 24, "VLAN 1 (Trunk)", "Dynamic (300s)"));
        macLogArea.setText("[INIT] Core Trunk Port 24 learned upstream Core Switch MAC.");
    }

    private void simulateFrameArrival(String srcMac, int inPort, String dstMac) {
        // Check if MAC is in table
        boolean learned = macTable.getItems().stream().anyMatch(m -> m.getMac().equals(srcMac));
        if (!learned) {
            macTable.getItems().add(new MacEntry(srcMac, inPort, "VLAN 10", "Dynamic (300s)"));
            macLogArea.appendText("\n[MAC LEARNED] Frame ingress on Port " + inPort + " -> Associated " + srcMac + " with Port " + inPort);
        }

        boolean dstKnown = macTable.getItems().stream().anyMatch(m -> m.getMac().equals(dstMac));
        if (dstKnown) {
            MacEntry target = macTable.getItems().stream().filter(m -> m.getMac().equals(dstMac)).findFirst().get();
            macLogArea.appendText("\n[FORWARDING] Destination " + dstMac + " found in MAC table -> UNICAST forward out Port " + target.getPort());
        } else {
            macLogArea.appendText("\n[FLOODING] Destination " + dstMac + " unknown in MAC table! FLOODING frame to all active ports on VLAN 10 (except ingress port " + inPort + ").");
        }
    }
}
