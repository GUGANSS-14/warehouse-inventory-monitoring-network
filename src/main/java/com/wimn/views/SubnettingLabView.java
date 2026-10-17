package com.wimn.views;

import com.wimn.models.SubnetInfo;
import com.wimn.protocols.SubnetCalculator;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SubnettingLabView extends BorderPane {
    private final TextField ipInputField = new TextField("192.168.10.45");
    private final Slider prefixSlider = new Slider(0, 32, 24);
    private final Label prefixLabel = new Label("/24");

    // Results Labels
    private final Label netAddrLbl = new Label("-");
    private final Label maskLbl = new Label("-");
    private final Label bcastLbl = new Label("-");
    private final Label rangeLbl = new Label("-");
    private final Label usableHostsLbl = new Label("-");
    private final Label totalAddrsLbl = new Label("-");
    private final Label ipClassLbl = new Label("-");
    private final Label wildcardLbl = new Label("-");
    private final Label binaryIpLbl = new Label("-");
    private final Label binaryMaskLbl = new Label("-");

    // IPv6 Controls
    private final TextField ipv6InputField = new TextField("2001:0db8:0000:0000:0000:0000:0000:0001");
    private final Label ipv6CompressedLbl = new Label("-");
    private final Label ipv6TypeLbl = new Label("-");

    public SubnettingLabView() {
        setPadding(new Insets(15));
        setCenter(createContent());

        calculateSubnet();
    }

    private ScrollPane createContent() {
        VBox root = new VBox(20);
        root.setFillWidth(true);

        Label header = new Label("UNIT-III: IP ADDRESSING, SUBNETTING & CIDR LAB");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        // Subnet Calculator Form
        VBox calcBox = new VBox(12);
        calcBox.getStyleClass().add("metric-card");

        Label calcTitle = new Label("INTERACTIVE IPv4 SUBNET CALCULATOR (RFC 791 / RFC 1519 / RFC 3021)");
        calcTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #F8FAFC;");

        HBox inputRow = new HBox(15);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        ipInputField.setPrefWidth(180);
        ipInputField.setPromptText("e.g. 192.168.1.1");

        prefixSlider.setShowTickMarks(true);
        prefixSlider.setShowTickLabels(true);
        prefixSlider.setMajorTickUnit(4);
        prefixSlider.setMinorTickCount(3);
        prefixSlider.setBlockIncrement(1);
        prefixSlider.setSnapToTicks(true);
        prefixSlider.setPrefWidth(280);

        prefixSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int p = newVal.intValue();
            prefixLabel.setText("/" + p);
            calculateSubnet();
        });

        prefixLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        Button btnCalc = new Button("Calculate Subnet");
        btnCalc.getStyleClass().add("btn-primary");
        btnCalc.setOnAction(e -> calculateSubnet());

        inputRow.getChildren().addAll(new Label("IPv4 Address:"), ipInputField, new Label("CIDR Prefix:"), prefixSlider, prefixLabel, btnCalc);

        // Quick Preset Buttons
        HBox presetsRow = new HBox(10);
        presetsRow.setAlignment(Pos.CENTER_LEFT);
        Label presetLbl = new Label("Warehouse Presets:");
        presetLbl.setStyle("-fx-text-fill: #94A3B8;");

        Button p24 = createPresetBtn("Warehouse LAN (/24)", "192.168.10.1", 24);
        Button p30 = createPresetBtn("Point-to-Point (/30)", "10.0.0.1", 30);
        Button p31 = createPresetBtn("Router Link (/31 RFC3021)", "10.0.0.0", 31);
        Button p32 = createPresetBtn("Host Loopback (/32)", "127.0.0.1", 32);
        Button p16 = createPresetBtn("Campus Network (/16)", "172.16.0.1", 16);

        presetsRow.getChildren().addAll(presetLbl, p24, p30, p31, p32, p16);

        // Results Grid
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 0, 0, 0));

        grid.add(createField("Network Address:", netAddrLbl, "#38BDF8"), 0, 0);
        grid.add(createField("Subnet Mask:", maskLbl, "#10B981"), 1, 0);
        grid.add(createField("Broadcast Address:", bcastLbl, "#F59E0B"), 2, 0);

        grid.add(createField("Usable Host Range:", rangeLbl, "#F8FAFC"), 0, 1);
        grid.add(createField("Usable Host Count:", usableHostsLbl, "#34D399"), 1, 1);
        grid.add(createField("Total Address Count:", totalAddrsLbl, "#818CF8"), 2, 1);

        grid.add(createField("IP Class:", ipClassLbl, "#E2E8F0"), 0, 2);
        grid.add(createField("Wildcard Mask:", wildcardLbl, "#E2E8F0"), 1, 2);

        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(33.3);
            grid.getColumnConstraints().add(cc);
        }

        // Binary 32-bit visualizer
        VBox binaryBox = new VBox(6);
        binaryBox.setStyle("-fx-background-color: #0F172A; -fx-padding: 12; -fx-background-radius: 6;");
        Label binTitle = new Label("32-BIT BINARY NETWORK & HOST MASK BREAKDOWN:");
        binTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        binaryIpLbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-text-fill: #38BDF8;");
        binaryMaskLbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-text-fill: #10B981;");

        binaryBox.getChildren().addAll(binTitle, binaryIpLbl, binaryMaskLbl);

        calcBox.getChildren().addAll(calcTitle, inputRow, presetsRow, new Separator(), grid, binaryBox);

        // IPv6 Addressing Section
        VBox ipv6Box = new VBox(12);
        ipv6Box.getStyleClass().add("metric-card");

        Label ipv6Title = new Label("IPv6 ADDRESSING & ZERO COMPRESSION DEMONSTRATION (RFC 5952)");
        ipv6Title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #A78BFA;");

        HBox ipv6InputRow = new HBox(12);
        ipv6InputRow.setAlignment(Pos.CENTER_LEFT);
        ipv6InputField.setPrefWidth(350);

        Button btnCompress = new Button("Compress & Validate IPv6");
        btnCompress.getStyleClass().add("btn-primary");
        btnCompress.setOnAction(e -> handleIpv6Compress());

        ipv6InputRow.getChildren().addAll(new Label("IPv6 Full Address:"), ipv6InputField, btnCompress);

        GridPane g6 = new GridPane();
        g6.setHgap(20);
        g6.setVgap(8);

        g6.add(createField("RFC 5952 Compressed:", ipv6CompressedLbl, "#A78BFA"), 0, 0);
        g6.add(createField("Address Scope / Type:", ipv6TypeLbl, "#34D399"), 1, 0);

        ipv6Box.getChildren().addAll(ipv6Title, ipv6InputRow, g6);

        root.getChildren().addAll(header, calcBox, ipv6Box);
        return new ScrollPane(root);
    }

    private Button createPresetBtn(String label, String ip, int prefix) {
        Button btn = new Button(label);
        btn.getStyleClass().add("btn-secondary");
        btn.setOnAction(e -> {
            ipInputField.setText(ip);
            prefixSlider.setValue(prefix);
            calculateSubnet();
        });
        return btn;
    }

    private VBox createField(String title, Label valLbl, String colorHex) {
        VBox v = new VBox(4);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");
        valLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        v.getChildren().addAll(t, valLbl);
        return v;
    }

    private void calculateSubnet() {
        try {
            String ip = ipInputField.getText().trim();
            int prefix = (int) prefixSlider.getValue();
            SubnetInfo info = SubnetCalculator.calculate(ip, prefix);

            netAddrLbl.setText(info.getNetworkAddress() + "/" + info.getPrefixLength());
            maskLbl.setText(info.getSubnetMask());
            bcastLbl.setText(info.getBroadcastAddress());
            rangeLbl.setText(info.getFirstUsableHost() + "  to  " + info.getLastUsableHost());
            usableHostsLbl.setText(String.format("%,d hosts", info.getUsableHosts()));
            totalAddrsLbl.setText(String.format("%,d addresses", info.getTotalAddresses()));
            ipClassLbl.setText(info.getIpClass());
            wildcardLbl.setText(info.getWildCardMask());

            binaryIpLbl.setText("IP (Bin):   " + info.getBinaryIp());
            binaryMaskLbl.setText("Mask (Bin): " + info.getBinarySubnetMask());
        } catch (Exception ex) {
            netAddrLbl.setText("Invalid IP Format");
        }
    }

    private void handleIpv6Compress() {
        String raw = ipv6InputField.getText().trim();
        // RFC 5952 compression simulation
        if (raw.equalsIgnoreCase("::1") || raw.contains("0000:0000:0000:0000:0000:0000:0000:0001")) {
            ipv6CompressedLbl.setText("::1 (Loopback)");
            ipv6TypeLbl.setText("Node-Local Loopback (RFC 4291)");
        } else if (raw.startsWith("fe80")) {
            ipv6CompressedLbl.setText("fe80::1 (Link-Local)");
            ipv6TypeLbl.setText("Link-Local Unicast");
        } else if (raw.startsWith("2001:0db8")) {
            ipv6CompressedLbl.setText(raw.replaceFirst("(:0000)+", "::").replaceAll(":0{1,3}", ":"));
            ipv6TypeLbl.setText("Global Unicast (Documentation RFC 3849)");
        } else {
            ipv6CompressedLbl.setText(raw.replaceFirst("(:0000)+", "::").replaceAll(":0{1,3}", ":"));
            ipv6TypeLbl.setText("Global Unicast (2000::/3)");
        }
    }
}
