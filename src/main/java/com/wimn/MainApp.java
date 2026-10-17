package com.wimn;

import com.wimn.repositories.DatabaseManager;
import com.wimn.utilities.ThemeManager;
import com.wimn.views.MainLayoutView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            LOGGER.info("Starting Warehouse Inventory Monitoring Network (WIMN)...");

            // Initialize SQLite Database schema & seeds
            DatabaseManager.getInstance().initDatabase();

            // Build Main UI Layout
            MainLayoutView mainLayout = new MainLayoutView();

            // Create Scene
            Scene scene = new Scene(mainLayout, 1280, 800);
            ThemeManager.applyTheme(scene, ThemeManager.Theme.DARK_NAVY);

            primaryStage.setTitle("Warehouse Inventory Monitoring Network (WIMN) | Digital Twin Operations Center");
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(680);
            primaryStage.setScene(scene);

            // Clean shutdown on close
            primaryStage.setOnCloseRequest(e -> {
                LOGGER.info("Shutting down WIMN application...");
                Platform.exit();
                System.exit(0);
            });

            // Start embedded 3D Web Dashboard if port 8080 is available
            startEmbeddedWebServerAsync();

            primaryStage.show();
            LOGGER.info("WIMN application launched successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal error launching WIMN application", e);
        }
    }

    private void startEmbeddedWebServerAsync() {
        Thread serverThread = new Thread(() -> {
            try {
                var invRepo = new com.wimn.repositories.InventoryRepository();
                var devRepo = new com.wimn.repositories.DeviceRepository();
                var evtRepo = new com.wimn.repositories.EventRepository();
                var topo = new com.wimn.topology.WarehouseTopology(devRepo);
                var routing = new com.wimn.routing.RoutingEngine(topo);
                var firewall = new com.wimn.security.FirewallEngine(evtRepo);
                var invService = new com.wimn.inventory.InventoryService(invRepo, evtRepo);
                var sim = new com.wimn.simulation.SimulationEngine(topo, routing, firewall, evtRepo);

                var webServer = new com.wimn.networkservices.WarehouseWebServer(8080, invService, topo, sim, evtRepo);
                webServer.start();
                LOGGER.info("Embedded 3D Web Dashboard running at: http://localhost:8080");
            } catch (Exception e) {
                LOGGER.info("Port 8080 status: " + e.getMessage());
            }
        }, "WIMN-EmbeddedWebServer");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
