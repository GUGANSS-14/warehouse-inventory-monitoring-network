package com.wimn.views;

import com.wimn.models.NetworkDevice;
import com.wimn.routing.RoutingEngine;
import com.wimn.routing.RoutingEntry;
import com.wimn.routing.RoutingTable;
import com.wimn.topology.WarehouseTopology;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.*;

public class RoutingLabView extends BorderPane {
    private final WarehouseTopology topology;
    private final RoutingEngine routingEngine;

    private final ComboBox<NetworkDevice> dijkstraSrcCombo = new ComboBox<>();
    private final ComboBox<NetworkDevice> dijkstraDstCombo = new ComboBox<>();
    private final TextArea dijkstraLogsArea = new TextArea();
    private final Label dijkstraPathLabel = new Label("-");
    private final Label dijkstraCostLabel = new Label("-");

    private final ComboBox<NetworkDevice> tableDeviceCombo = new ComboBox<>();
    private final TableView<RoutingEntry> routingTableView = new TableView<>();

    private final TextArea dvLogsArea = new TextArea();

    public RoutingLabView(WarehouseTopology topology, RoutingEngine routingEngine) {
        this.topology = topology;
        this.routingEngine = routingEngine;

        setPadding(new Insets(15));
        setCenter(createContent());

        setupRoutingTable();
        populateCombos();
    }

    private ScrollPane createContent() {
        VBox root = new VBox(20);
        root.setFillWidth(true);

        Label header = new Label("UNIT-III: ROUTING ALGORITHMS & FORWARDING LAB");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        // 1. Link-State Dijkstra Section
        VBox lsBox = new VBox(12);
        lsBox.getStyleClass().add("metric-card");

        Label lsTitle = new Label("1. LINK-STATE ROUTING (DIJKSTRA'S SHORTEST PATH ALGORITHM - OSPF)");
        lsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10B981;");

        HBox lsInputRow = new HBox(12);
        lsInputRow.setAlignment(Pos.CENTER_LEFT);

        dijkstraSrcCombo.setPrefWidth(220);
        dijkstraDstCombo.setPrefWidth(220);

        Button btnRunDijkstra = new Button("Calculate Shortest Route");
        btnRunDijkstra.getStyleClass().add("btn-primary");
        btnRunDijkstra.setOnAction(e -> runDijkstra());

        lsInputRow.getChildren().addAll(new Label("Source Node:"), dijkstraSrcCombo, new Label("Destination:"), dijkstraDstCombo, btnRunDijkstra);

        HBox lsResultRow = new HBox(25);
        lsResultRow.setAlignment(Pos.CENTER_LEFT);

        dijkstraPathLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");
        dijkstraCostLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #34D399;");

        lsResultRow.getChildren().addAll(
                new HBox(8, new Label("Computed Path:"), dijkstraPathLabel),
                new HBox(8, new Label("Total Latency / Metric:"), dijkstraCostLabel)
        );

        dijkstraLogsArea.setEditable(false);
        dijkstraLogsArea.setPrefHeight(140);
        dijkstraLogsArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        dijkstraLogsArea.setText("Select source & destination nodes and click 'Calculate Shortest Route'.");

        lsBox.getChildren().addAll(lsTitle, lsInputRow, lsResultRow, dijkstraLogsArea);

        // 2. Distance-Vector Bellman-Ford Section
        VBox dvBox = new VBox(12);
        dvBox.getStyleClass().add("metric-card");

        Label dvTitle = new Label("2. DISTANCE-VECTOR ROUTING (BELLMAN-FORD ITERATION & CONVERGENCE - RIP)");
        dvTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #F59E0B;");

        Button btnRunDV = new Button("Run Bellman-Ford Distributed Convergence");
        btnRunDV.getStyleClass().add("btn-secondary");
        btnRunDV.setOnAction(e -> runDistanceVector());

        dvLogsArea.setEditable(false);
        dvLogsArea.setPrefHeight(150);
        dvLogsArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        dvLogsArea.setText("Click 'Run Bellman-Ford Distributed Convergence' to view vector updates and iteration matrices.");

        dvBox.getChildren().addAll(dvTitle, btnRunDV, dvLogsArea);

        // 3. Router Forwarding Table Inspector
        VBox tableBox = new VBox(10);
        tableBox.getStyleClass().add("metric-card");

        Label tableTitle = new Label("3. ROUTING TABLE INSPECTOR (FORWARDING INFORMATION BASE - FIB)");
        tableTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #818CF8;");

        HBox tableControlRow = new HBox(12);
        tableControlRow.setAlignment(Pos.CENTER_LEFT);

        tableDeviceCombo.setPrefWidth(250);
        tableDeviceCombo.setOnAction(e -> inspectDeviceRoutingTable());

        tableControlRow.getChildren().addAll(new Label("Select Routing Node:"), tableDeviceCombo);

        tableBox.getChildren().addAll(tableTitle, tableControlRow, routingTableView);

        root.getChildren().addAll(header, lsBox, dvBox, tableBox);
        return new ScrollPane(root);
    }

    private void populateCombos() {
        List<NetworkDevice> devs = new ArrayList<>(topology.getDevices());
        devs.sort(Comparator.comparing(NetworkDevice::getId));

        dijkstraSrcCombo.setItems(FXCollections.observableArrayList(devs));
        dijkstraDstCombo.setItems(FXCollections.observableArrayList(devs));
        tableDeviceCombo.setItems(FXCollections.observableArrayList(devs));

        if (devs.size() >= 2) {
            dijkstraSrcCombo.setValue(devs.stream().filter(d -> d.getId().equals("WS-RCV-01")).findFirst().orElse(devs.get(0)));
            dijkstraDstCombo.setValue(devs.stream().filter(d -> d.getId().equals("SRV-MAIN-01")).findFirst().orElse(devs.get(1)));
            tableDeviceCombo.setValue(devs.stream().filter(d -> d.getId().equals("SW-CORE-01")).findFirst().orElse(devs.get(0)));
            inspectDeviceRoutingTable();
        }
    }

    private void runDijkstra() {
        NetworkDevice src = dijkstraSrcCombo.getValue();
        NetworkDevice dst = dijkstraDstCombo.getValue();
        if (src == null || dst == null) return;

        RoutingEngine.DijkstraResult res = routingEngine.findShortestPath(src.getId(), dst.getId());

        if (res.pathFound()) {
            dijkstraPathLabel.setText(String.join("  →  ", res.path()));
            dijkstraCostLabel.setText(String.format("%.2f ms", res.totalCost()));
        } else {
            dijkstraPathLabel.setText("NO ROUTE FOUND (Destination Unreachable)");
            dijkstraCostLabel.setText("INFINITY");
        }

        dijkstraLogsArea.clear();
        for (String log : res.stepLogs()) {
            dijkstraLogsArea.appendText(log + "\n");
        }
    }

    private void runDistanceVector() {
        RoutingEngine.DistanceVectorResult dv = routingEngine.computeDistanceVector();
        dvLogsArea.clear();
        dvLogsArea.appendText("Bellman-Ford Algorithm Execution for Warehouse Network Graph\n");
        dvLogsArea.appendText("============================================================\n");
        dvLogsArea.appendText("Convergence Status: " + (dv.converged() ? "CONVERGED (Stable state reached)" : "Count-to-Infinity Detected") + "\n");
        dvLogsArea.appendText("Total Iteration Cycles: " + dv.totalIterations() + "\n\n");

        dvLogsArea.appendText("Relaxation Log Entries:\n");
        for (RoutingEngine.DistanceVectorStep step : dv.stepHistory()) {
            dvLogsArea.appendText(String.format("  [Cycle %d] Node %s -> Dest %s cost updated to %.2f via NextHop %s\n",
                    step.iteration(), step.node(), step.destination(), step.cost(), step.nextHop()));
        }

        dvLogsArea.appendText("\nFinal Distance Vector Matrix Sample (SW-CORE-01):\n");
        Map<String, Double> coreVector = dv.distanceMatrix().get("SW-CORE-01");
        if (coreVector != null) {
            coreVector.forEach((dest, cost) -> {
                dvLogsArea.appendText(String.format("  Destination: %-15s Cost: %.2f ms\n", dest, cost));
            });
        }
    }

    private void setupRoutingTable() {
        routingTableView.setPrefHeight(180);
        routingTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<RoutingEntry, String> destCol = new TableColumn<>("Destination Node");
        destCol.setCellValueFactory(new PropertyValueFactory<>("destinationNode"));

        TableColumn<RoutingEntry, String> subCol = new TableColumn<>("Subnet / Target IP");
        subCol.setCellValueFactory(new PropertyValueFactory<>("destinationSubnet"));

        TableColumn<RoutingEntry, String> nextCol = new TableColumn<>("Next Hop");
        nextCol.setCellValueFactory(new PropertyValueFactory<>("nextHopNode"));

        TableColumn<RoutingEntry, Double> metricCol = new TableColumn<>("Metric / Cost");
        metricCol.setCellValueFactory(new PropertyValueFactory<>("costMetric"));
        metricCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f ms", item));
            }
        });

        TableColumn<RoutingEntry, String> ifaceCol = new TableColumn<>("Out Interface");
        ifaceCol.setCellValueFactory(new PropertyValueFactory<>("outInterface"));

        TableColumn<RoutingEntry, String> protoCol = new TableColumn<>("Protocol");
        protoCol.setCellValueFactory(new PropertyValueFactory<>("routingProtocol"));

        routingTableView.getColumns().addAll(destCol, subCol, nextCol, metricCol, ifaceCol, protoCol);
    }

    private void inspectDeviceRoutingTable() {
        NetworkDevice dev = tableDeviceCombo.getValue();
        if (dev == null) return;
        RoutingTable table = routingEngine.buildRoutingTable(dev.getId());
        routingTableView.setItems(FXCollections.observableArrayList(table.getEntries()));
    }
}
