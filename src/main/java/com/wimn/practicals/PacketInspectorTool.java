package com.wimn.practicals;

import com.wimn.models.NetworkPacket;
import com.wimn.models.PacketHeader;

import java.util.ArrayList;
import java.util.List;

public class PacketInspectorTool {
    public record DecodedLayer(String layerName, int osiLevel, List<String> fields) {}
    public record PacketInspectionReport(
            String packetId,
            String hexDump,
            List<DecodedLayer> layers,
            String summary
    ) {}

    public static PacketInspectionReport inspect(NetworkPacket packet) {
        List<DecodedLayer> layers = new ArrayList<>();
        PacketHeader hdr = packet.getHeader();

        // Layer 2: Data Link Layer (Ethernet II Frame)
        List<String> l2Fields = new ArrayList<>();
        l2Fields.add("Destination MAC: 00:1A:2B:01:00:02");
        l2Fields.add("Source MAC: 00:1A:2B:10:00:21");
        l2Fields.add("Type: IPv4 (0x0800)");
        l2Fields.add("802.1Q VLAN Tag: ID " + hdr.getVlanId() + " (Priority: 0)");
        l2Fields.add("Frame Check Sequence (FCS / CRC-32): 0x4A82F109");
        layers.add(new DecodedLayer("Data Link (Ethernet II Frame)", 2, l2Fields));

        // Layer 3: Network Layer (IPv4 Header)
        List<String> l3Fields = new ArrayList<>();
        l3Fields.add("Version: 4 | Header Length: 20 Bytes (IHL = 5)");
        l3Fields.add("Type of Service (DiffServ): 0x00 (Best Effort)");
        l3Fields.add("Total Length: " + (20 + 20 + packet.getPayloadSizeBytes()) + " Bytes");
        l3Fields.add("Identification: 0x3E12 (15890)");
        l3Fields.add("Flags: 0x02 (Don't Fragment)");
        l3Fields.add("Time to Live (TTL): " + hdr.getTtl() + " hops");
        l3Fields.add("Protocol: " + hdr.getProtocol() + " (" + (hdr.getProtocol().equals("TCP") ? "6" : "17") + ")");
        l3Fields.add("Header Checksum: 0xA4F2 [Valid]");
        l3Fields.add("Source IP: " + hdr.getSourceIp());
        l3Fields.add("Destination IP: " + hdr.getDestinationIp());
        layers.add(new DecodedLayer("Network Layer (IPv4 Header)", 3, l3Fields));

        // Layer 4: Transport Layer (TCP / UDP Header)
        List<String> l4Fields = new ArrayList<>();
        l4Fields.add("Source Port: " + hdr.getSourcePort());
        l4Fields.add("Destination Port: " + hdr.getDestinationPort());
        if ("TCP".equalsIgnoreCase(hdr.getProtocol())) {
            l4Fields.add("Sequence Number: " + hdr.getSequenceNumber());
            l4Fields.add("Acknowledgment Number: " + hdr.getAckNumber());
            l4Fields.add("Header Length (Data Offset): 20 Bytes (5 words)");
            l4Fields.add("Flags: [" + (hdr.getTcpFlags().isEmpty() ? "ACK" : hdr.getTcpFlags()) + "]");
            l4Fields.add("Window Size: 65,535 bytes");
            l4Fields.add("Urgent Pointer: 0");
        } else {
            l4Fields.add("Length: " + (8 + packet.getPayloadSizeBytes()) + " Bytes");
            l4Fields.add("Checksum: 0x981C");
        }
        layers.add(new DecodedLayer("Transport Layer (" + hdr.getProtocol() + ")", 4, l4Fields));

        // Layer 7: Application Payload
        List<String> l7Fields = new ArrayList<>();
        l7Fields.add("Payload Size: " + packet.getPayloadSizeBytes() + " Bytes");
        l7Fields.add("Raw Content: \"" + packet.getPayload() + "\"");
        l7Fields.add("Encoding: UTF-8 Plaintext / JSON");
        layers.add(new DecodedLayer("Application Layer (Payload Data)", 7, l7Fields));

        // Hex Dump Generation
        StringBuilder hex = new StringBuilder();
        byte[] payloadBytes = packet.getPayload().getBytes();
        for (int i = 0; i < Math.min(64, payloadBytes.length); i++) {
            hex.append(String.format("%02X ", payloadBytes[i]));
            if ((i + 1) % 16 == 0) hex.append("\n");
        }

        String summary = String.format("Packet %s: %s:%d -> %s:%d [%s] Status: %s",
                packet.getPacketId(), hdr.getSourceIp(), hdr.getSourcePort(),
                hdr.getDestinationIp(), hdr.getDestinationPort(), hdr.getProtocol(), packet.getStatus());

        return new PacketInspectionReport(packet.getPacketId(), hex.toString(), layers, summary);
    }
}
