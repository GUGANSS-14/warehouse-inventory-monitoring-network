package com.wimn.views;

import com.wimn.models.*;
import com.wimn.repositories.DeviceRepository;
import com.wimn.topology.WarehouseTopology;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.Optional;

public class DeviceManagementView extends BorderPane {
    private final DeviceRepository deviceRepo;
    private final WarehouseTopology topology;

    private final TableView<NetworkDevice> deviceTable = new TableView<>();
    private final javafx.collections.ObservableList<NetworkDevice> masterDeviceList;
    private final FilteredList<NetworkDevice> filteredDevices;

    private final TextField searchField = new TextField();
    private final ComboBox<String> deptFilter = new ComboBox<>();
    private final ComboBox<String> typeFilter = new ComboBox<>();

    public DeviceManagementView(DeviceRepository deviceRepo, WarehouseTopology topology) {
        this.deviceRepo = deviceRepo;
        this.topology = topology;
        this.masterDeviceList = FXCollections.observableArrayList(deviceRepo.getAllDevices());
        this.filteredDevices = new FilteredList<>(masterDeviceList, p -> true);

        setPadding(new Insets(15));
        setTop(createFilterToolbar());
        setCenter(createTableArea());

        setupTable();
    }

    private HBox createFilterToolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 12, 0));
        bar.getStyleClass().add("metric-card");

        searchField.setPromptText("Search by Name, IP, MAC or ID...");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        deptFilter.getItems().addAll("All Departments", "RECEIVING", "STORAGE", "DISPATCH", "SECURITY", "MANAGER", "CORE_INFRA");
        deptFilter.setValue("All Departments");
        deptFilter.setOnAction(e -> applyFilters());

        typeFilter.getItems().addAll("All Device Types", "ROUTER", "SWITCH", "SERVER", "ACCESS_POINT", "WORKSTATION", "BARCODE_SCANNER", "RFID_READER", "CCTV_CAMERA", "IOT_SENSOR");
        typeFilter.setValue("All Device Types");
        typeFilter.setOnAction(e -> applyFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add New Device");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setOnAction(e -> showAddDeviceDialog());

        Button btnToggle = new Button("Toggle Online/Offline");
        btnToggle.getStyleClass().add("btn-secondary");
        btnToggle.setOnAction(e -> toggleSelectedDeviceStatus());

        Button btnDelete = new Button("Delete Device");
        btnDelete.getStyleClass().add("btn-danger");
        btnDelete.setOnAction(e -> deleteSelectedDevice());

        bar.getChildren().addAll(new Label("Filter:"), searchField, deptFilter, typeFilter, spacer, btnAdd, btnToggle, btnDelete);
        return bar;
    }

    private VBox createTableArea() {
        VBox box = new VBox(8);
        box.getStyleClass().add("metric-card");

        Label title = new Label("WAREHOUSE NETWORK DEVICE REGISTRY (CRUD & VLAN ALLOCATION)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #F8FAFC;");

        deviceTable.setItems(filteredDevices);
        box.getChildren().addAll(title, deviceTable);
        return box;
    }

    private void setupTable() {
        deviceTable.setPrefHeight(520);
        deviceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<NetworkDevice, String> idCol = new TableColumn<>("Device ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMaxWidth(110);

        TableColumn<NetworkDevice, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<NetworkDevice, DeviceType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("deviceType"));

        TableColumn<NetworkDevice, DepartmentType> deptCol = new TableColumn<>("Department");
        deptCol.setCellValueFactory(new PropertyValueFactory<>("department"));

        TableColumn<NetworkDevice, String> ipCol = new TableColumn<>("IPv4 Address");
        ipCol.setCellValueFactory(new PropertyValueFactory<>("ipv4"));

        TableColumn<NetworkDevice, String> macCol = new TableColumn<>("MAC Address");
        macCol.setCellValueFactory(new PropertyValueFactory<>("macAddress"));

        TableColumn<NetworkDevice, Integer> vlanCol = new TableColumn<>("VLAN");
        vlanCol.setCellValueFactory(new PropertyValueFactory<>("vlanId"));
        vlanCol.setMaxWidth(70);

        TableColumn<NetworkDevice, String> connCol = new TableColumn<>("Connection");
        connCol.setCellValueFactory(new PropertyValueFactory<>("connectionType"));

        TableColumn<NetworkDevice, Boolean> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("online"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean online, boolean empty) {
                super.updateItem(online, empty);
                if (empty || online == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(online ? "ONLINE" : "OFFLINE");
                    setStyle(online ? "-fx-text-fill: #10B981; -fx-font-weight: bold;" : "-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<NetworkDevice, String> commCol = new TableColumn<>("Last Comm");
        commCol.setCellValueFactory(new PropertyValueFactory<>("lastCommunicationTime"));

        deviceTable.getColumns().addAll(idCol, nameCol, typeCol, deptCol, ipCol, macCol, vlanCol, connCol, statusCol, commCol);
    }

    private void applyFilters() {
        String query = searchField.getText().toLowerCase().trim();
        String dept = deptFilter.getValue();
        String type = typeFilter.getValue();

        filteredDevices.setPredicate(dev -> {
            boolean matchesSearch = query.isEmpty() ||
                    dev.getId().toLowerCase().contains(query) ||
                    dev.getName().toLowerCase().contains(query) ||
                    dev.getIpv4().toLowerCase().contains(query) ||
                    dev.getMacAddress().toLowerCase().contains(query);

            boolean matchesDept = dept.equals("All Departments") || dev.getDepartment().name().equals(dept);
            boolean matchesType = type.equals("All Device Types") || dev.getDeviceType().name().equals(type);

            return matchesSearch && matchesDept && matchesType;
        });
    }

    private void toggleSelectedDeviceStatus() {
        NetworkDevice selected = deviceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a device to toggle online/offline state.");
            return;
        }
        boolean newStatus = !selected.isOnline();
        selected.setOnline(newStatus);
        deviceRepo.updateStatus(selected.getId(), newStatus);
        topology.setDeviceOnline(selected.getId(), newStatus);
        deviceTable.refresh();
    }

    private void deleteSelectedDevice() {
        NetworkDevice selected = deviceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a device to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete device " + selected.getId() + "?");
        confirm.setContentText("This device will be removed from database and topology.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            deviceRepo.deleteDevice(selected.getId());
            refreshDevicesList();
        }
    }

    private void showAddDeviceDialog() {
        Dialog<NetworkDevice> dialog = new Dialog<>();
        dialog.setTitle("Register New Warehouse Device");
        dialog.setHeaderText("Configure Network Parameters");

        ButtonType btnSave = new ButtonType("Add Device", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(20));

        TextField idField = new TextField("DEV-NEW-01");
        TextField nameField = new TextField("Auxiliary Mobile Scanner");
        ComboBox<DeviceType> typeCmb = new ComboBox<>(FXCollections.observableArrayList(DeviceType.values()));
        typeCmb.setValue(DeviceType.BARCODE_SCANNER);
        ComboBox<DepartmentType> deptCmb = new ComboBox<>(FXCollections.observableArrayList(DepartmentType.values()));
        deptCmb.setValue(DepartmentType.RECEIVING);
        TextField ipField = new TextField("192.168.10.75");
        TextField macField = new TextField("00:1A:2B:10:00:75");
        TextField vlanField = new TextField("10");

        g.add(new Label("Device ID:"), 0, 0); g.add(idField, 1, 0);
        g.add(new Label("Name:"), 0, 1); g.add(nameField, 1, 1);
        g.add(new Label("Type:"), 0, 2); g.add(typeCmb, 1, 2);
        g.add(new Label("Department:"), 0, 3); g.add(deptCmb, 1, 3);
        g.add(new Label("IPv4 Address:"), 0, 4); g.add(ipField, 1, 4);
        g.add(new Label("MAC Address:"), 0, 5); g.add(macField, 1, 5);
        g.add(new Label("VLAN ID:"), 0, 6); g.add(vlanField, 1, 6);

        dialog.getDialogPane().setContent(g);

        dialog.setResultConverter(btn -> {
            if (btn == btnSave) {
                return new NetworkDevice(
                        idField.getText().trim(),
                        nameField.getText().trim(),
                        typeCmb.getValue(),
                        deptCmb.getValue(),
                        ipField.getText().trim(),
                        "2001:db8:1::99",
                        macField.getText().trim(),
                        "255.255.255.0",
                        "192.168.10.1",
                        Integer.parseInt(vlanField.getText().trim()),
                        "Wi-Fi 6",
                        "SW-RCV-01",
                        200, 300, 0
                );
            }
            return null;
        });

        Optional<NetworkDevice> result = dialog.showAndWait();
        result.ifPresent(dev -> {
            deviceRepo.saveOrUpdateDevice(dev);
            topology.reloadFromRepository();
            refreshDevicesList();
        });
    }

    private void refreshDevicesList() {
        masterDeviceList.setAll(deviceRepo.getAllDevices());
        applyFilters();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
