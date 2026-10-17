package com.wimn.networkservices;

import com.wimn.inventory.InventoryService;
import com.wimn.repositories.DeviceRepository;
import com.wimn.repositories.EventRepository;
import com.wimn.repositories.InventoryRepository;
import com.wimn.routing.RoutingEngine;
import com.wimn.security.FirewallEngine;
import com.wimn.simulation.SimulationEngine;
import com.wimn.topology.WarehouseTopology;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class WarehouseWebServerTest {
    private static WarehouseWebServer server;
    private static final int TEST_PORT = 18080;
    private static HttpClient client;

    @BeforeAll
    static void setUp() throws Exception {
        client = HttpClient.newHttpClient();
        var invRepo = new InventoryRepository();
        var devRepo = new DeviceRepository();
        var evtRepo = new EventRepository();
        var topo = new WarehouseTopology(devRepo);
        var routing = new RoutingEngine(topo);
        var firewall = new FirewallEngine(evtRepo);
        var invService = new InventoryService(invRepo, evtRepo);
        var sim = new SimulationEngine(topo, routing, firewall, evtRepo);

        server = new WarehouseWebServer(TEST_PORT, invService, topo, sim, evtRepo);
        server.start();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    @DisplayName("Verify /api/ai-chat handles male voice trigger")
    void testAiChatMaleVoiceTrigger() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/ai-chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"switch to male voice\"}"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("SET_MALE_VOICE"));
        assertTrue(resp.body().contains("Executive Male Voice"));
    }

    @Test
    @DisplayName("Verify /api/ai-chat returns telemetry metrics for health queries")
    void testAiChatMetricsQuery() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/ai-chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"check network health and pdr\"}"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Packet Delivery Ratio"));
        assertTrue(resp.body().contains("METRICS"));
    }

    @Test
    @DisplayName("Verify /api/ai-chat answers manual project architecture and syllabus queries")
    void testAiChatManualProjectAndSyllabusQueries() throws Exception {
        // Test manual project question
        HttpRequest reqProj = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/ai-chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"what is this project and how does it work?\"}"))
                .build();

        HttpResponse<String> respProj = client.send(reqProj, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respProj.statusCode());
        assertTrue(respProj.body().contains("PROJECT_ARCHITECTURE"));
        assertTrue(respProj.body().toLowerCase().contains("digital twin"));

        // Test manual routing syllabus question
        HttpRequest reqRoute = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/ai-chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"explain dijkstra shortest path algorithm\"}"))
                .build();

        HttpResponse<String> respRoute = client.send(reqRoute, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respRoute.statusCode());
        assertTrue(respRoute.body().contains("ROUTING"));
        assertTrue(respRoute.body().contains("Dijkstra"));
    }

    @Test
    @DisplayName("Verify /api/ai-chat fallback directs user to Gemini API Key button")
    void testAiChatUnmatchedQueryDirectsToApiKey() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/ai-chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"how to optimize warehouse floor conveyor belts\"}"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("API Key"));
        assertTrue(resp.body().contains("SMART_ASSISTANT") || resp.body().contains("GENERAL"));
    }
}
