package com.wimn.protocols;

import java.util.ArrayList;
import java.util.List;

public class EncodingDemo {
    public enum EncodingScheme {
        NRZ_L("NRZ-L (Non-Return to Zero Level)", "0 = High level, 1 = Low level"),
        NRZ_I("NRZ-I (Non-Return to Zero Inverted)", "1 = Transition at start, 0 = No transition"),
        MANCHESTER("Manchester Encoding (IEEE 802.3)", "0 = High-to-Low, 1 = Low-to-High"),
        DIFF_MANCHESTER("Differential Manchester (Token Ring)", "Always transition in middle; 0 = Transition at start, 1 = None");

        private final String title;
        private final String rule;
        EncodingScheme(String title, String rule) {
            this.title = title;
            this.rule = rule;
        }
        public String getTitle() { return title; }
        public String getRule() { return rule; }
    }

    public record WavePoint(double x, double level) {}

    /**
     * Converts a bit sequence e.g. "1011001" to signal levels (+1.0 or -1.0)
     */
    public static List<Double> generateSignalLevels(String bitSequence, EncodingScheme scheme) {
        List<Double> levels = new ArrayList<>();
        double currentLevel = 1.0;

        for (int i = 0; i < bitSequence.length(); i++) {
            char bit = bitSequence.charAt(i);
            if (bit != '0' && bit != '1') continue;

            switch (scheme) {
                case NRZ_L -> {
                    // 0 = +1.0, 1 = -1.0
                    levels.add(bit == '0' ? 1.0 : -1.0);
                    levels.add(bit == '0' ? 1.0 : -1.0);
                }
                case NRZ_I -> {
                    // 1 causes inversion
                    if (bit == '1') {
                        currentLevel = -currentLevel;
                    }
                    levels.add(currentLevel);
                    levels.add(currentLevel);
                }
                case MANCHESTER -> {
                    // 0: High-to-Low (+1 then -1), 1: Low-to-High (-1 then +1)
                    if (bit == '0') {
                        levels.add(1.0);
                        levels.add(-1.0);
                    } else {
                        levels.add(-1.0);
                        levels.add(1.0);
                    }
                }
                case DIFF_MANCHESTER -> {
                    // 0 = transition at beginning of interval
                    if (bit == '0') {
                        currentLevel = -currentLevel;
                    }
                    levels.add(currentLevel);
                    currentLevel = -currentLevel; // always transition in middle
                    levels.add(currentLevel);
                }
            }
        }
        return levels;
    }
}
