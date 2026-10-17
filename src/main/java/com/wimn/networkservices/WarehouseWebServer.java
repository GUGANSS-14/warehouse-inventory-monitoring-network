package com.wimn.networkservices;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.wimn.inventory.InventoryService;
import com.wimn.models.*;
import com.wimn.repositories.DeviceRepository;
import com.wimn.repositories.EventRepository;
import com.wimn.repositories.InventoryRepository;
import com.wimn.simulation.SimulationEngine;
import com.wimn.topology.WarehouseTopology;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WarehouseWebServer {
    private static final Logger LOGGER = Logger.getLogger(WarehouseWebServer.class.getName());
    private static final int DEFAULT_PORT = 8080;

    private HttpServer server;
    private final int port;
    private final InventoryService inventoryService;
    private final WarehouseTopology topology;
    private final SimulationEngine simEngine;
    private final EventRepository eventRepo;

    public WarehouseWebServer(InventoryService inventoryService, WarehouseTopology topology,
                              SimulationEngine simEngine, EventRepository eventRepo) {
        this(DEFAULT_PORT, inventoryService, topology, simEngine, eventRepo);
    }

    public WarehouseWebServer(int port, InventoryService inventoryService, WarehouseTopology topology,
                              SimulationEngine simEngine, EventRepository eventRepo) {
        this.port = port;
        this.inventoryService = inventoryService;
        this.topology = topology;
        this.simEngine = simEngine;
        this.eventRepo = eventRepo;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Web Dashboard Root
        server.createContext("/", new DashboardHtmlHandler());

        // REST API Endpoints
        server.createContext("/api/inventory", new InventoryApiHandler());
        server.createContext("/api/devices", new DevicesApiHandler());
        server.createContext("/api/topology", new TopologyApiHandler());
        server.createContext("/api/metrics", new MetricsApiHandler());
        server.createContext("/api/events", new EventsApiHandler());

        // Manual Interactive Actions API Endpoints
        server.createContext("/api/action/send-packet", new SendPacketApiHandler());
        server.createContext("/api/action/add-stock", new AddStockApiHandler());
        server.createContext("/api/action/dispatch-stock", new DispatchStockApiHandler());
        server.createContext("/api/action/toggle-device", new ToggleDeviceApiHandler());
        server.createContext("/api/action/toggle-link", new ToggleLinkApiHandler());
        server.createContext("/api/action/sim-control", new SimControlApiHandler());
        server.createContext("/api/ai-chat", new AiChatApiHandler());

        server.start();
        LOGGER.info("WIMN Embedded Web Server started at: http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            LOGGER.info("WIMN Web Server stopped.");
        }
    }

    public int getPort() { return port; }

    private class DashboardHtmlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("cyber_security_lock.jpg")) {
                byte[] imgBytes = null;
                java.io.File file = new java.io.File("src/main/resources/web/cyber_security_lock.jpg");
                if (file.exists()) {
                    imgBytes = java.nio.file.Files.readAllBytes(file.toPath());
                } else {
                    try (var is = getClass().getResourceAsStream("/web/cyber_security_lock.jpg")) {
                        if (is != null) imgBytes = is.readAllBytes();
                    } catch (Exception ignored) {}
                }
                if (imgBytes != null) {
                    exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
                    exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, imgBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(imgBytes); }
                    return;
                }
            }

            if (!path.equals("/") && !path.equals("/index.html")) {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(notFound.getBytes()); }
                return;
            }

            String html = generateDashboardHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class InventoryApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<InventoryItem> items = inventoryService.getAllItems();
            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < items.size(); i++) {
                InventoryItem it = items.get(i);
                json.append(String.format("  {\"sku\":\"%s\",\"name\":\"%s\",\"category\":\"%s\",\"quantity\":%d,\"unitPrice\":%.2f,\"rack\":\"%s\",\"zone\":\"%s\"}",
                        it.getSku(), it.getName(), it.getCategory(), it.getQuantity(), it.getUnitPrice(), it.getRackLocation(), it.getDepartmentZone().name()));
                if (i < items.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("]");

            sendJsonResponse(exchange, json.toString());
        }
    }

    private class DevicesApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            var devs = topology.getDevices();
            StringBuilder json = new StringBuilder("[\n");
            int i = 0;
            for (NetworkDevice d : devs) {
                json.append(String.format("  {\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"dept\":\"%s\",\"ip\":\"%s\",\"mac\":\"%s\",\"vlan\":%d,\"online\":%b}",
                        d.getId(), d.getName(), d.getDeviceType().name(), d.getDepartment().name(), d.getIpv4(), d.getMacAddress(), d.getVlanId(), d.isOnline()));
                if (++i < devs.size()) json.append(",");
                json.append("\n");
            }
            json.append("]");
            sendJsonResponse(exchange, json.toString());
        }
    }

    private class TopologyApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            var links = topology.getLinks();
            StringBuilder json = new StringBuilder("{\n  \"links\": [\n");
            for (int i = 0; i < links.size(); i++) {
                NetworkLink l = links.get(i);
                json.append(String.format("    {\"id\":\"%s\",\"src\":\"%s\",\"dst\":\"%s\",\"bandwidthMbps\":%.1f,\"latencyMs\":%.2f,\"status\":\"%s\",\"medium\":\"%s\"}",
                        l.getLinkId(), l.getSourceDeviceId(), l.getTargetDeviceId(), l.getBandwidthMbps(), l.getLatencyMs(), l.getStatus().name(), l.getMediumType()));
                if (i < links.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]\n}");
            sendJsonResponse(exchange, json.toString());
        }
    }

    private class MetricsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            var m = simEngine.getMetrics();
            String json = String.format("{\"clock\":\"%s\",\"sent\":%d,\"delivered\":%d,\"dropped\":%d,\"blocked\":%d,\"pdr\":%.2f,\"latencyMs\":%.2f,\"throughputMbps\":%.1f,\"bandwidthPercent\":%.1f,\"jitterMs\":%.2f}",
                    simEngine.getFormattedSimulationClock(), m.getPacketsSent(), m.getPacketsDelivered(), m.getPacketsDropped(), m.getPacketsBlocked(),
                    m.getPacketDeliveryRatio(), m.getCurrentLatencyMs(), m.getCurrentThroughputMbps(), m.getBandwidthUtilizationPercent(), m.getJitterMs());
            sendJsonResponse(exchange, json);
        }
    }

    private class EventsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<NetworkEvent> evts = eventRepo.getRecentEvents(15);
            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < evts.size(); i++) {
                NetworkEvent e = evts.get(i);
                json.append(String.format("  {\"id\":\"%s\",\"time\":\"%s\",\"category\":\"%s\",\"severity\":\"%s\",\"dept\":\"%s\",\"msg\":\"%s\"}",
                        e.getEventId(), e.getTimestamp(), e.getCategory(), e.getSeverity().name(), e.getSourceDepartment(), e.getMessage().replace("\"", "'")));
                if (i < evts.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("]");
            sendJsonResponse(exchange, json.toString());
        }
    }

    private void sendJsonResponse(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseParams(HttpExchange exchange) throws IOException {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getQuery();
        if (query != null && !query.isEmpty()) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                               URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                } else if (pair.length == 1) {
                    params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), "");
                }
            }
        }
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (body.startsWith("{") && body.endsWith("}")) {
                try {
                    com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    for (String key : obj.keySet()) {
                        if (!obj.get(key).isJsonNull()) {
                            params.put(key, obj.get(key).getAsString());
                        }
                    }
                } catch (Exception ignored) {}
            } else if (body.contains("=")) {
                for (String param : body.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2) {
                        params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                                   URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                    }
                }
            }
        }
        return params;
    }

    private class SendPacketApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseParams(exchange);
            String src = params.getOrDefault("src", "SCN-RCV-01");
            String dst = params.getOrDefault("dst", "SRV-MAIN-01");
            String protocol = params.getOrDefault("protocol", "TCP");
            String payload = params.getOrDefault("payload", "Manual Operator Packet Probe");
            boolean bitFlip = Boolean.parseBoolean(params.getOrDefault("bitFlip", "false"));

            NetworkDevice srcDev = topology.getDevice(src);
            NetworkDevice dstDev = topology.getDevice(dst);
            if (srcDev == null || dstDev == null) {
                sendJsonResponse(exchange, "{\"success\":false,\"error\":\"Invalid source or destination device ID.\"}");
                return;
            }

            PacketHeader header = new PacketHeader(
                    srcDev.getIpv4(), dstDev.getIpv4(), 1024 + (int)(Math.random() * 50000), 8080,
                    protocol, 64, System.currentTimeMillis() % 100000, 0, srcDev.getVlanId()
            );

            NetworkPacket pkt = new NetworkPacket(
                    "PKT-MANUAL-" + (System.currentTimeMillis() % 100000),
                    src, dst, header, payload, 256
            );
            if (bitFlip) {
                pkt.setSimulatedBitFlipped(true);
            }

            pkt = simEngine.transmitPacket(pkt);

            StringBuilder pathJson = new StringBuilder("[");
            if (pkt.getRoutedPath() != null) {
                for (int i = 0; i < pkt.getRoutedPath().size(); i++) {
                    pathJson.append("\"").append(pkt.getRoutedPath().get(i)).append("\"");
                    if (i < pkt.getRoutedPath().size() - 1) pathJson.append(",");
                }
            }
            pathJson.append("]");

            String response = String.format(
                    "{\"success\":true,\"packetId\":\"%s\",\"src\":\"%s\",\"dst\":\"%s\",\"status\":\"%s\",\"latencyMs\":%.2f,\"dropReason\":%s,\"path\":%s}",
                    pkt.getPacketId(), src, dst, pkt.getStatus().name(), pkt.getCalculatedLatencyMs(),
                    pkt.getDropReason() == null ? "null" : "\"" + pkt.getDropReason().replace("\"", "'") + "\"",
                    pathJson.toString()
            );
            sendJsonResponse(exchange, response);
        }
    }

    private class AddStockApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseParams(exchange);
            String sku = params.getOrDefault("sku", "SKU-MANUAL-" + (System.currentTimeMillis() % 1000));
            String name = params.getOrDefault("name", "Manual Inbound Product");
            String category = params.getOrDefault("category", "Electronics");
            int quantity = 25;
            try { quantity = Integer.parseInt(params.getOrDefault("quantity", "25")); } catch (Exception ignored) {}
            double unitPrice = 59.99;
            try { unitPrice = Double.parseDouble(params.getOrDefault("price", "59.99")); } catch (Exception ignored) {}
            String rack = params.getOrDefault("rack", "Rack-A1");
            String supplier = params.getOrDefault("supplier", "Manual Operator Intake");

            var res = inventoryService.receiveProduct(sku, name, category, quantity, unitPrice, rack, supplier);
            String json = String.format("{\"success\":%b,\"message\":\"%s\",\"txId\":%s,\"packetId\":%s}",
                    res.success(), res.message().replace("\"", "'"),
                    res.transactionId() == null ? "null" : "\"" + res.transactionId() + "\"",
                    res.packetId() == null ? "null" : "\"" + res.packetId() + "\"");
            sendJsonResponse(exchange, json);
        }
    }

    private class DispatchStockApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseParams(exchange);
            String sku = params.get("sku");
            if (sku == null || sku.isEmpty()) {
                sendJsonResponse(exchange, "{\"success\":false,\"message\":\"SKU is required for dispatch.\"}");
                return;
            }
            int quantity = 1;
            try { quantity = Integer.parseInt(params.getOrDefault("quantity", "1")); } catch (Exception ignored) {}
            String dest = params.getOrDefault("dest", "Fulfillment Gate Dock-3");

            var res = inventoryService.dispatchProduct(sku, quantity, dest);
            String json = String.format("{\"success\":%b,\"message\":\"%s\",\"txId\":%s}",
                    res.success(), res.message().replace("\"", "'"),
                    res.transactionId() == null ? "null" : "\"" + res.transactionId() + "\"");
            sendJsonResponse(exchange, json);
        }
    }

    private class ToggleDeviceApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseParams(exchange);
            String id = params.get("id");
            NetworkDevice dev = topology.getDevice(id);
            if (dev == null) {
                sendJsonResponse(exchange, "{\"success\":false,\"error\":\"Device not found: " + id + "\"}");
                return;
            }
            boolean newStatus = params.containsKey("online") ? Boolean.parseBoolean(params.get("online")) : !dev.isOnline();
            dev.setOnline(newStatus);
            eventRepo.addEvent(new NetworkEvent(
                    "EVT-DEV-" + System.currentTimeMillis() % 10000,
                    newStatus ? NetworkEvent.Severity.SUCCESS : NetworkEvent.Severity.ALERT,
                    "DEVICE", dev.getDepartment().getDisplayName(),
                    "Device " + dev.getId() + " (" + dev.getName() + ") manually set to " + (newStatus ? "ONLINE" : "OFFLINE (CUT POWER)") + "."
            ));
            sendJsonResponse(exchange, String.format("{\"success\":true,\"id\":\"%s\",\"online\":%b}", id, dev.isOnline()));
        }
    }

    private class ToggleLinkApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseParams(exchange);
            String id = params.get("id");
            NetworkLink link = topology.getLink(id);
            if (link == null) {
                sendJsonResponse(exchange, "{\"success\":false,\"error\":\"Link not found: " + id + "\"}");
                return;
            }
            String statusStr = params.getOrDefault("status", link.getStatus() == LinkStatus.UP ? "DOWN" : "UP");
            try {
                LinkStatus newStatus = LinkStatus.valueOf(statusStr.toUpperCase());
                link.setStatus(newStatus);
                eventRepo.addEvent(new NetworkEvent(
                        "EVT-LNK-" + System.currentTimeMillis() % 10000,
                        newStatus == LinkStatus.UP ? NetworkEvent.Severity.SUCCESS : NetworkEvent.Severity.ALERT,
                        "LINK", "Core Infrastructure",
                        "Link " + link.getLinkId() + " (" + link.getSourceDeviceId() + " <-> " + link.getTargetDeviceId() + ") status set to " + newStatus.name()
                ));
                sendJsonResponse(exchange, String.format("{\"success\":true,\"id\":\"%s\",\"status\":\"%s\"}", id, link.getStatus().name()));
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"success\":false,\"error\":\"Invalid link status: " + statusStr + "\"}");
            }
        }
    }

    private class SimControlApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseParams(exchange);
            String cmd = params.getOrDefault("cmd", "toggle");
            if ("pause".equalsIgnoreCase(cmd)) {
                simEngine.pause();
            } else if ("start".equalsIgnoreCase(cmd)) {
                simEngine.start();
            } else {
                if (simEngine.isRunning()) simEngine.pause(); else simEngine.start();
            }
            sendJsonResponse(exchange, String.format("{\"success\":true,\"running\":%b,\"clock\":\"%s\"}",
                    simEngine.isRunning(), simEngine.getFormattedSimulationClock()));
        }
    }

    private class AiChatApiHandler implements HttpHandler {
        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseParams(exchange);
            String query = params.getOrDefault("query", params.getOrDefault("q", params.getOrDefault("prompt", ""))).trim();

            if (query.isEmpty()) {
                String answer = "👋 Hello! I am **AURA AI**, your Warehouse Inventory & Computer Networks Assistant. Ask me about live device telemetry, network health & PDR metrics, warehouse inventory stock, or any academic networking concept from Units I through V (OSI, CRC-32, Dijkstra, Subnetting, TCP Congestion, or Firewalls)!";
                sendJsonResponse(exchange, String.format("{\"success\":true,\"query\":\"\",\"answer\":\"%s\",\"category\":\"GREETING\"}", escapeJson(answer)));
                return;
            }

            String qLower = query.toLowerCase();
            String answer;
            String category;
            String suggestedAction = "";

            String effectiveApiKey = params.getOrDefault("apiKey", "");
            if (effectiveApiKey.isEmpty()) {
                effectiveApiKey = System.getenv("GEMINI_API_KEY");
                if (effectiveApiKey == null || effectiveApiKey.isBlank()) {
                    effectiveApiKey = System.getProperty("gemini.api.key", "");
                }
            }

            // 1. Interactive Action triggers (Voice switching, packet simulation, sim controls)
            if (qLower.contains("female voice") || qLower.contains("woman voice")) {
                answer = "👩‍💼 **Voice-Over Accessibility Updated:** Switched active narration persona to **Executive Female Voice Over** (High-Clarity Natural Audio). All subsequent system intimations, project chapter tours, and assistant answers will speak in this voice.";
                category = "ACCESSIBILITY";
                suggestedAction = "SET_FEMALE_VOICE";
            } else if (qLower.contains("male voice") || qLower.contains("man voice")) {
                answer = "👨‍💼 **Voice-Over Accessibility Updated:** Switched active narration persona to **Executive Male Voice Over** (Natural Studio Audio). All subsequent system intimations, project chapter tours, and assistant answers will speak in this voice.";
                category = "ACCESSIBILITY";
                suggestedAction = "SET_MALE_VOICE";
            } else if (qLower.contains("robotic voice") || qLower.contains("robot voice")) {
                answer = "🤖 **Voice-Over Accessibility Updated:** Switched active narration persona to **Cyber Robotic AI Voice** (Monotone Synthesized Vocoder).";
                category = "ACCESSIBILITY";
                suggestedAction = "SET_ROBOTIC_VOICE";
            } else if (qLower.contains("send packet") || qLower.contains("test packet") || qLower.contains("flythrough") || qLower.contains("ping test")) {
                answer = "⚡ **Packet Transmission Initiated:** Dispatched a simulated test probe across the core distribution fabric (`SCN-RCV-01` -> `SW-RCV-01` -> `SW-CORE-01` -> `SRV-MAIN-01`). Check the 3D viewport or 2D canvas to observe particle traversal!";
                category = "SIMULATION";
                suggestedAction = "SIMULATE_PACKET";
            } else if (qLower.contains("pause") && (qLower.contains("sim") || qLower.contains("clock"))) {
                simEngine.pause();
                answer = "⏸️ **Simulation Engine Paused:** Live packet traffic generation is currently suspended at sim clock `" + simEngine.getFormattedSimulationClock() + "`.";
                category = "SIMULATION";
                suggestedAction = "PAUSE_SIMULATION";
            } else if ((qLower.contains("start") || qLower.contains("resume")) && (qLower.contains("sim") || qLower.contains("clock"))) {
                simEngine.start();
                answer = "▶️ **Simulation Engine Running:** Live packet generation resumed. Current clock: `" + simEngine.getFormattedSimulationClock() + "`.";
                category = "SIMULATION";
                suggestedAction = "START_SIMULATION";
            } else {
                // If effectiveApiKey is configured, prioritize live Gemini Generative AI
                if (!effectiveApiKey.isEmpty()) {
                    String geminiResp = queryGeminiApi(effectiveApiKey, query);
                    if (geminiResp != null && !geminiResp.isBlank()) {
                        answer = geminiResp;
                        category = "GEMINI_AI";
                    } else {
                        // Gemini returned error or quota exceeded, fallback to local engine
                        LocalQueryResult localRes = evaluateLocalQuery(qLower);
                        if (localRes != null) {
                            answer = "*(⚠️ Google Gemini call failed or key invalid. Providing local telemetry response:)*\n\n" + localRes.answer();
                            category = localRes.category();
                        } else {
                            answer = "⚠️ **Gemini API Error:** Could not contact Google Gemini with the provided API key. Please verify your key in the chat header **[🔑 API Key]** modal (make sure it is from Google AI Studio).\n\n" +
                                     "You can also query local warehouse telemetry (`metrics`), stock (`inventory`), or syllabus topics (`OSI`, `CRC-32`, `Dijkstra`, `Subnetting`, `TCP Handshake`).";
                            category = "API_ERROR";
                        }
                    }
                } else {
                    // No API key provided: use offline local knowledge matcher
                    LocalQueryResult localRes = evaluateLocalQuery(qLower);
                    if (localRes != null) {
                        answer = localRes.answer();
                        category = localRes.category();
                    } else {
                        answer = "🤖 **AURA Assistant:** I understand you're asking about **\"" + query + "\"**.\n\n" +
                                "🔑 **Connect Google Gemini API Key:** To unlock real-time generative AI answers to ANY custom query, connect your free Google Gemini API key by clicking the **🔑 API Key** button in the chat header (or setting `GEMINI_API_KEY` in your environment)!\n\n" +
                                "You can also query live network telemetry (`metrics`), warehouse stock status (`inventory`), packet transmission probes (`simulate packet`), or any syllabus topic from Units I through V (`OSI`, `CRC-32`, `Dijkstra`, `Subnetting`, `TCP Handshake`, or `Firewalls`).";
                        category = "GENERAL";
                    }
                }
            }

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"success\": true,\n");
            json.append("  \"query\": \"").append(escapeJson(query)).append("\",\n");
            json.append("  \"answer\": \"").append(escapeJson(answer)).append("\",\n");
            json.append("  \"category\": \"").append(escapeJson(category)).append("\"");
            if (!suggestedAction.isEmpty()) {
                json.append(",\n  \"suggestedAction\": \"").append(escapeJson(suggestedAction)).append("\"");
            }
            json.append("\n}");

            sendJsonResponse(exchange, json.toString());
        }

        private record LocalQueryResult(String answer, String category) {}

        private LocalQueryResult evaluateLocalQuery(String qLower) {
            // 1. Greetings, Bot Identity & Capabilities
            if (qLower.equals("hi") || qLower.equals("hello") || qLower.equals("hey") || qLower.startsWith("hi ") || qLower.startsWith("hello ") || qLower.contains("good morning") || qLower.contains("good afternoon") || qLower.contains("good evening") || qLower.contains("who are you") || qLower.contains("what is your name") || qLower.contains("what can you do")) {
                var m = simEngine.getMetrics();
                String text = String.format("""
                    👋 **Hello! I am AURA AI (Automated Utility & Routing Assistant)**, your digital twin operational intelligence partner for the Warehouse Inventory Monitoring Network.

                    • **Live Simulation Status:** Sim Clock `%s` (%s)
                    • **Current Performance:** `%.2f%% PDR` | `%.2f ms Latency` | `%d Active Network Nodes`

                    **You can ask me any question directly:**
                    • 📊 **Live Telemetry:** *"What is the current PDR and packet drop rate?"*
                    • 🌐 **Infrastructure:** *"Show devices in Receiving VLAN 10"* or *"Explain core switch role"*
                    • 📦 **Stock Ledger:** *"Show low stock items"* or *"Check Cisco switch stock"*
                    • 🧭 **Routing:** *"Explain Dijkstra vs Bellman-Ford"* or *"How does OSPF heal broken links?"*
                    • 🛡️ **Protocols:** *"Explain HDLC bit stuffing"*, *"How does CRC-32 work?"*, *"TCP 3-way handshake"*
                    • 🎮 **Sim Operations:** *"Send packet"*, *"Pause sim"*, or *"Switch to male voice"*
                    """,
                    simEngine.getFormattedSimulationClock(),
                    (simEngine.isRunning() ? "🟢 Traffic Online" : "⏸️ Traffic Paused"),
                    m.getPacketDeliveryRatio(),
                    m.getCurrentLatencyMs(),
                    topology.getDevices().size()
                );
                return new LocalQueryResult(text, "GREETING");
            }

            // 2. Project Architecture, Overview & Concept
            if (qLower.contains("what is this project") || qLower.contains("explain project") || qLower.contains("about project") || qLower.contains("wimn") || qLower.contains("architecture") || qLower.contains("digital twin") || qLower.contains("concept") || (qLower.contains("what is") && qLower.contains("project")) || (qLower.contains("how") && qLower.contains("work")) || (qLower.contains("how") && qLower.contains("project")) || qLower.contains("overview") || qLower.contains("summary") || qLower.contains("purpose") || qLower.contains("objective")) {
                String text = """
                    🏭 **Warehouse Inventory Monitoring Network (WIMN) — System Architecture:**

                    WIMN is an industrial cyber-physical digital twin that maps the 5 layers of the Computer Networks curriculum (Anna University BE23CS402) into real-world automated warehouse operations:

                    • **Physical Layer (Unit I):** Star-of-Stars topology connecting 5 departmental edge switches to a high-speed core switch (`SW-CORE-01`) and enterprise gateway (`RTR-CORE-01`) over 10G Single-Mode Fiber backbones and Cat6A UTP copper runs.
                    • **Data Link Layer (Unit II):** Barcode scanners, RFID gates, and mobile terminals encapsulate stock intake/dispatch transactions into HDLC frames with flag `01111110` bit-stuffing and 32-bit CRC Frame Check Sequences (FCS).
                    • **Network Layer (Unit III):** Point-to-point router links use RFC 3021 (/31 subnetting) with zero address waste. Departments are segmented into VLANs 10 through 50 with OSPF Link-State (Dijkstra SPF) routing dynamically recalculating paths around severed links.
                    • **Transport Layer (Unit IV):** Stock ledger transactions use reliable TCP connection-oriented streams with 3-way handshakes and Slow-Start congestion avoidance; RTSP CCTV feeds use low-overhead UDP datagrams.
                    • **Application Layer & Security (Unit V):** Network nodes acquire IP parameters via 4-step DHCP DORA; all intra-departmental flows pass through stateful 5-tuple firewall packet inspection.
                    """;
                return new LocalQueryResult(text, "PROJECT_ARCHITECTURE");
            }

            // 3. User Guide, How to Use & Simulation Commands
            if (qLower.contains("how to use") || qLower.contains("how to run") || qLower.contains("how do i") || qLower.contains("commands") || qLower.contains("guide") || qLower.contains("manual") || qLower.contains("help") || qLower.contains("features") || qLower.contains("controls")) {
                String text = """
                    📖 **WIMN Operator Control Quick Reference:**

                    • **Simulate Packet Traversal:** Type `send packet` or click **⚡ Send Test Probe** to watch particle traversal across the 3D viewport and 2D topology canvas.
                    • **Inject Bit-Flip Error:** Check the bit-flip box in the simulation lab to observe receiver-side CRC-32 FCS polynomial remainder failure drop corrupted frames.
                    • **Test Link Disconnection:** Click any device or cable in the 2D topology to cut power/connectivity and observe dynamic OSPF path recalculation.
                    • **Voice Accessibility Selection:** Type `switch to male voice` (👨‍💼), `set female voice` (👩‍💼), or `set robotic voice` (🤖).
                    • **Simulation Controls:** Type `pause sim` to freeze background traffic, or `resume sim` to resume.
                    • **Inspect Stock:** Query specific items like `check Zebra scanner stock` or `low stock alert`.
                    """;
                return new LocalQueryResult(text, "HELP_GUIDE");
            }

            // 4. Link Failures, Redundancy & Dynamic Failover
            if (qLower.contains("fail") || qLower.contains("break") || qLower.contains("cut") || qLower.contains("sever") || qLower.contains("disconnect") || qLower.contains("redundant") || qLower.contains("drop reason") || qLower.contains("why packet drop")) {
                String text = """
                    ⚠️ **Network Link Failures & Dynamic Convergence Mechanics:**

                    • **Failure Detection:** When a physical link (e.g. `LNK-C1` between `SW-CORE-01` and `SW-RCV-01`) is broken or severed, adjacent switch ports detect carrier loss within milliseconds.
                    • **Dijkstra SPF Recomputation:** The core router (`RTR-CORE-01`) recalculates the Shortest Path Tree. If redundant distribution links exist, traffic reroutes automatically without human intervention.
                    • **Packet Delivery Ratio (PDR) Impact:** If an edge switch becomes completely isolated, queued packets exceed TTL or buffer limits and are dropped. This causes live PDR to drop below the 95% SLA threshold, triggering audible and visual alerts.
                    • **Recovery:** Upon link restoration, OSPF exchanges Link-State Advertisements (LSAs) and immediately restores lowest-cost 10Gbps transmission.
                    """;
                return new LocalQueryResult(text, "LINK_FAILURE");
            }

            // 5. Real-time Metrics & Health
            if (qLower.contains("metric") || qLower.contains("health") || qLower.contains("pdr") || qLower.contains("latency") || qLower.contains("jitter") || qLower.contains("throughput") || qLower.contains("dropped") || qLower.contains("packet delivery")) {
                var m = simEngine.getMetrics();
                double pdr = m.getPacketDeliveryRatio();
                double lat = m.getCurrentLatencyMs();
                double tp = m.getCurrentThroughputMbps();
                double jit = m.getJitterMs();
                long sent = m.getPacketsSent();
                long deliv = m.getPacketsDelivered();
                long drop = m.getPacketsDropped();
                long blk = m.getPacketsBlocked();

                String text = String.format("""
                    📊 **Live Network Performance Telemetry (Clock: %s)**:
                    • **Packet Delivery Ratio (PDR):** `%.2f%%` %s
                    • **Average Latency:** `%.2f ms`
                    • **Throughput:** `%.1f Mbps`
                    • **Jitter:** `%.2f ms`
                    • **Total Packets Sent:** `%d` | **Delivered:** `%d`
                    • **Dropped (Congestion/CRC):** `%d` | **Blocked by Firewall:** `%d`
                    """,
                    simEngine.getFormattedSimulationClock(), pdr, (pdr >= 95 ? "🟢 (Healthy SLA)" : "🟡 (Degraded)"),
                    lat, tp, jit, sent, deliv, drop, blk);
                return new LocalQueryResult(text, "METRICS");
            }

            // 6. Devices & Topology
            if (qLower.contains("device") || qLower.contains("switch") || qLower.contains("router") || qLower.contains("scanner") || qLower.contains("workstation") || qLower.contains("camera") || qLower.contains("rtr-") || qLower.contains("sw-") || qLower.contains("srv-") || qLower.contains("scn-") || qLower.contains("core switch") || qLower.contains("access point") || qLower.contains("ap-")) {
                var devs = topology.getDevices();
                NetworkDevice matched = null;
                for (var d : devs) {
                    if (qLower.contains(d.getId().toLowerCase())) {
                        matched = d;
                        break;
                    }
                }
                if (matched != null) {
                    String text = String.format("""
                        🖥️ **Device Telemetry: %s (%s)**
                        • **Department:** %s (VLAN %d)
                        • **Device Type:** %s
                        • **IPv4 Address:** `%s`
                        • **MAC Address:** `%s`
                        • **Operational Status:** %s
                        • **Connection:** %s
                        """,
                        matched.getId(), matched.getName(), matched.getDepartment().getDisplayName(), matched.getVlanId(),
                        matched.getDeviceType().name(), matched.getIpv4(), matched.getMacAddress(),
                        (matched.isOnline() ? "🟢 ONLINE" : "🔴 OFFLINE / DISCONNECTED"),
                        matched.getConnectionType());
                    return new LocalQueryResult(text, "DEVICES");
                } else {
                    long onlineCount = devs.stream().filter(NetworkDevice::isOnline).count();
                    String text = String.format("""
                        🌐 **Warehouse Device Infrastructure Summary:**
                        • **Total Registered Devices:** `%d` (%d Online, %d Offline)
                        • **Core Routing & Switching:** `RTR-CORE-01` (192.168.1.1), `SW-CORE-01` (192.168.1.2), `SRV-MAIN-01` (192.168.1.50)
                        • **VLAN 10 (Receiving):** `SW-RCV-01`, `WS-RCV-01`, `AP-RCV-01`, `SCN-RCV-01`
                        • **VLAN 20 (Storage):** `SW-STR-01`, `AP-STR-01`, `RFID-STR-01`, `IOT-STR-01`
                        • **VLAN 30 (Dispatch):** `SW-DSP-01`, `WS-DSP-01`, `SCN-DSP-01`
                        • **VLAN 40 (Security):** `SW-SEC-01`, `WS-SEC-01`, `CAM-SEC-01`
                        • **VLAN 50 (Manager):** `SW-MGR-01`, `WS-MGR-01`
                        """,
                        devs.size(), onlineCount, (devs.size() - onlineCount));
                    return new LocalQueryResult(text, "DEVICES");
                }
            }

            // 7. Inventory & Warehouse Stock
            if (qLower.contains("inventory") || qLower.contains("stock") || qLower.contains("sku") || qLower.contains("rack") || qLower.contains("pallet") || qLower.contains("cisco") || qLower.contains("zebra") || qLower.contains("cable") || qLower.contains("product") || qLower.contains("item") || qLower.contains("order") || qLower.contains("price") || qLower.contains("valuation")) {
                var items = inventoryService.getAllItems();
                if (qLower.contains("low") || qLower.contains("warning") || qLower.contains("critical") || qLower.contains("reorder")) {
                    var lowItems = items.stream().filter(it -> it.getQuantity() <= 15).toList();
                    if (lowItems.isEmpty()) {
                        return new LocalQueryResult("✅ **Inventory Status:** All warehouse inventory levels are currently above reorder thresholds (minimum 15 units).", "INVENTORY");
                    } else {
                        StringBuilder sb = new StringBuilder("⚠️ **Low-Stock Inventory Alert (< 15 Units):**\n");
                        for (var it : lowItems) {
                            sb.append(String.format("• **%s** (%s): `%d units` @ $%.2f | Location: `%s` (%s)\n",
                                    it.getSku(), it.getName(), it.getQuantity(), it.getUnitPrice(), it.getRackLocation(), it.getDepartmentZone().name()));
                        }
                        return new LocalQueryResult(sb.toString(), "INVENTORY");
                    }
                } else {
                    var matched = items.stream().filter(it -> qLower.contains(it.getSku().toLowerCase()) || qLower.contains(it.getName().toLowerCase()) || qLower.contains(it.getCategory().toLowerCase())).findFirst();
                    if (matched.isPresent()) {
                        var it = matched.get();
                        String text = String.format("""
                            📦 **Item Ledger: %s - %s**
                            • **Category:** %s
                            • **Stock on Hand:** `%d units`
                            • **Unit Price:** `$%.2f` (Total Valuation: `$%.2f`)
                            • **Storage Rack:** `%s` (Zone: %s)
                            • **Status:** %s
                            """,
                            it.getSku(), it.getName(), it.getCategory(), it.getQuantity(), it.getUnitPrice(),
                            (it.getQuantity() * it.getUnitPrice()), it.getRackLocation(), it.getDepartmentZone().name(),
                            (it.getQuantity() <= 15 ? "⚠️ LOW STOCK (Reorder Triggered)" : "🟢 HEALTHY"));
                        return new LocalQueryResult(text, "INVENTORY");
                    } else {
                        int totalUnits = items.stream().mapToInt(InventoryItem::getQuantity).sum();
                        double totalVal = items.stream().mapToDouble(it -> it.getQuantity() * it.getUnitPrice()).sum();
                        String text = String.format("""
                            📦 **Consolidated Inventory Stock Summary:**
                            • **Total Tracked SKUs:** `%d items`
                            • **Total Units in Warehouse:** `%d units`
                            • **Aggregated Inventory Valuation:** `$%,.2f`
                            • **Key Categories:** Networking Hardware, Computing Servers, Barcode Mobile Terminals, RFID Gate Antennas, Fiber Optics, and Power UPS.
                            • *Tip:* Ask *\"What items are low on stock?\"* or query a specific item like *\"Check Cisco switch stock\"*.
                            """,
                            items.size(), totalUnits, totalVal);
                        return new LocalQueryResult(text, "INVENTORY");
                    }
                }
            }

            // 8. Unit I: Physical Layer & Transmission (BE23CS402)
            if (qLower.contains("unit 1") || qLower.contains("unit i") || qLower.contains("physical layer") || qLower.contains("nrz") || qLower.contains("manchester") || qLower.contains("transmission media") || qLower.contains("encoding") || qLower.contains("fiber optic") || qLower.contains("attenuation")) {
                String text = """
                    🔌 **Physical Layer & Transmission Media (Unit-I | CO1):**
                    • **Line Encoding:** 
                      - **NRZ-L / NRZ-I:** Simple signal levels but susceptible to baseline wander and lack of synchronization during long strings of 0s or 1s.
                      - **Manchester Encoding:** Mid-bit transition provides built-in clock recovery (low-to-high for 1, high-to-low for 0), utilized on legacy Ethernet.
                    • **Transmission Media in WIMN:**
                      - **10GBASE-LR Single-Mode Fiber:** Core backbone interconnecting `RTR-CORE-01` and `SW-CORE-01` (wavelength: 1310nm, zero electromagnetic interference).
                      - **Cat6A Shielded Twisted Pair (STP):** Edge drops to workstations and scanners (bandwidth up to 500 MHz, 10 Gbps over 100 meters).
                    • **Star Topology:** Each department connects directly to central switches, ensuring single cable failures do not collapse the entire warehouse.
                    """;
                return new LocalQueryResult(text, "PHYSICAL_LAYER");
            }

            // 9. Unit II: Data Link Layer & Framing (BE23CS402)
            if (qLower.contains("unit 2") || qLower.contains("unit ii") || qLower.contains("data link") || qLower.contains("crc") || qLower.contains("framing") || qLower.contains("stuffing") || qLower.contains("error control") || qLower.contains("flow control") || qLower.contains("hdlc") || qLower.contains("checksum") || qLower.contains("stop and wait") || qLower.contains("go back n") || qLower.contains("selective repeat")) {
                String text = """
                    🛡️ **Data Link Layer: Framing & Error Detection (Unit-II | CO2):**
                    • **HDLC Framing & Bit-Stuffing:** Frames begin and end with flag pattern `01111110`. The sender automatically inserts a `0` after any sequence of five consecutive `1`s (`11111 -> 111110`) to prevent false frame delimiter triggers.
                    • **Cyclic Redundancy Check (CRC-32):**
                      Employs binary modulo-2 polynomial division ($G(x) = x^{32} + x^{26} + ...$). The transmitter appends the remainder (FCS). If the receiver's modulo-2 division yields a non-zero remainder, a bit-flip is detected and the frame is dropped.
                    • **Sliding Window Flow Control:**
                      - **Stop-and-Wait:** Window size = 1.
                      - **Go-Back-N:** Cumulative ACK; Sender window $\\le 2^m - 1$, Receiver window = 1. Corrupted frame triggers retransmission of entire window.
                      - **Selective Repeat:** Independent ACK; Sender & Receiver windows $\\le 2^{m-1}$. Only lost frames are retransmitted.
                    """;
                return new LocalQueryResult(text, "DATALINK");
            }

            // 10. Unit III: Network Layer, Routing & Subnetting (BE23CS402)
            if (qLower.contains("unit 3") || qLower.contains("unit iii") || qLower.contains("network layer") || qLower.contains("dijkstra") || qLower.contains("bellman") || qLower.contains("routing algorithm") || qLower.contains("ospf") || qLower.contains("rip") || qLower.contains("shortest path") || qLower.contains("link state") || qLower.contains("distance vector") || qLower.contains("count to infinity")) {
                String text = """
                    🧭 **Routing Protocols & Algorithms (Unit-III | CO3):**
                    • **Dijkstra Shortest Path First (Link-State / OSPF):**
                      Computes the global minimum cost tree from a source to all nodes using non-negative link weights. Time complexity: $O(V^2)$ or $O(E \\log V)$ with min-heaps. Every router possesses a synchronized Link-State Database (LSDB).
                    • **Bellman-Ford Algorithm (Distance-Vector / RIP):**
                      Distributed algorithm where nodes only exchange routing vectors with immediate neighbors. Equation: $D_x(y) = \\min_v \\{ c(x,v) + D_v(y) \\}$. Suffers from the **Count-to-Infinity problem**, mitigated by Split Horizon with Poison Reverse and a maximum hop limit of 15.
                    """;
                return new LocalQueryResult(text, "ROUTING");
            }

            if (qLower.contains("subnet") || qLower.contains("cidr") || qLower.contains("vlsm") || qLower.contains("ipv4") || qLower.contains("ipv6") || qLower.contains("ip address") || qLower.contains("rfc 3021")) {
                String text = """
                    🌐 **IP Addressing, Subnetting & CIDR (Unit-III | CO3):**
                    • **Subnet Math:** For prefix `/$N$`, total addresses $= 2^{32 - N}$, usable hosts $= 2^{32 - N} - 2$.
                    • **Warehouse Subnet Schema:**
                      - Core Infrastructure: `192.168.1.0/24` (VLAN 1)
                      - Receiving Dept: `192.168.10.0/24` (VLAN 10)
                      - Storage Dept: `192.168.20.0/24` (VLAN 20)
                      - Dispatch Dept: `192.168.30.0/24` (VLAN 30)
                      - Security Dept: `192.168.40.0/24` (VLAN 40)
                      - Manager Office: `192.168.50.0/24` (VLAN 50)
                    • **RFC 3021 (/31 Subnetting):** Used on point-to-point router links; supports 2 addresses with no separate network or broadcast addresses wasted.
                    • **IPv6 Compression (RFC 5952):** Single longest contiguous block of zero 16-bit fields is replaced with `::`.
                    """;
                return new LocalQueryResult(text, "SUBNETTING");
            }

            // 11. Unit IV: Transport Layer Protocols (BE23CS402)
            if (qLower.contains("unit 4") || qLower.contains("unit iv") || qLower.contains("transport layer") || qLower.contains("tcp") || qLower.contains("udp") || qLower.contains("handshake") || qLower.contains("congestion") || qLower.contains("slow start") || qLower.contains("cwnd") || qLower.contains("fast retransmit")) {
                String text = """
                    🔄 **Transport Layer Protocols (Unit-IV | CO4):**
                    • **TCP 3-Way Handshake:**
                      1. `Client -> Server`: `SYN` (seq = x)
                      2. `Server -> Client`: `SYN-ACK` (seq = y, ack = x + 1)
                      3. `Client -> Server`: `ACK` (seq = x + 1, ack = y + 1)
                    • **TCP Congestion Control:**
                      - **Slow Start:** Exponential growth of Congestion Window (`CWND`) per RTT until `ssthresh` is reached.
                      - **Congestion Avoidance:** Additive increase (+1 MSS per RTT) to probe bandwidth gently.
                      - **Fast Retransmit:** Triggered upon receiving 3 duplicate ACKs without waiting for retransmission timer timeout.
                    • **TCP vs UDP:** TCP is connection-oriented with guaranteed delivery, ordering, and flow control (used for inventory intake/dispatch transactions). UDP is connectionless, low-overhead datagram transport used for RTSP CCTV streams and IoT environmental sensors.
                    """;
                return new LocalQueryResult(text, "TRANSPORT");
            }

            // 12. Unit V: Application Layer & Security (BE23CS402)
            if (qLower.contains("unit 5") || qLower.contains("unit v") || qLower.contains("application layer") || qLower.contains("dns") || qLower.contains("dhcp") || qLower.contains("firewall") || qLower.contains("vlan") || qLower.contains("quic") || qLower.contains("http") || qLower.contains("acl") || qLower.contains("5-tuple")) {
                String text = """
                    🏢 **Application Services & Network Security (Unit-V | CO5):**
                    • **DHCP 4-Step DORA Process:**
                      `Discover` (Client UDP 68 -> UDP 67 Broadcast) -> `Offer` (Server offers IP) -> `Request` (Client requests lease) -> `Acknowledge` (Server commits lease with Gateway & DNS).
                    • **DNS Resolution:** Hierarchical tree query (Local Resolver Cache -> Root DNS `.` -> TLD Nameserver `.org` -> Authoritative Nameserver `wimn.internal`).
                    • **Stateful Firewall & ACLs:** Filters packets via 5-tuple matching: `(Source IP, Destination IP, Protocol, Source Port, Destination Port)`. Security VLAN 40 isolates CCTV streams from unauthorized external access.
                    """;
                return new LocalQueryResult(text, "APPLICATION");
            }

            // 13. Universal Dynamic Intelligent Synthesizer for ALL other manual queries:
            var m = simEngine.getMetrics();
            long onlineNodes = topology.getDevices().stream().filter(NetworkDevice::isOnline).count();
            int totalSkus = inventoryService.getAllItems().size();
            double totalVal = inventoryService.getAllItems().stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum();

            String text = String.format("""
                🤖 **AURA AI Operational Response:**
                I analyzed your manual query: **\"%s\"** against our live digital twin state and Computer Networks curriculum models.

                • **Active Digital Twin State (%s):**
                  - **Network Health:** `%.2f%% PDR` %s | **Avg Latency:** `%.2f ms` | **Throughput:** `%.1f Mbps`
                  - **Online Infrastructure:** `%d / %d Nodes Active` (Star-of-Stars 10G Fiber Backbone)
                  - **Warehouse Inventory:** `%d SKUs tracked` (Total Valuation: `$%,.2f`)

                • **Direct Guidance on Your Question:**
                  - If you are testing warehouse communication, run `send packet` to transmit a live probe and trace particle traversal across edge switches.
                  - If you are evaluating network resilience, toggle any switch or link in the 2D topology to see dynamic Dijkstra SPF rerouting in action.
                  - To review academic theory for exam or viva presentation, ask me about *OSI Layers, HDLC Framing, CRC-32 Math, Dijkstra vs Bellman-Ford, Subnet Math, TCP Handshake, or Firewalls*.
                  - To enable unrestricted generative conversational intelligence for arbitrary general topics, click the chat header **[🔑 API Key]** button to activate Google Gemini!
                """,
                qLower,
                simEngine.getFormattedSimulationClock(),
                m.getPacketDeliveryRatio(), (m.getPacketDeliveryRatio() >= 95 ? "🟢 Nominal" : "🟡 Degraded"),
                m.getCurrentLatencyMs(),
                m.getCurrentThroughputMbps(),
                onlineNodes, topology.getDevices().size(),
                totalSkus, totalVal);
            return new LocalQueryResult(text, "SMART_ASSISTANT");
        }

        private String queryGeminiApi(String apiKey, String userQuery) {
            if (apiKey == null || apiKey.trim().isEmpty()) return null;
            String[] models = {"gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro"};
            for (String model : models) {
                try {
                    String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

                    var m = simEngine.getMetrics();
                    String systemContext = String.format("""
                            You are AURA AI, the intelligent, highly capable digital twin operational assistant for the Warehouse Inventory Monitoring Network (WIMN).
                            Live Operational Telemetry:
                            - Simulation Clock: %s | Running: %b
                            - Packet Delivery Ratio (PDR): %.2f%%
                            - Average Latency: %.2f ms | Throughput: %.1f Mbps | Jitter: %.2f ms
                            - Total Packets Sent: %d, Delivered: %d, Dropped: %d, Blocked by Firewall: %d
                            - Total Inventory Items: %d
                            Academic Syllabi Context:
                            - Computer Networks (BE23CS402) Units I to V: Physical Layer (NRZ, Manchester, Edge/Core star topology), Data Link Layer (HDLC bit-stuffing flag 01111110, CRC-32 FCS, Go-Back-N & Selective Repeat), Network Layer (Dijkstra shortest path link-state, Bellman-Ford distance-vector, IPv4 /0 to /32 CIDR subnetting, RFC 3021 /31 P2P, IPv6 RFC 5952), Transport Layer (TCP 3-way handshake SYN/SYN-ACK/ACK, CWND Slow Start & Fast Retransmit, UDP datagrams), Application Layer (DHCP DORA 4-step, DNS hierarchical resolution, DASH video streaming, Stateful 5-tuple Firewall ACLs).
                            Provide a concise, direct, helpful answer in clean GitHub markdown. If the user asks about live telemetry or metrics, always use the real numbers provided above.
                            """,
                            simEngine.getFormattedSimulationClock(),
                            simEngine.isRunning(),
                            m.getPacketDeliveryRatio(),
                            m.getCurrentLatencyMs(),
                            m.getCurrentThroughputMbps(),
                            m.getJitterMs(),
                            m.getPacketsSent(),
                            m.getPacketsDelivered(),
                            m.getPacketsDropped(),
                            m.getPacketsBlocked(),
                            inventoryService.getAllItems().size()
                    );

                    JsonObject root = new JsonObject();
                    JsonArray contents = new JsonArray();
                    JsonObject contentObj = new JsonObject();
                    JsonArray parts = new JsonArray();
                    JsonObject partObj = new JsonObject();
                    partObj.addProperty("text", systemContext + "\n\nUser Question: " + userQuery);
                    parts.add(partObj);
                    contentObj.add("parts", parts);
                    contents.add(contentObj);
                    root.add("contents", contents);

                    HttpClient client = HttpClient.newBuilder()
                            .proxy(java.net.ProxySelector.getDefault())
                            .connectTimeout(Duration.ofSeconds(8))
                            .build();

                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                            .timeout(Duration.ofSeconds(12))
                            .build();

                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 200) {
                        JsonObject respObj = JsonParser.parseString(resp.body()).getAsJsonObject();
                        if (respObj.has("candidates")) {
                            JsonArray cands = respObj.getAsJsonArray("candidates");
                            if (cands.size() > 0) {
                                JsonObject cand = cands.get(0).getAsJsonObject();
                                if (cand.has("content") && cand.getAsJsonObject("content").has("parts")) {
                                    JsonArray partsArr = cand.getAsJsonObject("content").getAsJsonArray("parts");
                                    if (partsArr.size() > 0 && partsArr.get(0).getAsJsonObject().has("text")) {
                                        return partsArr.get(0).getAsJsonObject().get("text").getAsString();
                                    }
                                }
                            }
                        }
                    } else {
                        LOGGER.warning("Gemini model " + model + " returned status: " + resp.statusCode());
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error calling Gemini model " + model + ": " + e.getMessage());
                }
            }
            return null;
        }
    }

    private String generateDashboardHtml() {
        // Priority 1: Read directly from development file for live updates
        java.io.File devFile = new java.io.File("src/main/resources/web/index.html");
        if (devFile.exists()) {
            try {
                return java.nio.file.Files.readString(devFile.toPath(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not read development web/index.html: " + e.getMessage());
            }
        }

        // Priority 2: Read from classpath resource
        try (java.io.InputStream is = getClass().getResourceAsStream("/web/index.html")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load /web/index.html from classpath: " + e.getMessage());
        }

        // Priority 3: Fallback minimal HTML
        return """
            <!DOCTYPE html>
            <html>
            <head><title>WIMN 3D Monitor</title></head>
            <body style='background:#0B132B;color:#FFF;font-family:sans-serif;padding:40px;text-align:center;'>
                <h1>WIMN 3D Warehouse Monitor</h1>
                <p>Loading dashboard resource...</p>
                <p><a href='/api/inventory' style='color:#00F5D4;'>REST Inventory API</a></p>
            </body>
            </html>
            """;
    }
}
