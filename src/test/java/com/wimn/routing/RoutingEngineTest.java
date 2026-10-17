package com.wimn.routing;

import com.wimn.models.LinkStatus;
import com.wimn.models.NetworkLink;
import com.wimn.repositories.DeviceRepository;
import com.wimn.topology.WarehouseTopology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RoutingEngineTest {
    private WarehouseTopology topology;
    private RoutingEngine routingEngine;

    @BeforeEach
    void setUp() {
        DeviceRepository devRepo = new DeviceRepository();
        topology = new WarehouseTopology(devRepo);
        routingEngine = new RoutingEngine(topology);
    }

    @Test
    @DisplayName("Test Dijkstra Shortest Path from Receiving Workstation to Central Server")
    void testDijkstraShortestPath() {
        RoutingEngine.DijkstraResult result = routingEngine.findShortestPath("WS-RCV-01", "SRV-MAIN-01");

        assertTrue(result.pathFound(), "Shortest path should be discovered");
        assertFalse(result.path().isEmpty());
        assertEquals("WS-RCV-01", result.path().get(0));
        assertEquals("SRV-MAIN-01", result.path().get(result.path().size() - 1));
        assertTrue(result.totalCost() > 0.0);
    }

    @Test
    @DisplayName("Test Dynamic Route Failover when Direct Link is Severed")
    void testDynamicRouteFailoverOnLinkDown() {
        // Find direct link between SW-CORE-01 and SW-RCV-01
        NetworkLink link = topology.findLinkBetween("SW-CORE-01", "SW-RCV-01");
        assertNotNull(link);

        // Inject fault: Link DOWN
        topology.setLinkStatus(link.getLinkId(), LinkStatus.DOWN);

        // Dijkstra should automatically find alternate path via redundant backup trunk (SW-RCV-01 <-> SW-STR-01)
        RoutingEngine.DijkstraResult failoverResult = routingEngine.findShortestPath("WS-RCV-01", "SRV-MAIN-01");

        assertTrue(failoverResult.pathFound(), "Dijkstra must find alternate route through backup link");
        assertTrue(failoverResult.path().contains("SW-STR-01"), "Path must detour through storage switch");
    }

    @Test
    @DisplayName("Test Bellman-Ford Distance Vector Convergence")
    void testBellmanFordConvergence() {
        RoutingEngine.DistanceVectorResult dv = routingEngine.computeDistanceVector();
        assertTrue(dv.converged(), "Bellman-Ford should converge on loop-free warehouse graph");
        assertTrue(dv.totalIterations() > 0);
    }
}
