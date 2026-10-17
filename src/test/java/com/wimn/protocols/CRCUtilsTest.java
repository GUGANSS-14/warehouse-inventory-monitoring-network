package com.wimn.protocols;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CRCUtilsTest {

    @Test
    @DisplayName("Test CRC-16 and CRC-32 Deterministic Computation")
    void testCrcComputation() {
        String data = "WAREHOUSE-FRAME-TEST-01";
        int crc16 = CRCUtils.calculateCRC16(data);
        long crc32 = CRCUtils.calculateCRC32(data);

        assertTrue(crc16 != 0);
        assertTrue(crc32 > 0);

        // Same data produces identical CRC
        assertEquals(crc16, CRCUtils.calculateCRC16(data));
        assertEquals(crc32, CRCUtils.calculateCRC32(data));
    }

    @Test
    @DisplayName("Test Bit Inversion causes CRC mismatch (Error Detection)")
    void testBitInversionDetectsCorruption() {
        String data = "WAREHOUSE-FRAME-TEST-01";
        long originalCrc = CRCUtils.calculateCRC32(data);

        String corrupted = CRCUtils.flipBit(data, 5);
        long corruptedCrc = CRCUtils.calculateCRC32(corrupted);

        assertNotEquals(originalCrc, corruptedCrc, "Corrupted bit must result in differing CRC-32 checksum");
    }
}
