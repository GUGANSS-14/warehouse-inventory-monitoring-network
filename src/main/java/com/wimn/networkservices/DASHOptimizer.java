package com.wimn.networkservices;

public class DASHOptimizer {
    public record VideoRepresentation(
            String qualityLabel,
            int resolutionWidth,
            int resolutionHeight,
            int targetBitrateKbps,
            double bufferTargetSec,
            String codec
    ) {}

    public static final VideoRepresentation[] STREAM_LEVELS = {
            new VideoRepresentation("1080p Ultra HD", 1920, 1080, 8000, 15.0, "H.265 / HEVC"),
            new VideoRepresentation("720p HD", 1280, 720, 4000, 10.0, "H.264 / AVC"),
            new VideoRepresentation("480p SD", 854, 480, 1500, 8.0, "H.264 / AVC"),
            new VideoRepresentation("360p Low", 640, 360, 750, 6.0, "H.264 / AVC"),
            new VideoRepresentation("240p Minimal", 426, 240, 350, 4.0, "H.264 / AVC")
    };

    /**
     * Determines optimal representation based on simulated available bandwidth (kbps)
     */
    public static VideoRepresentation adaptBitrate(double availableBandwidthKbps) {
        // Safety margin: target bitrate should not exceed 80% of link capacity
        double safeBandwidth = availableBandwidthKbps * 0.80;
        for (VideoRepresentation rep : STREAM_LEVELS) {
            if (safeBandwidth >= rep.targetBitrateKbps()) {
                return rep;
            }
        }
        return STREAM_LEVELS[STREAM_LEVELS.length - 1]; // Lowest fallback
    }
}
