package com.wimn.views;

import com.wimn.models.*;
import com.wimn.repositories.EventRepository;
import com.wimn.security.FirewallEngine;
import com.wimn.topology.WarehouseTopology;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

public class SecurityFirewallView extends BorderPane {
    private final FirewallEngine firewallEngine;
    private final WarehouseTopology topology;
    private final EventRepository eventRepo;

    private final TableView<FirewallRule> rulesTable = new TableView<>();
    private final ComboBox<NetworkLink> linkFaultCombo = new ComboBox<>();
    private final ComboBox<NetworkDevice> deviceFaultCombo = new ComboBox<>();
    private final TextArea faultLogArea = new TextArea();

    public SecurityFirewallView(FirewallEngine firewallEngine, WarehouseTopology topology, EventRepository eventRepo) {
        this.firewallEngine = firewallEngine;
        this.topology = topology;
        this.eventRepo = eventRepo;

        setPadding(new Insets(15));
        setCenter(createContent());

        setupRulesTable();
        refreshRulesTable();
        populateFaultCombos();
    }

    private ScrollPane createContent() {
        VBox root = new VBox(20);
        root.setFillWidth(true);

        Label header = new Label("NETWORK SECURITY, FIREWALL ACLs & FAULT INJECTION LAB");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");

        // 1. Firewall Rule Management Table
        VBox fwBox = new VBox(10);
        fwBox.getStyleClass().add("metric-card");
        Label fwTitle = new Label("1. STATEFUL FIREWALL RULE TABLE (ACCESS CONTROL LIST - ACL)");
        fwTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #F8FAFC;");

        HBox fwActionBar = new HBox(12);
        fwActionBar.setAlignment(Pos.CENTER_LEFT);

        Button btnAddRule = new Button("+ Add Custom Rule");
        btnAddRule.getStyleClass().add("btn-primary");
        btnAddRule.setOnAction(e -> showAddRuleDialog());

        Button btnDeleteRule = new Button("Delete Selected Rule");
        btnDeleteRule.getStyleClass().add("btn-danger");
        btnDeleteRule.setOnAction(e -> deleteSelectedRule());

        Button btnToggleRule = new Button("Toggle Enabled / Disabled");
        btnToggleRule.getStyleClass().add("btn-secondary");
        btnToggleRule.setOnAction(e -> toggleSelectedRule());

        fwActionBar.getChildren().addAll(btnAddRule, btnDeleteRule, btnToggleRule);
        fwBox.getChildren().addAll(fwTitle, fwActionBar, rulesTable);

        // 2. Fault Injection & Reliability Panel
        VBox faultBox = new VBox(12);
        faultBox.getStyleClass().add("metric-card");
        Label faultTitle = new Label("2. FAULT INJECTION & REDUNDANCY RESILIENCE CONTROLLER");
        faultTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #F59E0B;");

        Label faultDesc = new Label("Inject link severing or device failure to test dynamic rerouting and network fault tolerance.");
        faultDesc.setStyle("-fx-text-fill: #94A3B8;");

        GridPane g = new GridPane();
        g.setHgap(15);
        g.setVgap(12);

        // Link Fault Controls
        linkFaultCombo.setPrefWidth(260);
        Button btnBreakLink = new Button("Sever Link (Status: DOWN)");
        btnBreakLink.getStyleClass().add("btn-danger");
        btnBreakLink.setOnAction(e -> setLinkFault(LinkStatus.DOWN));

        Button btnCongestLink = new Button("Make Congested (Status: CONGESTED)");
        btnCongestLink.getStyleClass().add("btn-secondary");
        btnCongestLink.setOnAction(e -> setLinkFault(LinkStatus.CONGESTED));

        Button btnRestoreLink = new Button("Restore Link (Status: UP)");
        btnRestoreLink.getStyleClass().add("btn-success");
        btnRestoreLink.setOnAction(e -> setLinkFault(LinkStatus.UP));

        g.add(new Label("Target Link:"), 0, 0);
        g.add(linkFaultCombo, 1, 0);
        g.add(new HBox(8, btnBreakLink, btnCongestLink, btnRestoreLink), 2, 0);

        // Device Fault Controls
        deviceFaultCombo.setPrefWidth(260);
        Button btnCrashDev = new Button("Take Node Offline");
        btnCrashDev.getStyleClass().add("btn-danger");
        btnCrashDev.setOnAction(e -> setDeviceOnline(false));

        Button btnRestoreDev = new Button("Restore Node Online");
        btnRestoreDev.getStyleClass().add("btn-success");
        btnRestoreDev.setOnAction(e -> setDeviceOnline(true));

        g.add(new Label("Target Node:"), 0, 1);
        g.add(deviceFaultCombo, 1, 1);
        g.add(new HBox(8, btnCrashDev, btnRestoreDev), 2, 1);

        faultLogArea.setEditable(false);
        faultLogArea.setPrefHeight(130);
        faultLogArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
        faultLogArea.setText("[FAULT CONTROLLER] All links nominal. Select target to simulate link severing.");

        faultBox.getChildren().addAll(faultTitle, faultDesc, g, faultLogArea);

        root.getChildren().addAll(header, fwBox, faultBox);
        return new ScrollPane(root);
    }

    private void setupRulesTable() {
        rulesTable.setPrefHeight(200);
        rulesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<FirewallRule, Integer> prioCol = new TableColumn<>("Priority");
        prioCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        prioCol.setMaxWidth(70);

        TableColumn<FirewallRule, String> idCol = new TableColumn<>("Rule ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("ruleId"));
        idCol.setMaxWidth(80);

        TableColumn<FirewallRule, String> nameCol = new TableColumn<>("Rule Description");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<FirewallRule, String> srcCol = new TableColumn<>("Source Subnet");
        srcCol.setCellValueFactory(new PropertyValueFactory<>("sourceSubnet"));

        TableColumn<FirewallRule, String> dstCol = new TableColumn<>("Destination Subnet");
        dstCol.setCellValueFactory(new PropertyValueFactory<>("destinationSubnet"));

        TableColumn<FirewallRule, String> protoCol = new TableColumn<>("Protocol");
        protoCol.setCellValueFactory(new PropertyValueFactory<>("protocol"));
        protoCol.setMaxWidth(80);

        TableColumn<FirewallRule, Integer> portCol = new TableColumn<>("Port");
        portCol.setCellValueFactory(new PropertyValueFactory<>("destinationPort"));
        portCol.setMaxWidth(70);

        TableColumn<FirewallRule, FirewallRule.Action> actCol = new TableColumn<>("Action");
        actCol.setCellValueFactory(new PropertyValueFactory<>("action"));
        actCol.setMaxWidth(90);
        actCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(FirewallRule.Action act, boolean empty) {
                super.updateItem(act, empty);
                if (empty || act == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(act.name());
                    setStyle(act == FirewallRule.Action.ALLOW ?
                            "-fx-background-color: #064E3B; -fx-text-fill: #34D399; -fx-alignment: CENTER; -fx-font-weight: bold;" :
                            "-fx-background-color: #7F1D1D; -fx-text-fill: #F87171; -fx-alignment: CENTER; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<FirewallRule, Long> hitsCol = new TableColumn<>("Hit Count");
        hitsCol.setCellValueFactory(new PropertyValueFactory<>("hitCount"));
        hitsCol.setMaxWidth(80);

        rulesTable.getColumns().addAll(prioCol, idCol, nameCol, srcCol, dstCol, protoCol, portCol, actCol, hitsCol);
    }

    private void refreshRulesTable() {
        rulesTable.setItems(FXCollections.observableArrayList(firewallEngine.getRules()));
    }

    private void populateFaultCombos() {
        linkFaultCombo.setItems(FXCollections.observableArrayList(topology.getLinks()));
        if (!topology.getLinks().isEmpty()) linkFaultCombo.setValue(topology.getLinks().get(2)); // Default distribution trunk

        deviceFaultCombo.setItems(FXCollections.observableArrayList(topology.getDevices()));
        if (!topology.getDevices().isEmpty()) deviceFaultCombo.setValue(topology.getDevices().iterator().next());
    }

    private void setLinkFault(LinkStatus status) {
        NetworkLink link = linkFaultCombo.getValue();
        if (link == null) return;

        topology.setLinkStatus(link.getLinkId(), status);
        eventRepo.addEvent(new NetworkEvent(
                "EVT-FAULT-" + System.currentTimeMillis() % 1000,
                status == LinkStatus.DOWN ? NetworkEvent.Severity.ALERT : status == LinkStatus.CONGESTED ? NetworkEvent.Severity.WARNING : NetworkEvent.Severity.SUCCESS,
                "NETWORK", "Fault Injector",
                "Link [" + link.getLinkId() + "] status changed to " + status.getLabel() + ". Routing engine alerted."
        ));

        faultLogArea.appendText("\n[FAULT INJECTED] Link " + link.getLinkId() + " (" + link.getSourceDeviceId() + " <-> " + link.getTargetDeviceId() + ") set to " + status.name());
        faultLogArea.appendText("\n[RE-CONVERGENCE] Routing table updating paths. Alternate redundant links will be utilized.");
    }

    private void setDeviceOnline(boolean online) {
        NetworkDevice dev = deviceFaultCombo.getValue();
        if (dev == null) return;

        topology.setDeviceOnline(dev.getId(), online);
        eventRepo.addEvent(new NetworkEvent(
                "EVT-DEV-" + dev.getId(),
                online ? NetworkEvent.Severity.SUCCESS : NetworkEvent.Severity.ALERT,
                "NETWORK", "Fault Injector",
                "Node [" + dev.getId() + "] powered " + (online ? "ONLINE" : "OFFLINE (Failure simulated)")
        ));

        faultLogArea.appendText("\n[NODE STATE CHANGE] " + dev.getId() + " is now " + (online ? "ONLINE" : "OFFLINE"));
    }

    private void toggleSelectedRule() {
        FirewallRule r = rulesTable.getSelectionModel().getSelectedItem();
        if (r != null) {
            r.setEnabled(!r.isEnabled());
            rulesTable.refresh();
        }
    }

    private void deleteSelectedRule() {
        FirewallRule r = rulesTable.getSelectionModel().getSelectedItem();
        if (r != null) {
            firewallEngine.removeRule(r.getRuleId());
            refreshRulesTable();
        }
    }

    private void showAddRuleDialog() {
        Dialog<FirewallRule> d = new Dialog<>();
        d.setTitle("Define New Firewall Rule");
        d.setHeaderText("Specify Access Control Criteria");

        ButtonType btnOk = new ButtonType("Add Rule", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(15));

        TextField nameFld = new TextField("Block Cross-Department Telnet");
        TextField srcFld = new TextField("ANY");
        TextField dstFld = new TextField("ANY");
        ComboBox<String> protoCmb = new ComboBox<>(FXCollections.observableArrayList("ANY", "TCP", "UDP", "ICMP"));
        protoCmb.setValue("TCP");
        TextField portFld = new TextField("23");
        ComboBox<FirewallRule.Action> actCmb = new ComboBox<>(FXCollections.observableArrayList(FirewallRule.Action.values()));
        actCmb.setValue(FirewallRule.Action.DENY);
        Spinner<Integer> prioSpn = new Spinner<>(1, 100, 35);

        g.add(new Label("Rule Name:"), 0, 0); g.add(nameFld, 1, 0);
        g.add(new Label("Source Subnet:"), 0, 1); g.add(srcFld, 1, 1);
        g.add(new Label("Dest Subnet:"), 0, 2); g.add(dstFld, 1, 2);
        g.add(new Label("Protocol:"), 0, 3); g.add(protoCmb, 1, 3);
        g.add(new Label("Dest Port (0 for ANY):"), 0, 4); g.add(portFld, 1, 4);
        g.add(new Label("Action:"), 0, 5); g.add(actCmb, 1, 5);
        g.add(new Label("Priority:"), 0, 6); g.add(prioSpn, 1, 6);

        d.getDialogPane().setContent(g);

        d.setResultConverter(btn -> {
            if (btn == btnOk) {
                return new FirewallRule(
                        "FW-CUST-" + System.currentTimeMillis() % 1000,
                        nameFld.getText().trim(),
                        srcFld.getText().trim(),
                        dstFld.getText().trim(),
                        protoCmb.getValue(),
                        Integer.parseInt(portFld.getText().trim()),
                        actCmb.getValue(),
                        prioSpn.getValue(),
                        "Custom user rule"
                );
            }
            return null;
        });

        Optional<FirewallRule> res = d.showAndWait();
        res.ifPresent(r -> {
            firewallEngine.addRule(r);
            refreshRulesTable();
        });
    }
}
