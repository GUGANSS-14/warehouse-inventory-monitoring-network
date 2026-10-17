package com.wimn.views;

import com.wimn.models.InventoryTransaction;
import com.wimn.models.NetworkEvent;
import com.wimn.inventory.InventoryService;
import com.wimn.repositories.EventRepository;
import com.wimn.utilities.CsvExporter;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

public class ReportsLogsView extends BorderPane {
    private final EventRepository eventRepo;
    private final InventoryService inventoryService;

    private final TableView<NetworkEvent> eventTable = new TableView<>();
    private final TableView<InventoryTransaction> txTable = new TableView<>();

    public ReportsLogsView(EventRepository eventRepo, InventoryService inventoryService) {
        this.eventRepo = eventRepo;
        this.inventoryService = inventoryService;

        setPadding(new Insets(15));
        setCenter(createContent());

        setupEventTable();
        setupTxTable();
        refreshTables();
    }

    private ScrollPane createContent() {
        VBox root = new VBox(20);
        root.setFillWidth(true);

        Label header = new Label("SYSTEM AUDIT LOGS, TRANSACTION LEDGER & CSV EXPORTS");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        // Action Toolbar
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("metric-card");

        Button btnRefresh = new Button("Refresh Logs");
        btnRefresh.getStyleClass().add("btn-primary");
        btnRefresh.setOnAction(e -> refreshTables());

        Button btnExportTx = new Button("Export Transactions CSV");
        btnExportTx.getStyleClass().add("btn-secondary");
        btnExportTx.setOnAction(e -> exportTransactions());

        bar.getChildren().addAll(btnRefresh, btnExportTx);

        // 1. System Events Table
        VBox evBox = new VBox(8);
        evBox.getStyleClass().add("metric-card");
        Label evTitle = new Label("REAL-TIME NETWORK & SYSTEM EVENT STREAM");
        evTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #F8FAFC;");
        evBox.getChildren().addAll(evTitle, eventTable);

        // 2. Transactions Table
        VBox txBox = new VBox(8);
        txBox.getStyleClass().add("metric-card");
        Label txTitle = new Label("INVENTORY INTAKE & DISPATCH TRANSACTION AUDIT LEDGER");
        txTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10B981;");
        txBox.getChildren().addAll(txTitle, txTable);

        root.getChildren().addAll(header, bar, evBox, txBox);
        return new ScrollPane(root);
    }

    private void setupEventTable() {
        eventTable.setPrefHeight(250);
        eventTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<NetworkEvent, String> idCol = new TableColumn<>("Event ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("eventId"));
        idCol.setMaxWidth(120);

        TableColumn<NetworkEvent, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeCol.setMaxWidth(90);

        TableColumn<NetworkEvent, NetworkEvent.Severity> sevCol = new TableColumn<>("Severity");
        sevCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        sevCol.setMaxWidth(100);

        TableColumn<NetworkEvent, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setMaxWidth(110);

        TableColumn<NetworkEvent, String> deptCol = new TableColumn<>("Origin");
        deptCol.setCellValueFactory(new PropertyValueFactory<>("sourceDepartment"));
        deptCol.setMaxWidth(150);

        TableColumn<NetworkEvent, String> msgCol = new TableColumn<>("Event Detail");
        msgCol.setCellValueFactory(new PropertyValueFactory<>("message"));

        eventTable.getColumns().addAll(idCol, timeCol, sevCol, catCol, deptCol, msgCol);
    }

    private void setupTxTable() {
        txTable.setPrefHeight(220);
        txTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<InventoryTransaction, String> idCol = new TableColumn<>("Tx ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("transactionId"));

        TableColumn<InventoryTransaction, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        TableColumn<InventoryTransaction, InventoryTransaction.TransactionType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<InventoryTransaction, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));

        TableColumn<InventoryTransaction, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<InventoryTransaction, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<InventoryTransaction, String> partyCol = new TableColumn<>("Carrier / Customer");
        partyCol.setCellValueFactory(new PropertyValueFactory<>("partyInfo"));

        TableColumn<InventoryTransaction, String> pktCol = new TableColumn<>("Network Packet ID");
        pktCol.setCellValueFactory(new PropertyValueFactory<>("networkPacketId"));

        TableColumn<InventoryTransaction, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        txTable.getColumns().addAll(idCol, timeCol, typeCol, skuCol, nameCol, qtyCol, partyCol, pktCol, statusCol);
    }

    private void refreshTables() {
        eventTable.setItems(FXCollections.observableArrayList(eventRepo.getAllEvents()));
        txTable.setItems(FXCollections.observableArrayList(inventoryService.getTransactions()));
    }

    private void exportTransactions() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Transactions Ledger to CSV");
        fc.setInitialFileName("warehouse_transactions.csv");
        File f = fc.showSaveDialog(getScene().getWindow());
        if (f != null) {
            try {
                CsvExporter.exportTransactionsToCsv(inventoryService.getTransactions(), f);
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Exported successfully to " + f.getAbsolutePath());
                a.showAndWait();
            } catch (IOException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                a.showAndWait();
            }
        }
    }
}
