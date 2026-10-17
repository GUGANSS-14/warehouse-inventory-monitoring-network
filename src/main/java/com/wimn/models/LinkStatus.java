package com.wimn.models;

public enum LinkStatus {
    UP("Healthy / Connected", "#10B981"),       // Emerald Green
    CONGESTED("Congested / Warning", "#F59E0B"), // Amber
    DOWN("Offline / Link Failure", "#EF4444");  // Red

    private final String label;
    private final String colorHex;

    LinkStatus(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() { return label; }
    public String getColorHex() { return colorHex; }
}
