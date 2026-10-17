package com.wimn;

import com.wimn.inventory.InventoryService;
import com.wimn.networkservices.WarehouseWebServer;
import com.wimn.repositories.DatabaseManager;
import com.wimn.repositories.DeviceRepository;
import com.wimn.repositories.EventRepository;
import com.wimn.repositories.InventoryRepository;
import com.wimn.routing.RoutingEngine;
import com.wimn.security.FirewallEngine;
import com.wimn.simulation.SimulationEngine;
import com.wimn.topology.WarehouseTopology;

import java.util.logging.Logger;

public class StandaloneWebLauncher {
    private static final Logger LOGGER = Logger.getLogger(StandaloneWebLauncher.class.getName());

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("Starting Warehouse Inventory Monitoring Network (WIMN) Web Server");
        System.out.println("Browser Dashboard: http://localhost:8080");
        System.out.println("===============================================================================");

        DatabaseManager.getInstance().initDatabase();
        InventoryRepository inventoryRepo = new InventoryRepository();
        DeviceRepository deviceRepo = new DeviceRepository();
        EventRepository eventRepo = new EventRepository();

        WarehouseTopology topology = new WarehouseTopology(deviceRepo);
        RoutingEngine routingEngine = new RoutingEngine(topology);
        FirewallEngine firewallEngine = new FirewallEngine(eventRepo);
        InventoryService inventoryService = new InventoryService(inventoryRepo, eventRepo);
        SimulationEngine simEngine = new SimulationEngine(topology, routingEngine, firewallEngine, eventRepo);

        try {
            WarehouseWebServer webServer = new WarehouseWebServer(8080, inventoryService, topology, simEngine, eventRepo);
            webServer.start();

            System.out.println("\n>>> SUCCESS: Dashboard is now LIVE at http://localhost:8080 <<<");
            System.out.println("REST API Endpoints available:");
            System.out.println("  • http://localhost:8080/api/inventory");
            System.out.println("  • http://localhost:8080/api/devices");
            System.out.println("  • http://localhost:8080/api/topology");
            System.out.println("  • http://localhost:8080/api/metrics");
            System.out.println("  • http://localhost:8080/api/events\n");

            // Keep main thread alive
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
