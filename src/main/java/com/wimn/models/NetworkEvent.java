package com.wimn.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NetworkEvent {
    public enum Severity {
        INFO("#3B82F6"),     // Blue
        SUCCESS("#10B981"),  // Emerald
        WARNING("#F59E0B"),  // Amber
        ALERT("#EF4444");    // Red

        private final String color;
        Severity(String color) { this.color = color; }
        public String getColor() { return color; }
    }

    private String eventId;
    private String timestamp;
    private Severity severity;
    private String category; // "SECURITY", "NETWORK", "INVENTORY", "ROUTING", "SIMULATION"
    private String sourceDepartment;
    private String message;

    public NetworkEvent(String eventId, Severity severity, String category,
                        String sourceDepartment, String message) {
        this.eventId = eventId;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.severity = severity;
        this.category = category;
        this.sourceDepartment = sourceDepartment;
        this.message = message;
    }

    public String getEventId() { return eventId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public Severity getSeverity() { return severity; }
    public String getCategory() { return category; }
    public String getSourceDepartment() { return sourceDepartment; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "[" + timestamp + "] [" + category + "] (" + sourceDepartment + ") " + message;
    }
}
