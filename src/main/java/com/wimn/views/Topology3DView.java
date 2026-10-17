package com.wimn.views;

import com.wimn.models.*;
import com.wimn.routing.RoutingEngine;
import com.wimn.simulation.SimulationEngine;
import com.wimn.topology.WarehouseTopology;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.*;

public class Topology3DView extends BorderPane {
    private final WarehouseTopology topology;
    private final RoutingEngine routingEngine;
    private final SimulationEngine simEngine;

    // SubScene and 3D Nodes
    private SubScene subScene3D;
    private Group root3D;
    private Group warehouseFloorGroup;
    private Group devicesGroup;
    private Group linksGroup;
    private Group particlesGroup;

    private PerspectiveCamera camera;
    private final Rotate rotateX = new Rotate(-35, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(45, Rotate.Y_AXIS);
    private final Translate cameraTranslate = new Translate(0, 0, -1100);

    private double mouseAnchorX;
    private double mouseAnchorY;
    private double mouseAnchorAngleX;
    private double mouseAnchorAngleY;

    // Side Inspector
    private final Label inspect3dName = new Label("Click any 3D equipment");
    private final Label inspect3dType = new Label("-");
    private final Label inspect3dIp = new Label("-");
    private final Label inspect3dDept = new Label("-");
    private final Label inspect3dStatus = new Label("-");
    private final Label inspect3dCoords = new Label("-");

    // 2D Fallback view instance
    private final Topology2DView fallback2DView;
    private boolean is3DMode = true;
    private final Button toggleModeBtn = new Button("Switch to 2D Logical View");

    public Topology3DView(WarehouseTopology topology, RoutingEngine routingEngine, SimulationEngine simEngine) {
        this.topology = topology;
        this.routingEngine = routingEngine;
        this.simEngine = simEngine;
        this.fallback2DView = new Topology2DView(topology, routingEngine, simEngine);

        setPadding(new Insets(15));
        setTop(createToolbar());

        build3DScene();
        setCenter(subScene3D);
        setRight(createSidebar());

        start3DAnimationLoop();
    }

    private HBox createToolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 12, 0));
        bar.getStyleClass().add("metric-card");

        Label title = new Label("INTERACTIVE 3D WAREHOUSE & INFRASTRUCTURE TOPOLOGY");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #38BDF8; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toggleModeBtn.getStyleClass().add("btn-primary");
        toggleModeBtn.setOnAction(e -> toggle2D3D());

        Button btnResetCamera = new Button("Reset 3D Camera");
        btnResetCamera.getStyleClass().add("btn-secondary");
        btnResetCamera.setOnAction(e -> resetCamera());

        Button btnTestSend = new Button("Simulate 3D Packet");
        btnTestSend.getStyleClass().add("btn-success");
        btnTestSend.setOnAction(e -> simulateTestPacket3D());

        bar.getChildren().addAll(title, spacer, btnTestSend, btnResetCamera, toggleModeBtn);
        return bar;
    }

    private void toggle2D3D() {
        is3DMode = !is3DMode;
        if (is3DMode) {
            setCenter(subScene3D);
            toggleModeBtn.setText("Switch to 2D Logical View");
        } else {
            setCenter(fallback2DView);
            toggleModeBtn.setText("Switch to 3D Warehouse View");
        }
    }

    private VBox createSidebar() {
        VBox box = new VBox(15);
        box.setPrefWidth(300);
        box.setPadding(new Insets(0, 0, 0, 15));

        VBox card = new VBox(10);
        card.getStyleClass().add("metric-card");

        Label cardTitle = new Label("3D DEVICE TELEMETRY");
        cardTitle.getStyleClass().add("card-title");

        inspect3dName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);

        g.add(new Label("Device:"), 0, 0); g.add(inspect3dName, 1, 0);
        g.add(new Label("Category:"), 0, 1); g.add(inspect3dType, 1, 1);
        g.add(new Label("IP Address:"), 0, 2); g.add(inspect3dIp, 1, 2);
        g.add(new Label("Department:"), 0, 3); g.add(inspect3dDept, 1, 3);
        g.add(new Label("Link Status:"), 0, 4); g.add(inspect3dStatus, 1, 4);
        g.add(new Label("3D Coords:"), 0, 5); g.add(inspect3dCoords, 1, 5);

        Label helpText = new Label("CONTROLS:\n• Left Mouse Drag: Orbit 3D Camera\n• Scroll Wheel: Zoom In / Out\n• Click 3D Nodes: Inspect Details");
        helpText.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-padding: 8 0 0 0;");

        card.getChildren().addAll(cardTitle, g, new Separator(), helpText);
        box.getChildren().add(card);
        return box;
    }

    private void build3DScene() {
        root3D = new Group();
        warehouseFloorGroup = new Group();
        devicesGroup = new Group();
        linksGroup = new Group();
        particlesGroup = new Group();

        // 1. Warehouse Floor Grid (Dark Navy Industrial Finish)
        Box floor = new Box(1000, 10, 800);
        floor.setTranslateY(5);
        PhongMaterial floorMat = new PhongMaterial(Color.web("#0F172A"));
        floor.setMaterial(floorMat);
        warehouseFloorGroup.getChildren().add(floor);

        // Department Boundary Zones (Receiving, Storage, Dispatch, Security, Manager, Server Room)
        addDepartmentZoneMarker("ZONE-RCV", "Receiving Dock", -350, 0, -250, 260, 240, Color.web("#38BDF8", 0.4));
        addDepartmentZoneMarker("ZONE-STR", "High-Bay Storage", 0, 0, -100, 380, 480, Color.web("#10B981", 0.4));
        addDepartmentZoneMarker("ZONE-DSP", "Dispatch Staging", 350, 0, -250, 260, 240, Color.web("#F59E0B", 0.4));
        addDepartmentZoneMarker("ZONE-SEC", "Security Guardroom", -350, 0, 250, 240, 200, Color.web("#EF4444", 0.4));
        addDepartmentZoneMarker("ZONE-MGR", "Manager Office", 350, 0, 250, 240, 200, Color.web("#818CF8", 0.4));
        addDepartmentZoneMarker("ZONE-CORE", "Central Server Rack Room", 0, 0, 280, 200, 160, Color.web("#6366F1", 0.6));

        // Build 3D Warehouse Storage Racks
        buildStorageRacks();

        // Build 3D Equipment Nodes from Topology
        buildDeviceMeshes();

        // Build 3D Connecting Links
        buildLinkCylinders();

        // Lighting
        AmbientLight ambient = new AmbientLight(Color.web("#475569"));
        PointLight pointLight = new PointLight(Color.web("#FFFFFF"));
        pointLight.setTranslateY(-400);
        pointLight.setTranslateZ(-200);

        root3D.getChildren().addAll(warehouseFloorGroup, devicesGroup, linksGroup, particlesGroup, ambient, pointLight);

        // Setup 3D Camera
        camera = new PerspectiveCamera(true);
        camera.setNearClip(1.0);
        camera.setFarClip(10000.0);
        camera.getTransforms().addAll(rotateY, rotateX, cameraTranslate);

        subScene3D = new SubScene(root3D, 920, 600, true, SceneAntialiasing.BALANCED);
        subScene3D.setFill(Color.web("#0B132B"));
        subScene3D.setCamera(camera);

        setupCameraMouseControls();
    }

    private void addDepartmentZoneMarker(String id, String label, double x, double y, double z, double w, double d, Color color) {
        Box zoneBox = new Box(w, 2, d);
        zoneBox.setTranslateX(x);
        zoneBox.setTranslateY(y);
        zoneBox.setTranslateZ(z);
        PhongMaterial mat = new PhongMaterial(color);
        zoneBox.setMaterial(mat);
        warehouseFloorGroup.getChildren().add(zoneBox);
    }

    private void buildStorageRacks() {
        // Build 4 multi-tier metal storage rack rows
        for (int row = -1; row <= 1; row += 2) {
            for (int col = -1; col <= 1; col += 2) {
                double rx = col * 90;
                double rz = row * 120 - 100;

                // 4 upright pillars
                for (int dx = -25; dx <= 25; dx += 50) {
                    for (int dz = -50; dz <= 50; dz += 100) {
                        Cylinder pillar = new Cylinder(2.5, 90);
                        pillar.setTranslateX(rx + dx);
                        pillar.setTranslateY(-45);
                        pillar.setTranslateZ(rz + dz);
                        pillar.setMaterial(new PhongMaterial(Color.web("#475569")));
                        warehouseFloorGroup.getChildren().add(pillar);
                    }
                }
                // 3 shelf tiers
                for (int tier = 1; tier <= 3; tier++) {
                    Box shelf = new Box(55, 3, 105);
                    shelf.setTranslateX(rx);
                    shelf.setTranslateY(-tier * 28);
                    shelf.setTranslateZ(rz);
                    shelf.setMaterial(new PhongMaterial(Color.web("#334155")));
                    warehouseFloorGroup.getChildren().add(shelf);

                    // Add simulated inventory cartons
                    Box box1 = new Box(18, 14, 20);
                    box1.setTranslateX(rx - 10);
                    box1.setTranslateY(-tier * 28 - 8);
                    box1.setTranslateZ(rz - 20);
                    box1.setMaterial(new PhongMaterial(Color.web("#D97706")));
                    warehouseFloorGroup.getChildren().add(box1);
                }
            }
        }
    }

    private void buildDeviceMeshes() {
        devicesGroup.getChildren().clear();
        for (NetworkDevice dev : topology.getDevices()) {
            double[] coords3D = mapTo3D(dev);

            Shape3D mesh;
            Color nodeColor;

            switch (dev.getDeviceType()) {
                case ROUTER -> {
                    mesh = new Cylinder(15, 12);
                    nodeColor = Color.web("#3B82F6");
                }
                case SWITCH -> {
                    mesh = new Box(32, 10, 22);
                    nodeColor = Color.web("#06B6D4");
                }
                case SERVER -> {
                    mesh = new Box(25, 55, 35); // Server Cabinet
                    nodeColor = Color.web("#8B5CF6");
                }
                case ACCESS_POINT -> {
                    mesh = new Sphere(10);
                    nodeColor = Color.web("#10B981");
                }
                case CCTV_CAMERA -> {
                    mesh = new Cylinder(6, 16);
                    nodeColor = Color.web("#EF4444");
                }
                case BARCODE_SCANNER, RFID_READER -> {
                    mesh = new Box(12, 12, 12);
                    nodeColor = Color.web("#F59E0B");
                }
                default -> {
                    mesh = new Box(18, 14, 14); // Workstation PC
                    nodeColor = Color.web("#64748B");
                }
            }

            mesh.setTranslateX(coords3D[0]);
            mesh.setTranslateY(coords3D[1]);
            mesh.setTranslateZ(coords3D[2]);

            PhongMaterial mat = new PhongMaterial(nodeColor);
            mesh.setMaterial(mat);

            mesh.setOnMouseClicked(e -> select3DDevice(dev));
            devicesGroup.getChildren().add(mesh);
        }
    }

    private void buildLinkCylinders() {
        linksGroup.getChildren().clear();
        for (NetworkLink link : topology.getLinks()) {
            NetworkDevice src = topology.getDevice(link.getSourceDeviceId());
            NetworkDevice dst = topology.getDevice(link.getTargetDeviceId());
            if (src == null || dst == null) continue;

            double[] p1 = mapTo3D(src);
            double[] p2 = mapTo3D(dst);

            Color linkColor = switch (link.getStatus()) {
                case UP -> Color.web(link.getMediumType().contains("Redundant") ? "#6366F1" : "#10B981");
                case CONGESTED -> Color.web("#F59E0B");
                case DOWN -> Color.web("#EF4444");
            };

            Cylinder cylinder = createCylinderLine(p1, p2, link.getMediumType().contains("10G") ? 2.5 : 1.5, linkColor);
            linksGroup.getChildren().add(cylinder);
        }
    }

    private Cylinder createCylinderLine(double[] p1, double[] p2, double radius, Color color) {
        double dx = p2[0] - p1[0];
        double dy = p2[1] - p1[1];
        double dz = p2[2] - p1[2];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

        Cylinder line = new Cylinder(radius, length);
        line.setMaterial(new PhongMaterial(color));

        double midX = (p1[0] + p2[0]) / 2.0;
        double midY = (p1[1] + p2[1]) / 2.0;
        double midZ = (p1[2] + p2[2]) / 2.0;
        line.setTranslateX(midX);
        line.setTranslateY(midY);
        line.setTranslateZ(midZ);

        // Rotation towards target point
        double theta = Math.atan2(Math.sqrt(dx * dx + dz * dz), dy);
        double phi = Math.atan2(dz, dx);
        line.getTransforms().add(new Rotate(Math.toDegrees(theta) - 90, Rotate.Z_AXIS));
        line.getTransforms().add(new Rotate(Math.toDegrees(phi), Rotate.Y_AXIS));

        return line;
    }

    private double[] mapTo3D(NetworkDevice d) {
        // Map 2D coordinate positions to warehouse 3D layout coordinates
        double x = (d.getPosX() - 450) * 1.8;
        double y = d.getDeviceType() == DeviceType.ACCESS_POINT ? -85 :
                   d.getDeviceType() == DeviceType.CCTV_CAMERA ? -90 :
                   d.getDeviceType() == DeviceType.SERVER ? -28 : -15;
        double z = (d.getPosY() - 300) * 1.5;
        return new double[]{x, y, z};
    }

    private void select3DDevice(NetworkDevice dev) {
        inspect3dName.setText(dev.getName());
        inspect3dType.setText(dev.getDeviceType().getTitle());
        inspect3dIp.setText(dev.getIpv4() + " (VLAN " + dev.getVlanId() + ")");
        inspect3dDept.setText(dev.getDepartment().getDisplayName());
        inspect3dStatus.setText(dev.isOnline() ? "HEALTHY (ONLINE)" : "OFFLINE / DISRUPTED");
        inspect3dStatus.setStyle(dev.isOnline() ? "-fx-text-fill: #10B981; -fx-font-weight: bold;" : "-fx-text-fill: #EF4444; -fx-font-weight: bold;");
        double[] coords = mapTo3D(dev);
        inspect3dCoords.setText(String.format("X: %.0f, Y: %.0f, Z: %.0f", coords[0], coords[1], coords[2]));
    }

    private void setupCameraMouseControls() {
        subScene3D.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            mouseAnchorX = e.getSceneX();
            mouseAnchorY = e.getSceneY();
            mouseAnchorAngleX = rotateX.getAngle();
            mouseAnchorAngleY = rotateY.getAngle();
        });

        subScene3D.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            rotateY.setAngle(mouseAnchorAngleY + (e.getSceneX() - mouseAnchorX) * 0.4);
            double newAngleX = mouseAnchorAngleX - (e.getSceneY() - mouseAnchorY) * 0.4;
            rotateX.setAngle(Math.max(-85, Math.min(5, newAngleX)));
        });

        subScene3D.addEventHandler(ScrollEvent.SCROLL, e -> {
            double delta = e.getDeltaY();
            double newZ = cameraTranslate.getZ() + delta * 1.5;
            if (newZ < -300 && newZ > -2500) {
                cameraTranslate.setZ(newZ);
            }
        });
    }

    private void resetCamera() {
        rotateX.setAngle(-35);
        rotateY.setAngle(45);
        cameraTranslate.setZ(-1100);
    }

    private void simulateTestPacket3D() {
        Sphere packetSphere = new Sphere(7);
        packetSphere.setMaterial(new PhongMaterial(Color.web("#00F5D4")));
        double[] start = mapTo3D(topology.getDevice("WS-RCV-01"));
        packetSphere.setTranslateX(start[0]);
        packetSphere.setTranslateY(start[1]);
        packetSphere.setTranslateZ(start[2]);
        particlesGroup.getChildren().add(packetSphere);

        // Simple animation to server
        new AnimationTimer() {
            int step = 0;
            final double[] end = mapTo3D(topology.getDevice("SRV-MAIN-01"));
            @Override
            public void handle(long now) {
                step++;
                double t = step / 60.0;
                packetSphere.setTranslateX(start[0] + (end[0] - start[0]) * t);
                packetSphere.setTranslateY(start[1] + (end[1] - start[1]) * t);
                packetSphere.setTranslateZ(start[2] + (end[2] - start[2]) * t);
                if (step >= 60) {
                    particlesGroup.getChildren().remove(packetSphere);
                    stop();
                }
            }
        }.start();
    }

    private void start3DAnimationLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Keep 3D scene updated with topology health
            }
        };
        timer.start();
    }
}
