package com.wimn.simulation;

import com.wimn.models.SubnetInfo;
import com.wimn.protocols.SubnetCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SubnetCalculatorTest {

    @Test
    @DisplayName("Test Standard /24 Class C Subnet Calculation")
    void testStandardSlash24() {
        SubnetInfo info = SubnetCalculator.calculate("192.168.10.45", 24);

        assertEquals("192.168.10.0", info.getNetworkAddress());
        assertEquals("255.255.255.0", info.getSubnetMask());
        assertEquals("192.168.10.255", info.getBroadcastAddress());
        assertEquals("192.168.10.1", info.getFirstUsableHost());
        assertEquals("192.168.10.254", info.getLastUsableHost());
        assertEquals(256, info.getTotalAddresses());
        assertEquals(254, info.getUsableHosts());
    }

    @Test
    @DisplayName("Test /30 Subnet for Point-to-Point Links")
    void testSlash30PointToPoint() {
        SubnetInfo info = SubnetCalculator.calculate("10.0.0.1", 30);

        assertEquals("10.0.0.0", info.getNetworkAddress());
        assertEquals("255.255.255.252", info.getSubnetMask());
        assertEquals("10.0.0.3", info.getBroadcastAddress());
        assertEquals("10.0.0.1", info.getFirstUsableHost());
        assertEquals("10.0.0.2", info.getLastUsableHost());
        assertEquals(4, info.getTotalAddresses());
        assertEquals(2, info.getUsableHosts());
    }

    @Test
    @DisplayName("Test RFC 3021 /31 Edge Case (Point-to-Point 2 Usable Hosts)")
    void testSlash31EdgeCase() {
        SubnetInfo info = SubnetCalculator.calculate("10.0.0.0", 31);

        assertEquals("10.0.0.0", info.getNetworkAddress());
        assertEquals("255.255.255.254", info.getSubnetMask());
        assertEquals(2, info.getTotalAddresses());
        assertEquals(2, info.getUsableHosts(), "RFC 3021 allows both addresses to be usable hosts");
        assertEquals("10.0.0.0", info.getFirstUsableHost());
        assertEquals("10.0.0.1", info.getLastUsableHost());
    }

    @Test
    @DisplayName("Test /32 Single Host Edge Case")
    void testSlash32HostRoute() {
        SubnetInfo info = SubnetCalculator.calculate("127.0.0.1", 32);

        assertEquals("127.0.0.1", info.getNetworkAddress());
        assertEquals("255.255.255.255", info.getSubnetMask());
        assertEquals(1, info.getTotalAddresses());
        assertEquals(1, info.getUsableHosts());
        assertEquals("127.0.0.1", info.getFirstUsableHost());
        assertEquals("127.0.0.1", info.getLastUsableHost());
    }

    @Test
    @DisplayName("Test Invalid IP Input Rejection")
    void testInvalidIpThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> SubnetCalculator.calculate("999.168.1.1", 24));
        assertThrows(IllegalArgumentException.class, () -> SubnetCalculator.calculate("192.168.1", 24));
        assertThrows(IllegalArgumentException.class, () -> SubnetCalculator.calculate("abc.def.ghi.jkl", 24));
        assertThrows(IllegalArgumentException.class, () -> SubnetCalculator.calculate("192.168.1.1", 35));
    }
}
