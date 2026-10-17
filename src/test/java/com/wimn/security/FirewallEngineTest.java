package com.wimn.security;

import com.wimn.models.FirewallRule;
import com.wimn.models.NetworkPacket;
import com.wimn.models.PacketHeader;
import com.wimn.repositories.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FirewallEngineTest {
    private FirewallEngine firewallEngine;

    @BeforeEach
    void setUp() {
        EventRepository eventRepo = new EventRepository();
        firewallEngine = new FirewallEngine(eventRepo);
    }

    @Test
    @DisplayName("Test Firewall ALLOW for Central Server Inventory Port 8080")
    void testAllowCentralServer() {
        PacketHeader hdr = new PacketHeader("192.168.10.11", "192.168.1.50", 49152, 8080, "TCP", 64, 100, 200, 10);
        NetworkPacket pkt = new NetworkPacket("PKT-TEST-ALLOW", "WS-RCV-01", "SRV-MAIN-01", hdr, "QUERY", 64);

        FirewallEngine.FirewallDecision decision = firewallEngine.evaluatePacket(pkt);
        assertEquals(FirewallRule.Action.ALLOW, decision.action());
    }

    @Test
    @DisplayName("Test Firewall DENY for Unauthorized CCTV Camera cross-connecting to Dispatch")
    void testDenyUnauthorizedCrossVlan() {
        PacketHeader hdr = new PacketHeader("192.168.40.21", "192.168.30.11", 5004, 80, "TCP", 64, 100, 200, 40);
        NetworkPacket pkt = new NetworkPacket("PKT-TEST-DENY", "CAM-SEC-01", "WS-DSP-01", hdr, "CROSS_PROBE", 64);

        FirewallEngine.FirewallDecision decision = firewallEngine.evaluatePacket(pkt);
        assertEquals(FirewallRule.Action.DENY, decision.action());
        assertNotNull(decision.matchedRule());
        assertEquals("FW-30", decision.matchedRule().getRuleId());
    }
}
