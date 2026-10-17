package com.wimn.networkservices;

import java.util.LinkedHashMap;
import java.util.Map;

public class HTTPSimulator {
    public record HTTPResponse(int statusCode, String statusText, Map<String, String> headers, String body) {}

    public static HTTPResponse executeRequest(String method, String endpoint, String requestBody) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Server", "WIMN-Embedded-HTTP/2.0");
        headers.put("Date", java.time.ZonedDateTime.now().toString());
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Connection", "keep-alive");
        headers.put("X-Warehouse-Zone", "Core-DC-Rack1");

        String upperMethod = method.toUpperCase();
        if (upperMethod.equals("GET")) {
            if (endpoint.contains("/api/inventory")) {
                headers.put("Content-Length", "248");
                String body = """
                    {
                      "status": "success",
                      "timestamp": "%s",
                      "totalSkus": 12,
                      "storageCapacity": "74.5%%",
                      "warehouseHealth": "NOMINAL"
                    }
                    """.formatted(java.time.LocalDateTime.now());
                return new HTTPResponse(200, "OK", headers, body);
            } else if (endpoint.contains("/api/network/topology")) {
                return new HTTPResponse(200, "OK", headers, "{\"topology\":\"hierarchical-star\",\"activeLinks\":17,\"vlanCount\":6}");
            } else {
                return new HTTPResponse(404, "Not Found", headers, "{\"error\": \"Endpoint " + endpoint + " not found on central server.\"}");
            }
        } else if (upperMethod.equals("POST")) {
            headers.put("Content-Length", "180");
            return new HTTPResponse(201, "Created", headers, """
                {
                  "status": "created",
                  "transaction": "TX-HTTP-SUBMIT",
                  "message": "Resource committed to warehouse SQLite database."
                }
                """);
        } else {
            return new HTTPResponse(405, "Method Not Allowed", headers, "{\"error\": \"Method " + method + " not supported.\"}");
        }
    }
}
