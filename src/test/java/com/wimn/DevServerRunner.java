package com.wimn;

import com.wimn.inventory.InventoryService;
import com.wimn.networkservices.WarehouseWebServer;
import com.wimn.repositories.DeviceRepository;
import com.wimn.repositories.EventRepository;
import com.wimn.repositories.InventoryRepository;
import com.wimn.routing.RoutingEngine;
import com.wimn.security.FirewallEngine;
import com.wimn.simulation.SimulationEngine;
import com.wimn.topology.WarehouseTopology;

public class DevServerRunner {
    public static void main(String[] args) throws Exception {
        var invRepo = new InventoryRepository();
        var devRepo = new DeviceRepository();
        var evtRepo = new EventRepository();
        var topo = new WarehouseTopology(devRepo);
        var routing = new RoutingEngine(topo);
        var firewall = new FirewallEngine(evtRepo);
        var invService = new InventoryService(invRepo, evtRepo);
        var sim = new SimulationEngine(topo, routing, firewall, evtRepo);

        var server = new WarehouseWebServer(8080, invService, topo, sim, evtRepo);
        server.start();
        System.out.println("SERVER_ONLINE_ON_8080");
        while (true) {
            Thread.sleep(60000);
        }
    }
}
