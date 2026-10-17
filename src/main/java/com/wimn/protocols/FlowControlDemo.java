package com.wimn.protocols;

import java.util.ArrayList;
import java.util.List;

public class FlowControlDemo {
    public enum FlowType {
        STOP_AND_WAIT("Stop-and-Wait ARQ", 1),
        SLIDING_WINDOW_GBN("Sliding Window (Go-Back-N)", 4),
        SLIDING_WINDOW_SR("Sliding Window (Selective Repeat)", 4);

        private final String label;
        private final int defaultWindow;
        FlowType(String label, int defaultWindow) {
            this.label = label;
            this.defaultWindow = defaultWindow;
        }
        public String getLabel() { return label; }
        public int getDefaultWindow() { return defaultWindow; }
    }

    public record TransmissionStep(
            int stepNumber,
            String action,
            String senderState,
            String receiverState,
            String description
    ) {}

    public static List<TransmissionStep> simulateFlow(FlowType type, int totalFrames, int windowSize, int dropFrameIndex) {
        List<TransmissionStep> steps = new ArrayList<>();
        int stepCount = 1;

        if (type == FlowType.STOP_AND_WAIT) {
            for (int i = 0; i < totalFrames; i++) {
                steps.add(new TransmissionStep(
                        stepCount++, "TX_FRAME", "Sending Frame " + i, "Waiting",
                        "Sender transmits Frame " + i + " and starts retransmission timer."
                ));
                if (i == dropFrameIndex) {
                    steps.add(new TransmissionStep(
                            stepCount++, "TIMEOUT", "Timer Expired", "No Frame Received",
                            "Frame " + i + " was lost in transit! Timeout triggered at sender."
                    ));
                    steps.add(new TransmissionStep(
                            stepCount++, "RETRANSMIT", "Retransmitting Frame " + i, "Received Frame " + i,
                            "Sender retransmits Frame " + i + ". Receiver validates and accepts."
                    ));
                }
                steps.add(new TransmissionStep(
                        stepCount++, "ACK", "ACK " + (i + 1) + " Received", "Sent ACK " + (i + 1),
                        "Receiver returns ACK " + (i + 1) + ". Sender advances window."
                ));
            }
        } else {
            // Sliding Window Simulation
            int base = 0;
            int nextSeqNum = 0;

            steps.add(new TransmissionStep(
                    stepCount++, "INIT", "Window [0 .. " + (windowSize - 1) + "]", "Expecting Seq 0",
                    "Sliding Window initialized with Window Size N = " + windowSize
            ));

            while (base < totalFrames) {
                // Send up to window size frames
                while (nextSeqNum < base + windowSize && nextSeqNum < totalFrames) {
                    boolean willDrop = (nextSeqNum == dropFrameIndex);
                    steps.add(new TransmissionStep(
                            stepCount++, "TX_BURST", "Sent Frame " + nextSeqNum,
                            willDrop ? "Frame Dropped" : "Buffered Frame " + nextSeqNum,
                            "Transmitting Frame " + nextSeqNum + (willDrop ? " [SIMULATED LOST]" : " [SUCCESS]")
                    ));
                    nextSeqNum++;
                }

                if (dropFrameIndex >= base && dropFrameIndex < nextSeqNum) {
                    steps.add(new TransmissionStep(
                            stepCount++, "TIMEOUT", "Timeout for Frame " + dropFrameIndex, "Discarding out-of-order",
                            "Timer expired for unacknowledged Frame " + dropFrameIndex + ". Sliding window recovery triggered!"
                    ));
                    if (type == FlowType.SLIDING_WINDOW_GBN) {
                        steps.add(new TransmissionStep(
                                stepCount++, "GBN_REPLAY", "Rewinding nextSeq to " + dropFrameIndex, "Expecting " + dropFrameIndex,
                                "Go-Back-N: Sender resets pointer back to Frame " + dropFrameIndex + " and retransmits entire window."
                        ));
                        nextSeqNum = dropFrameIndex; // Go back
                        dropFrameIndex = -1; // Recovered
                    } else {
                        steps.add(new TransmissionStep(
                                stepCount++, "SR_SELECTIVE", "Retransmitting ONLY Frame " + dropFrameIndex, "Selective Buffer Active",
                                "Selective Repeat: Retransmits ONLY dropped Frame " + dropFrameIndex + " without re-sending other frames."
                        ));
                        dropFrameIndex = -1;
                    }
                } else {
                    int acked = Math.min(base + 1, totalFrames);
                    steps.add(new TransmissionStep(
                            stepCount++, "SLIDE_WINDOW", "Window slides forward [" + acked + " .. " + (acked + windowSize - 1) + "]",
                            "ACK " + acked + " delivered",
                            "Cumulative ACK received. Window slides forward."
                    ));
                    base++;
                }
            }
        }

        return steps;
    }
}
