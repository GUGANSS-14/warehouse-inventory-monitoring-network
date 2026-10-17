package com.wimn.controllers;

import com.wimn.inventory.InventoryService;
import com.wimn.networkservices.WarehouseWebServer;
import com.wimn.repositories.DeviceRepository;
import com.wimn.repositories.EventRepository;
import com.wimn.repositories.InventoryRepository;
import com.wimn.routing.RoutingEngine;
import com.wimn.security.FirewallEngine;
import com.wimn.simulation.SimulationEngine;
import com.wimn.topology.WarehouseTopology;
import com.wimn.views.*;
import javafx.scene.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NavigationManager {
    public enum NavSection {
        DASHBOARD("Executive Dashboard"),
        WAREHOUSE_OPS("Warehouse Operations"),
        TOPOLOGY_3D("Interactive 3D Network"),
        TOPOLOGY_2D("Network Topology (2D)"),
        DEVICE_MGMT("Device Management"),
        SUBNETTING_LAB("IP Addressing & Subnetting"),
        ROUTING_SIM("Routing Simulator"),
        DATA_LINK_LAB("Data Link Lab"),
        TRANSPORT_LAB("Transport Layer Lab"),
        APP_SERVICES_LAB("Application Services Lab"),
        SECURITY_FIREWALL("Security and Firewall"),
        ANALYTICS("Performance Analytics"),
        PRACTICALS("Practical Experiments"),
        REPORTS_LOGS("Reports and Logs"),
        SETTINGS_ABOUT("Settings and Syllabus");

        private final String title;
        NavSection(String title) { this.title = title; }
        public String getTitle() { return title; }
    }

    private final Map<NavSection, Node> viewCache = new HashMap<>();
    private final Consumer<Node> viewSetter;
    private final Consumer<NavSection> navStateNotifier;

    // Services
    private final InventoryRepository inventoryRepo;
    private final DeviceRepository deviceRepo;
    private final EventRepository eventRepo;
    private final WarehouseTopology topology;
    private final RoutingEngine routingEngine;
    private final FirewallEngine firewallEngine;
    private final InventoryService inventoryService;
    private final SimulationEngine simEngine;

    public NavigationManager(Consumer<Node> viewSetter, Consumer<NavSection> navStateNotifier) {
        this.viewSetter = viewSetter;
        this.navStateNotifier = navStateNotifier;

        // Initialize Shared Service & Persistence Layer
        this.inventoryRepo = new InventoryRepository();
        this.deviceRepo = new DeviceRepository();
        this.eventRepo = new EventRepository();

        this.topology = new WarehouseTopology(deviceRepo);
        this.routingEngine = new RoutingEngine(topology);
        this.firewallEngine = new FirewallEngine(eventRepo);
        this.inventoryService = new InventoryService(inventoryRepo, eventRepo);
        this.simEngine = new SimulationEngine(topology, routingEngine, firewallEngine, eventRepo);

        // Start Embedded Web Server for browser localhost access (http://localhost:8080)
        try {
            WarehouseWebServer webServer = new WarehouseWebServer(8080, inventoryService, topology, simEngine, eventRepo);
            webServer.start();
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(NavigationManager.class.getName())
                    .log(java.util.logging.Level.WARNING, "Could not start embedded HTTP server on 8080: " + e.getMessage());
        }
    }

    public void navigateTo(NavSection section) {
        Node view = viewCache.computeIfAbsent(section, this::createViewForSection);
        viewSetter.accept(view);
        navStateNotifier.accept(section);
    }

    private Node createViewForSection(NavSection section) {
        return switch (section) {
            case DASHBOARD -> new DashboardView(simEngine, topology, inventoryService, eventRepo);
            case WAREHOUSE_OPS -> new WarehouseOpsView(inventoryService, topology, simEngine, eventRepo);
            case TOPOLOGY_3D -> new Topology3DView(topology, routingEngine, simEngine);
            case TOPOLOGY_2D -> new Topology2DView(topology, routingEngine, simEngine);
            case DEVICE_MGMT -> new DeviceManagementView(deviceRepo, topology);
            case SUBNETTING_LAB -> new SubnettingLabView();
            case ROUTING_SIM -> new RoutingLabView(topology, routingEngine);
            case DATA_LINK_LAB -> new DataLinkLabView();
            case TRANSPORT_LAB -> new TransportLabView();
            case APP_SERVICES_LAB -> new AppServicesLabView();
            case SECURITY_FIREWALL -> new SecurityFirewallView(firewallEngine, topology, eventRepo);
            case ANALYTICS -> new AnalyticsView(simEngine);
            case PRACTICALS -> new PracticalsView();
            case REPORTS_LOGS -> new ReportsLogsView(eventRepo, inventoryService);
            case SETTINGS_ABOUT -> new SettingsAboutView();
        };
    }

    public SimulationEngine getSimEngine() { return simEngine; }
    public WarehouseTopology getTopology() { return topology; }
}
