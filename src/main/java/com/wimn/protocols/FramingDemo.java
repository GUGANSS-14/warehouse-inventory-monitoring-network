package com.wimn.protocols;

public class FramingDemo {
    public static final String HDLC_FLAG = "01111110";

    /**
     * Bit stuffing: insert '0' after five consecutive '1's
     */
    public static String bitStuff(String bitStream) {
        StringBuilder sb = new StringBuilder();
        int consecutiveOnes = 0;
        for (char c : bitStream.toCharArray()) {
            if (c != '0' && c != '1') continue;
            sb.append(c);
            if (c == '1') {
                consecutiveOnes++;
                if (consecutiveOnes == 5) {
                    sb.append('0'); // Stuffed bit
                    consecutiveOnes = 0;
                }
            } else {
                consecutiveOnes = 0;
            }
        }
        return sb.toString();
    }

    /**
     * Bit destuffing: remove '0' after five consecutive '1's
     */
    public static String bitDestuff(String stuffedStream) {
        StringBuilder sb = new StringBuilder();
        int consecutiveOnes = 0;
        char[] chars = stuffedStream.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            sb.append(c);
            if (c == '1') {
                consecutiveOnes++;
                if (consecutiveOnes == 5 && i + 1 < chars.length && chars[i + 1] == '0') {
                    i++; // Skip the stuffed 0
                    consecutiveOnes = 0;
                }
            } else {
                consecutiveOnes = 0;
            }
        }
        return sb.toString();
    }

    /**
     * Byte stuffing (Character stuffing): ESC sequences for DLE, STX, ETX
     */
    public static String byteStuff(String message) {
        return message.replace("ESC", "ESC ESC")
                      .replace("FLAG", "ESC FLAG");
    }

    public static String byteDestuff(String stuffedMessage) {
        return stuffedMessage.replace("ESC FLAG", "FLAG")
                             .replace("ESC ESC", "ESC");
    }
}
