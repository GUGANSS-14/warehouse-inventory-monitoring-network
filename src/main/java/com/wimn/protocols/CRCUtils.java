package com.wimn.protocols;

public class CRCUtils {
    // Standard CRC-16-CCITT polynomial: x^16 + x^12 + x^5 + 1 (0x1021)
    private static final int POLYNOMIAL_16 = 0x1021;
    // Standard CRC-32 polynomial: 0xEDB88320 (IEEE 802.3)
    private static final int POLYNOMIAL_32 = 0xEDB88320;

    public static int calculateCRC16(String data) {
        int crc = 0xFFFF; // Initial value
        byte[] bytes = data.getBytes();
        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= POLYNOMIAL_16;
            }
        }
        return crc & 0xFFFF;
    }

    public static long calculateCRC32(String data) {
        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
        crc32.update(data.getBytes());
        return crc32.getValue();
    }

    public static int calculateChecksum16(String data) {
        byte[] bytes = data.getBytes();
        int sum = 0;
        for (int i = 0; i < bytes.length; i += 2) {
            int word = (bytes[i] & 0xFF) << 8;
            if (i + 1 < bytes.length) {
                word |= (bytes[i + 1] & 0xFF);
            }
            sum += word;
            while ((sum >> 16) > 0) {
                sum = (sum & 0xFFFF) + (sum >> 16);
            }
        }
        return (~sum) & 0xFFFF;
    }

    public static String flipBit(String input, int bitIndex) {
        if (input == null || input.isEmpty()) return input;
        char[] chars = input.toCharArray();
        int charIdx = (bitIndex / 8) % chars.length;
        int bitInChar = bitIndex % 8;
        chars[charIdx] = (char) (chars[charIdx] ^ (1 << bitInChar));
        return new String(chars);
    }
}
