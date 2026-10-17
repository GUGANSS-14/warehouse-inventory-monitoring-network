package com.wimn.views;

import com.wimn.controllers.NavigationManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainLayoutView extends StackPane {
    private static final Logger LOGGER = Logger.getLogger(MainLayoutView.class.getName());

    private final BorderPane contentPane = new BorderPane();
    private final NavigationManager navManager;
    private final Map<NavigationManager.NavSection, Button> navButtons = new HashMap<>();
    private final Label currentSectionLabel = new Label("Executive Dashboard");

    // Entrance Animation Components
    private StackPane entranceOverlay;
    private VBox doorLeft;
    private VBox doorRight;
    private VBox hudCard;
    private ProgressBar scanProgress;
    private Label scanStatusLabel;
    private Timeline scanTimeline;
    private PauseTransition autoEnterPause;
    private boolean isEntered = false;

    public MainLayoutView() {
        this.navManager = new NavigationManager(this::setViewCenter, this::updateActiveNavButton);

        contentPane.setLeft(createSidebar());
        contentPane.setTop(createTopHeader());

        // Default navigation to Dashboard
        navManager.navigateTo(NavigationManager.NavSection.DASHBOARD);

        // Build Entrance Animation Overlay
        entranceOverlay = createEntranceOverlay();

        getChildren().addAll(contentPane, entranceOverlay);

        // Start scanning sequence on launch
        Platform.runLater(this::startEntranceSequence);
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(260);

        // Sidebar Branding
        VBox brandBox = new VBox(3);
        brandBox.setPadding(new Insets(16, 16, 12, 16));
        Label brandTitle = new Label("WIMN");
        brandTitle.getStyleClass().add("sidebar-title");
        brandTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #38BDF8;");

        Label brandSub = new Label("Warehouse Network Monitor\nReal-Time Operations");
        brandSub.getStyleClass().add("sidebar-subtitle");
        brandBox.getChildren().addAll(brandTitle, brandSub);

        VBox navList = new VBox(2);
        navList.setPadding(new Insets(4, 8, 16, 8));

        // Group 1: Overview
        addNavHeader(navList, "OVERVIEW");
        addNavBtn(navList, NavigationManager.NavSection.DASHBOARD, "Executive Dashboard");
        addNavBtn(navList, NavigationManager.NavSection.WAREHOUSE_OPS, "Warehouse Operations");

        // Group 2: Visualizations
        addNavHeader(navList, "TOPOLOGY & 3D");
        addNavBtn(navList, NavigationManager.NavSection.TOPOLOGY_3D, "Interactive 3D Network");
        addNavBtn(navList, NavigationManager.NavSection.TOPOLOGY_2D, "2D Logical Topology");

        // Group 3: Network Management
        addNavHeader(navList, "INFRASTRUCTURE");
        addNavBtn(navList, NavigationManager.NavSection.DEVICE_MGMT, "Device Management");
        addNavBtn(navList, NavigationManager.NavSection.SUBNETTING_LAB, "IP Subnetting & CIDR");
        addNavBtn(navList, NavigationManager.NavSection.ROUTING_SIM, "Routing Simulator");

        // Group 4: Unit-wise Laboratories
        addNavHeader(navList, "SYLLABUS LABS");
        addNavBtn(navList, NavigationManager.NavSection.DATA_LINK_LAB, "Unit-II: Data Link Lab");
        addNavBtn(navList, NavigationManager.NavSection.TRANSPORT_LAB, "Unit-IV: Transport Lab");
        addNavBtn(navList, NavigationManager.NavSection.APP_SERVICES_LAB, "Unit-V: App Services Lab");
        addNavBtn(navList, NavigationManager.NavSection.SECURITY_FIREWALL, "Security & Firewall");
        addNavBtn(navList, NavigationManager.NavSection.ANALYTICS, "Performance Analytics");

        // Group 5: Academic & Tools
        addNavHeader(navList, "PRACTICALS & REPORTS");
        addNavBtn(navList, NavigationManager.NavSection.PRACTICALS, "Practical Experiments");
        addNavBtn(navList, NavigationManager.NavSection.REPORTS_LOGS, "Reports & Audit Logs");
        addNavBtn(navList, NavigationManager.NavSection.SETTINGS_ABOUT, "Settings & Syllabus Map");

        ScrollPane scrollNav = new ScrollPane(navList);
        scrollNav.setFitToWidth(true);
        scrollNav.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scrollNav, Priority.ALWAYS);

        sidebar.getChildren().addAll(brandBox, scrollNav);
        return sidebar;
    }

    private void addNavHeader(VBox container, String title) {
        Label header = new Label(title);
        header.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-padding: 10 12 4 12;");
        container.getChildren().add(header);
    }

    private void addNavBtn(VBox container, NavigationManager.NavSection section, String label) {
        Button btn = new Button(label);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> navManager.navigateTo(section));
        navButtons.put(section, btn);
        container.getChildren().add(btn);
    }

    private HBox createTopHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("top-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 18, 10, 18));

        currentSectionLabel.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Replay Entrance Animation Button
        Button btnReplay = new Button("🚪 Replay Entrance");
        btnReplay.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        btnReplay.setOnAction(e -> replayEntrance());

        // Open 3D Web Dashboard in Browser
        Button btnOpenWeb = new Button("🌐 Open 3D Web Dashboard");
        btnOpenWeb.setStyle("-fx-background-color: linear-gradient(to right, #0284C7, #2563EB); -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        btnOpenWeb.setOnAction(e -> openBrowserDashboard());

        Label vlanBadge = new Label("6 VLANs Active");
        vlanBadge.setStyle("-fx-background-color: #1E3A8A; -fx-text-fill: #60A5FA; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label statusBadge = new Label("● NETWORK ONLINE");
        statusBadge.getStyleClass().add("header-badge");

        header.getChildren().addAll(currentSectionLabel, spacer, btnReplay, btnOpenWeb, vlanBadge, statusBadge);
        return header;
    }

    private void setViewCenter(Node view) {
        contentPane.setCenter(view);
    }

    private void updateActiveNavButton(NavigationManager.NavSection activeSection) {
        currentSectionLabel.setText(activeSection.getTitle());
        navButtons.forEach((section, btn) -> {
            if (section == activeSection) {
                if (!btn.getStyleClass().contains("nav-button-active")) {
                    btn.getStyleClass().add("nav-button-active");
                }
            } else {
                btn.getStyleClass().remove("nav-button-active");
            }
        });
    }

    /* =========================================================================
       WAREHOUSE ENTERING ANIMATION & 3D BLAST DOORS OVERLAY
       ========================================================================= */
    private StackPane createEntranceOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(5, 8, 20, 0.96);");

        HBox doorsContainer = new HBox();
        doorsContainer.setAlignment(Pos.CENTER);

        // Left Blast Door
        doorLeft = new VBox(20);
        doorLeft.prefWidthProperty().bind(overlay.widthProperty().divide(2));
        doorLeft.setStyle("-fx-background-color: linear-gradient(to right, #0B132B, #1C2541); -fx-border-color: #334155; -fx-border-width: 0 4 0 0;");
        doorLeft.setAlignment(Pos.CENTER_LEFT);
        doorLeft.setPadding(new Insets(40));

        Region topStripeL = createHazardStripe();
        VBox doorInfoL = new VBox(6);
        Label lblWarnL = new Label("⚠ AUTOMATED FACILITY ALPHA-07");
        lblWarnL.setStyle("-fx-font-weight: bold; -fx-text-fill: #FACC15; -fx-font-size: 12px;");
        Label lblSecL = new Label("SECTOR 04-A");
        lblSecL.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: #64748B;");
        doorInfoL.getChildren().addAll(lblWarnL, lblSecL);
        Region botStripeL = createHazardStripe();

        doorLeft.getChildren().addAll(topStripeL, doorInfoL, botStripeL);
        VBox.setVgrow(doorInfoL, Priority.ALWAYS);

        // Right Blast Door
        doorRight = new VBox(20);
        doorRight.prefWidthProperty().bind(overlay.widthProperty().divide(2));
        doorRight.setStyle("-fx-background-color: linear-gradient(to left, #0B132B, #1C2541); -fx-border-color: #334155; -fx-border-width: 0 0 0 4;");
        doorRight.setAlignment(Pos.CENTER_RIGHT);
        doorRight.setPadding(new Insets(40));

        Region topStripeR = createHazardStripe();
        VBox doorInfoR = new VBox(6);
        doorInfoR.setAlignment(Pos.CENTER_RIGHT);
        Label lblWarnR = new Label("🛡 WIMN SECURITY RESTRICTED");
        lblWarnR.setStyle("-fx-font-weight: bold; -fx-text-fill: #38BDF8; -fx-font-size: 12px;");
        Label lblSecR = new Label("VLAN 1..50");
        lblSecR.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: #64748B;");
        doorInfoR.getChildren().addAll(lblWarnR, lblSecR);
        Region botStripeR = createHazardStripe();

        doorRight.getChildren().addAll(topStripeR, doorInfoR, botStripeR);
        VBox.setVgrow(doorInfoR, Priority.ALWAYS);

        doorsContainer.getChildren().addAll(doorLeft, doorRight);

        // Center HUD Card
        hudCard = new VBox(14);
        hudCard.setMaxWidth(520);
        hudCard.setAlignment(Pos.CENTER);
        hudCard.setPadding(new Insets(30));
        hudCard.setStyle(
                "-fx-background-color: rgba(11, 19, 43, 0.95); " +
                "-fx-border-color: rgba(56, 189, 248, 0.4); " +
                "-fx-border-radius: 16; -fx-background-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,245,212,0.3), 30, 0, 0, 0);"
        );

        Node lockBadge;
        try {
            java.io.File imgFile = new java.io.File("src/main/resources/web/cyber_security_lock.jpg");
            javafx.scene.image.Image img = null;
            if (imgFile.exists()) {
                img = new javafx.scene.image.Image(imgFile.toURI().toString(), 140, 140, true, true);
            } else {
                var is = getClass().getResourceAsStream("/web/cyber_security_lock.jpg");
                if (is != null) img = new javafx.scene.image.Image(is, 140, 140, true, true);
            }
            if (img != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(100);
                iv.setFitHeight(100);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(100, 100);
                clip.setArcWidth(20);
                clip.setArcHeight(20);
                iv.setClip(clip);
                StackPane lockBox = new StackPane(iv);
                lockBox.setStyle("-fx-border-color: #00F5D4; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0, 245, 212, 0.6), 16, 0, 0, 0);");
                lockBadge = lockBox;
            } else {
                Label fallback = new Label("🔐");
                fallback.setStyle("-fx-font-size: 34px;");
                lockBadge = fallback;
            }
        } catch (Exception ex) {
            Label fallback = new Label("🔐");
            fallback.setStyle("-fx-font-size: 34px;");
            lockBadge = fallback;
        }

        Label hudTitle = new Label("WAREHOUSE FACILITY ENTRY");
        hudTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #38BDF8;");

        Label hudSub = new Label("Warehouse Inventory Monitoring Network (WIMN)\nAutonomous Facility & Star Network Digital Twin");
        hudSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-text-alignment: center;");

        scanProgress = new ProgressBar(0);
        scanProgress.setMaxWidth(Double.MAX_VALUE);
        scanProgress.setStyle("-fx-accent: #00F5D4;");

        scanStatusLabel = new Label("Establishing encrypted TLS link to Core Gateway...");
        scanStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #00F5D4; -fx-font-family: monospace;");

        Button btnEnter = new Button("ENTER WAREHOUSE FACILITY  ➔");
        btnEnter.setMaxWidth(Double.MAX_VALUE);
        btnEnter.setStyle(
                "-fx-background-color: linear-gradient(to right, #00F5D4, #38BDF8); " +
                "-fx-text-fill: #050814; -fx-font-weight: 900; -fx-font-size: 14px; " +
                "-fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        btnEnter.setOnAction(e -> triggerEntrance());

        Button btnSkip = new Button("Quick Skip ➔");
        btnSkip.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-size: 11px; -fx-cursor: hand;");
        btnSkip.setOnAction(e -> finishEntranceImmediately());

        hudCard.getChildren().addAll(lockBadge, hudTitle, hudSub, scanProgress, scanStatusLabel, btnEnter, btnSkip);

        overlay.getChildren().addAll(doorsContainer, hudCard);
        return overlay;
    }

    private Region createHazardStripe() {
        Region stripe = new Region();
        stripe.setPrefHeight(16);
        stripe.setStyle("-fx-background-color: linear-gradient(from 0px 0px to 20px 20px, repeat, #FACC15 0%, #FACC15 50%, #0F172A 50%, #0F172A 100%); -fx-background-radius: 4;");
        return stripe;
    }

    private void startEntranceSequence() {
        if (scanTimeline != null) scanTimeline.stop();

        scanProgress.setProgress(0);
        scanTimeline = new Timeline(
                new KeyFrame(Duration.millis(300), e -> {
                    scanProgress.setProgress(0.25);
                    scanStatusLabel.setText("[0.3s] Pinging Central Server SRV-MAIN-01 (192.168.1.50)... ACK");
                }),
                new KeyFrame(Duration.millis(800), e -> {
                    scanProgress.setProgress(0.60);
                    scanStatusLabel.setText("[0.8s] Scanning RFID Gate Readers & Barcode Terminals... ONLINE");
                }),
                new KeyFrame(Duration.millis(1400), e -> {
                    scanProgress.setProgress(0.90);
                    scanStatusLabel.setText("[1.4s] Depressurizing security airlock. Biometric tokens valid.");
                }),
                new KeyFrame(Duration.millis(1900), e -> {
                    scanProgress.setProgress(1.0);
                    scanStatusLabel.setText("[1.9s] CLEARANCE GRANTED: Opening Warehouse Blast Doors...");
                }),
                new KeyFrame(Duration.millis(2300), e -> {
                    if (!isEntered) triggerEntrance();
                })
        );
        scanTimeline.play();
    }

    private void triggerEntrance() {
        if (isEntered) return;
        isEntered = true;
        if (scanTimeline != null) scanTimeline.stop();

        // 3D Scale & Fade on contentPane
        contentPane.setScaleX(0.94);
        contentPane.setScaleY(0.94);
        contentPane.setOpacity(0.5);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(1200), contentPane);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(1200), contentPane);
        fadeIn.setToValue(1.0);

        // Slide doors open
        double leftDist = doorLeft.getWidth() > 0 ? doorLeft.getWidth() + 100 : 700;
        double rightDist = doorRight.getWidth() > 0 ? doorRight.getWidth() + 100 : 700;

        TranslateTransition slideLeft = new TranslateTransition(Duration.millis(1200), doorLeft);
        slideLeft.setToX(-leftDist);
        slideLeft.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition slideRight = new TranslateTransition(Duration.millis(1200), doorRight);
        slideRight.setToX(rightDist);
        slideRight.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeHud = new FadeTransition(Duration.millis(500), hudCard);
        fadeHud.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(scaleIn, fadeIn, slideLeft, slideRight, fadeHud);
        pt.setOnFinished(e -> {
            entranceOverlay.setVisible(false);
        });
        pt.play();
    }

    private void finishEntranceImmediately() {
        isEntered = true;
        if (scanTimeline != null) scanTimeline.stop();
        entranceOverlay.setVisible(false);
        contentPane.setScaleX(1.0);
        contentPane.setScaleY(1.0);
        contentPane.setOpacity(1.0);
    }

    public void replayEntrance() {
        isEntered = false;
        doorLeft.setTranslateX(0);
        doorRight.setTranslateX(0);
        hudCard.setOpacity(1.0);
        entranceOverlay.setVisible(true);
        startEntranceSequence();
    }

    private void openBrowserDashboard() {
        new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("http://localhost:8080"));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to launch browser: " + e.getMessage());
            }
        }).start();
    }
}
