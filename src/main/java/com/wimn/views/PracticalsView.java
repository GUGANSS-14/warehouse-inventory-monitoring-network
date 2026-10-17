package com.wimn.views;

import com.wimn.models.NetworkPacket;
import com.wimn.models.PacketHeader;
import com.wimn.practicals.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.List;

public class PracticalsView extends BorderPane {
    // TCP Chat Controls
    private LocalTcpChatServer server;
    private LocalTcpChatClient client1;
    private LocalTcpChatClient client2;

    private final TextArea chatServerLog = new TextArea();
    private final TextArea client1ChatLog = new TextArea();
    private final TextField client1Input = new TextField();
    private final Button btnStartServer = new Button("Start Local TCP Server (Port 9191)");
    private final Button btnStopServer = new Button("Stop Server");
    private final Button btnConnectClient1 = new Button("Connect Terminal 1");

    // UDP DNS Tool
    private final TextField dnsToolInput = new TextField("warehouse.local");
    private final TextArea dnsToolLog = new TextArea();

    // Packet Inspector
    private final TextArea packetInspectLog = new TextArea();

    public PracticalsView() {
        setPadding(new Insets(15));
        setCenter(createContent());

        setupCliCommands();
        runSamplePacketInspection();
    }

    private ScrollPane createContent() {
        VBox root = new VBox(20);
        root.setFillWidth(true);

        Label header = new Label("LABORATORY PRACTICAL EXPERIMENTS (JAVA SOCKETS & NETWORK COMMANDS)");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        // 1. Real Java TCP Socket Chat Lab
        VBox tcpBox = new VBox(12);
        tcpBox.getStyleClass().add("metric-card");
        Label tcpTitle = new Label("1. JAVA TCP SOCKET PROGRAMMING: LOCALHOST CHAT DEMONSTRATION");
        tcpTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10B981;");

        HBox serverControls = new HBox(12);
        serverControls.setAlignment(Pos.CENTER_LEFT);

        btnStartServer.getStyleClass().add("btn-primary");
        btnStartServer.setOnAction(e -> startLocalServer());

        btnStopServer.getStyleClass().add("btn-danger");
        btnStopServer.setDisable(true);
        btnStopServer.setOnAction(e -> stopLocalServer());

        btnConnectClient1.getStyleClass().add("btn-success");
        btnConnectClient1.setOnAction(e -> connectClient1());

        serverControls.getChildren().addAll(btnStartServer, btnStopServer, btnConnectClient1);

        HBox chatPanes = new HBox(15);
        // Server log
        VBox srvBox = new VBox(6);
        srvBox.getChildren().addAll(new Label("Server Console (127.0.0.1:9191):"), chatServerLog);
        chatServerLog.setEditable(false);
        chatServerLog.setPrefHeight(140);
        chatServerLog.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        HBox.setHgrow(srvBox, Priority.ALWAYS);

        // Client 1 log & input
        VBox cltBox = new VBox(6);
        HBox cltInputRow = new HBox(8);
        client1Input.setPromptText("Type message to send over TCP socket...");
        HBox.setHgrow(client1Input, Priority.ALWAYS);
        Button btnSendMsg = new Button("Send TCP");
        btnSendMsg.getStyleClass().add("btn-primary");
        btnSendMsg.setOnAction(e -> sendClient1Message());
        cltInputRow.getChildren().addAll(client1Input, btnSendMsg);

        client1ChatLog.setEditable(false);
        client1ChatLog.setPrefHeight(110);
        client1ChatLog.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        cltBox.getChildren().addAll(new Label("Client Terminal Stream:"), client1ChatLog, cltInputRow);
        HBox.setHgrow(cltBox, Priority.ALWAYS);

        chatPanes.getChildren().addAll(srvBox, cltBox);
        tcpBox.getChildren().addAll(tcpTitle, serverControls, chatPanes);

        // 2. Real Java UDP DNS Query Lab
        VBox udpBox = new VBox(10);
        udpBox.getStyleClass().add("metric-card");
        Label udpTitle = new Label("2. UDP SOCKET DNS RESOLUTION QUERY");
        udpTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #F59E0B;");

        HBox udpInputRow = new HBox(12);
        udpInputRow.setAlignment(Pos.CENTER_LEFT);
        dnsToolInput.setPrefWidth(260);

        Button btnQueryUdp = new Button("Perform UDP DNS Query");
        btnQueryUdp.getStyleClass().add("btn-primary");
        btnQueryUdp.setOnAction(e -> runUdpDns());

        udpInputRow.getChildren().addAll(new Label("Domain Name:"), dnsToolInput, btnQueryUdp);

        dnsToolLog.setEditable(false);
        dnsToolLog.setPrefHeight(90);
        dnsToolLog.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

        udpBox.getChildren().addAll(udpTitle, udpInputRow, dnsToolLog);

        // 3. Packet Header Inspector & Decoder
        VBox packetBox = new VBox(10);
        packetBox.getStyleClass().add("metric-card");
        Label pktTitle = new Label("3. PACKET HEADER LEARNING & PROTOCOL-LAYER DECODER (OSI INSPECTION)");
        pktTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #818CF8;");

        Button btnInspectSample = new Button("Decode Warehouse Receiving Packet (Ethernet + IPv4 + TCP)");
        btnInspectSample.getStyleClass().add("btn-secondary");
        btnInspectSample.setOnAction(e -> runSamplePacketInspection());

        packetInspectLog.setEditable(false);
        packetInspectLog.setPrefHeight(200);
        packetInspectLog.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

        packetBox.getChildren().addAll(pktTitle, btnInspectSample, packetInspectLog);

        // 4. Network CLI Reference
        VBox cliBox = new VBox(10);
        cliBox.getStyleClass().add("metric-card");
        Label cliTitle = new Label("4. ESSENTIAL COMPUTER NETWORKING COMMAND-LINE REFERENCE");
        cliTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #38BDF8;");

        TabPane cliTabPane = new TabPane();
        for (NetworkCliHelper.CommandDoc cmd : NetworkCliHelper.getReferenceCommands()) {
            Tab t = new Tab(cmd.commandName());
            t.setClosable(false);

            VBox tabContent = new VBox(8);
            tabContent.setPadding(new Insets(10));

            Label purpose = new Label("Purpose: " + cmd.purpose());
            purpose.setStyle("-fx-font-weight: bold; -fx-text-fill: #38BDF8;");

            Label winSyn = new Label("Windows Syntax: " + cmd.windowsSyntax());
            winSyn.setStyle("-fx-font-family: 'Consolas', monospace; -fx-text-fill: #10B981;");

            Label linSyn = new Label("Linux/Unix Syntax: " + cmd.linuxSyntax());
            linSyn.setStyle("-fx-font-family: 'Consolas', monospace; -fx-text-fill: #F59E0B;");

            TextArea sample = new TextArea(cmd.sampleOutput());
            sample.setEditable(false);
            sample.setPrefHeight(130);
            sample.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

            tabContent.getChildren().addAll(purpose, winSyn, linSyn, new Label("Terminal Output Demonstration:"), sample);
            t.setContent(tabContent);
            cliTabPane.getTabs().add(t);
        }

        cliBox.getChildren().addAll(cliTitle, cliTabPane);

        root.getChildren().addAll(header, tcpBox, udpBox, packetBox, cliBox);
        return new ScrollPane(root);
    }

    private void startLocalServer() {
        try {
            server = new LocalTcpChatServer(9191, msg -> Platform.runLater(() -> chatServerLog.appendText(msg + "\n")));
            server.startServer();
            btnStartServer.setDisable(true);
            btnStopServer.setDisable(false);
        } catch (IOException ex) {
            chatServerLog.appendText("[ERROR] " + ex.getMessage() + "\n");
        }
    }

    private void stopLocalServer() {
        if (server != null) {
            server.stopServer();
            btnStartServer.setDisable(false);
            btnStopServer.setDisable(true);
        }
    }

    private void connectClient1() {
        if (server == null || !server.isRunning()) {
            client1ChatLog.setText("[ERROR] Start the TCP Server first!\n");
            return;
        }
        client1 = new LocalTcpChatClient(msg -> Platform.runLater(() -> client1ChatLog.appendText(msg + "\n")));
        try {
            client1.connect("127.0.0.1", 9191, "ReceivingDesk-PC");
        } catch (IOException e) {
            client1ChatLog.appendText("[ERROR] Connection failed: " + e.getMessage() + "\n");
        }
    }

    private void sendClient1Message() {
        String msg = client1Input.getText().trim();
        if (msg.isEmpty() || client1 == null || !client1.isConnected()) return;
        client1.sendMessage(msg);
        client1Input.clear();
    }

    private void runUdpDns() {
        String query = dnsToolInput.getText().trim();
        UdpDnsQueryTool.DnsResult res = UdpDnsQueryTool.queryDns(query);
        dnsToolLog.clear();
        for (String l : res.rawLog()) {
            dnsToolLog.appendText(l + "\n");
        }
    }

    private void runSamplePacketInspection() {
        PacketHeader hdr = new PacketHeader("192.168.10.21", "192.168.1.50", 49200, 8080, "TCP", 64, 1500, 3000, 10);
        hdr.setTcpFlags("PSH-ACK");
        NetworkPacket pkt = new NetworkPacket("PKT-SAMPLE-RCV-01", "SCN-RCV-01", "SRV-MAIN-01", hdr, "BARCODE_SCAN:SKU-NET-03:QTY=5", 148);
        pkt.setStatus(NetworkPacket.PacketStatus.DELIVERED);

        PacketInspectorTool.PacketInspectionReport report = PacketInspectorTool.inspect(pkt);
        packetInspectLog.clear();
        packetInspectLog.appendText("SUMMARY: " + report.summary() + "\n\n");
        for (PacketInspectorTool.DecodedLayer layer : report.layers()) {
            packetInspectLog.appendText("=".repeat(50) + "\n");
            packetInspectLog.appendText(String.format("OSI LAYER %d: %s\n", layer.osiLevel(), layer.layerName()));
            packetInspectLog.appendText("=".repeat(50) + "\n");
            for (String f : layer.fields()) {
                packetInspectLog.appendText("  " + f + "\n");
            }
            packetInspectLog.appendText("\n");
        }
        packetInspectLog.appendText("RAW HEXADECIMAL DUMP:\n" + report.hexDump());
    }

    private void setupCliCommands() {
        // Populated in createContent
    }
}
