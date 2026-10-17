package com.wimn.views;

import com.wimn.simulation.SimulationEngine;
import com.wimn.simulation.SimulationMetrics;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AnalyticsView extends BorderPane {
    private final SimulationEngine simEngine;

    // Charts
    private final XYChart.Series<String, Number> throughputSeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> jitterSeries = new XYChart.Series<>();

    // Theoretical Calculator
    private final Slider linkSpeedSlider = new Slider(10, 10000, 1000);
    private final Slider packetSizeSlider = new Slider(64, 9000, 1500);
    private final Slider distanceSlider = new Slider(10, 2000, 150);

    private final Label calcTxDelayLabel = new Label("-");
    private final Label calcPropDelayLabel = new Label("-");
    private final Label calcTotalLatencyLabel = new Label("-");
    private final Label calcThroughputLabel = new Label("-");

    public AnalyticsView(SimulationEngine simEngine) {
        this.simEngine = simEngine;

        setPadding(new Insets(15));
        setCenter(createContent());

        simEngine.addTickListener(() -> Platform.runLater(this::updateCharts));
        recalculateDelayFormula();
    }

    private ScrollPane createContent() {
        VBox root = new VBox(20);
        root.setFillWidth(true);

        Label header = new Label("NETWORK PERFORMANCE ANALYTICS & DELAY METRICS LAB");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        // 1. Performance Charts Row
        HBox chartsRow = new HBox(15);

        // Throughput Chart
        CategoryAxis xAxis1 = new CategoryAxis();
        xAxis1.setLabel("Samples");
        NumberAxis yAxis1 = new NumberAxis(0, 600, 100);
        yAxis1.setLabel("Throughput (Mbps)");
        LineChart<String, Number> throughputChart = new LineChart<>(xAxis1, yAxis1);
        throughputChart.setTitle("Simulated Warehouse Network Throughput (Mbps)");
        throughputChart.setLegendVisible(false);
        throughputChart.setAnimated(false);
        throughputSeries.setName("Throughput");
        throughputChart.getData().add(throughputSeries);
        throughputChart.getStyleClass().add("metric-card");
        HBox.setHgrow(throughputChart, Priority.ALWAYS);

        // Jitter Chart
        CategoryAxis xAxis2 = new CategoryAxis();
        xAxis2.setLabel("Samples");
        NumberAxis yAxis2 = new NumberAxis(0, 5, 1);
        yAxis2.setLabel("Jitter (ms)");
        BarChart<String, Number> jitterChart = new BarChart<>(xAxis2, yAxis2);
        jitterChart.setTitle("Network Jitter (Packet Delay Variation)");
        jitterChart.setLegendVisible(false);
        jitterChart.setAnimated(false);
        jitterSeries.setName("Jitter");
        jitterChart.getData().add(jitterSeries);
        jitterChart.getStyleClass().add("metric-card");
        HBox.setHgrow(jitterChart, Priority.ALWAYS);

        chartsRow.getChildren().addAll(throughputChart, jitterChart);

        // 2. Theoretical vs Actual Performance Calculator
        VBox calcBox = new VBox(12);
        calcBox.getStyleClass().add("metric-card");

        Label calcTitle = new Label("MATHEMATICAL DELAY & CAPACITY CALCULATOR (SECTION 7.11)");
        calcTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10B981;");

        GridPane inputs = new GridPane();
        inputs.setHgap(15);
        inputs.setVgap(12);

        inputs.add(new Label("Configured Link Speed (Mbps):"), 0, 0);
        linkSpeedSlider.setShowTickLabels(true);
        linkSpeedSlider.setShowTickMarks(true);
        linkSpeedSlider.setMajorTickUnit(2500);
        linkSpeedSlider.valueProperty().addListener((obs, o, n) -> recalculateDelayFormula());
        inputs.add(linkSpeedSlider, 1, 0);

        inputs.add(new Label("Packet Size (Bytes):"), 2, 0);
        packetSizeSlider.setShowTickLabels(true);
        packetSizeSlider.setShowTickMarks(true);
        packetSizeSlider.setMajorTickUnit(2000);
        packetSizeSlider.valueProperty().addListener((obs, o, n) -> recalculateDelayFormula());
        inputs.add(packetSizeSlider, 3, 0);

        inputs.add(new Label("Physical Link Distance (Meters):"), 0, 1);
        distanceSlider.setShowTickLabels(true);
        distanceSlider.setShowTickMarks(true);
        distanceSlider.setMajorTickUnit(500);
        distanceSlider.valueProperty().addListener((obs, o, n) -> recalculateDelayFormula());
        inputs.add(distanceSlider, 1, 1);

        // Results Grid
        GridPane results = new GridPane();
        results.setHgap(20);
        results.setVgap(10);
        results.setPadding(new Insets(10, 0, 0, 0));

        results.add(createField("Transmission Delay (Ttx = L / R):", calcTxDelayLabel, "#38BDF8"), 0, 0);
        results.add(createField("Propagation Delay (Tprop = d / v):", calcPropDelayLabel, "#34D399"), 1, 0);
        results.add(createField("Total One-Way Latency:", calcTotalLatencyLabel, "#F59E0B"), 2, 0);
        results.add(createField("Effective Throughput:", calcThroughputLabel, "#818CF8"), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            results.getColumnConstraints().add(cc);
        }

        Label explanation = new Label("""
            KEY NETWORKING CONCEPTS:
            • Bandwidth: The maximum theoretical capacity of a link (e.g. 1000 Mbps Cat6A).
            • Throughput: The actual rate of successful data delivery over time, reduced by protocol framing overhead, contention, and propagation delay.
            • Transmission Delay (Ttx): Time required to push all packet bits onto the physical transmission wire (L / R).
            • Propagation Delay (Tprop): Time for a signal wave to travel across the physical medium at speed of light in copper (~2x10^8 m/s).
            """);
        explanation.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");

        calcBox.getChildren().addAll(calcTitle, inputs, new Separator(), results, explanation);

        root.getChildren().addAll(header, chartsRow, calcBox);
        return new ScrollPane(root);
    }

    private VBox createField(String title, Label valLbl, String colorHex) {
        VBox v = new VBox(4);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");
        valLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        v.getChildren().addAll(t, valLbl);
        return v;
    }

    private int tickCount = 0;

    private void updateCharts() {
        SimulationMetrics m = simEngine.getMetrics();
        tickCount++;
        throughputSeries.getData().add(new XYChart.Data<>("T" + tickCount, m.getCurrentThroughputMbps()));
        jitterSeries.getData().add(new XYChart.Data<>("T" + tickCount, m.getJitterMs()));

        if (throughputSeries.getData().size() > 15) {
            throughputSeries.getData().remove(0);
        }
        if (jitterSeries.getData().size() > 15) {
            jitterSeries.getData().remove(0);
        }
    }

    private void recalculateDelayFormula() {
        double linkSpeedMbps = linkSpeedSlider.getValue();
        double packetSizeBytes = packetSizeSlider.getValue();
        double distanceMeters = distanceSlider.getValue();

        // Ttx = (Packet Size in bits) / (Link speed in bps)
        double ttxSeconds = (packetSizeBytes * 8.0) / (linkSpeedMbps * 1_000_000.0);
        double ttxMs = ttxSeconds * 1000.0;

        // Tprop = distance / (2 * 10^8 m/s speed of light in copper)
        double propagationSpeed = 200_000_000.0; // 2x10^8 m/s
        double tpropSeconds = distanceMeters / propagationSpeed;
        double tpropMs = tpropSeconds * 1000.0;

        double totalMs = ttxMs + tpropMs + 0.15; // + processing delay

        calcTxDelayLabel.setText(String.format("%.4f ms", ttxMs));
        calcPropDelayLabel.setText(String.format("%.4f ms", tpropMs));
        calcTotalLatencyLabel.setText(String.format("%.3f ms", totalMs));

        // Effective throughput accounting for header overhead
        double effectiveRatio = (packetSizeBytes - 40.0) / packetSizeBytes;
        calcThroughputLabel.setText(String.format("%.1f Mbps", linkSpeedMbps * effectiveRatio * 0.92));
    }
}
