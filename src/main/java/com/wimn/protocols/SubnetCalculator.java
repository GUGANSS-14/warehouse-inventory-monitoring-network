package com.wimn.protocols;

import com.wimn.models.SubnetInfo;

public class SubnetCalculator {

    public static SubnetInfo calculate(String ipStr, int prefix) throws IllegalArgumentException {
        if (prefix < 0 || prefix > 32) {
            throw new IllegalArgumentException("Prefix length must be between 0 and 32.");
        }
        long ipNum = parseIp(ipStr);

        long maskNum = (prefix == 0) ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        long netNum = ipNum & maskNum;
        long wildcardNum = ~maskNum & 0xFFFFFFFFL;
        long bcastNum = netNum | wildcardNum;

        long totalAddresses = 1L << (32 - prefix);
        long usableHosts;
        long firstUsable;
        long lastUsable;

        if (prefix == 31) {
            // RFC 3021: Point-to-Point Links (2 usable addresses, no subnet/broadcast)
            usableHosts = 2;
            firstUsable = netNum;
            lastUsable = bcastNum;
        } else if (prefix == 32) {
            // Single Host / Loopback
            usableHosts = 1;
            firstUsable = netNum;
            lastUsable = netNum;
        } else if (prefix >= 30) {
            usableHosts = 2;
            firstUsable = netNum + 1;
            lastUsable = bcastNum - 1;
        } else {
            usableHosts = totalAddresses - 2;
            firstUsable = netNum + 1;
            lastUsable = bcastNum - 1;
        }

        SubnetInfo info = new SubnetInfo();
        info.setInputIp(formatIp(ipNum));
        info.setPrefixLength(prefix);
        info.setNetworkAddress(formatIp(netNum));
        info.setSubnetMask(formatIp(maskNum));
        info.setBroadcastAddress(prefix == 32 ? "N/A (Host Route)" : formatIp(bcastNum));
        info.setFirstUsableHost(formatIp(firstUsable));
        info.setLastUsableHost(formatIp(lastUsable));
        info.setTotalAddresses(totalAddresses);
        info.setUsableHosts(usableHosts);
        info.setCidrNotation(info.getNetworkAddress() + "/" + prefix);
        info.setWildCardMask(formatIp(wildcardNum));
        info.setIpClass(determineClass(ipNum));
        info.setBinaryIp(toBinary32(ipNum));
        info.setBinarySubnetMask(toBinary32(maskNum));

        return info;
    }

    public static long parseIp(String ipStr) throws IllegalArgumentException {
        if (ipStr == null || ipStr.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty.");
        }
        String[] parts = ipStr.trim().split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 format. Expected 4 octets (e.g. 192.168.1.1).");
        }
        long num = 0;
        for (int i = 0; i < 4; i++) {
            try {
                int octet = Integer.parseInt(parts[i]);
                if (octet < 0 || octet > 255) {
                    throw new IllegalArgumentException("Octet " + octet + " out of valid range 0-255.");
                }
                num = (num << 8) | octet;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid numeric octet: " + parts[i]);
            }
        }
        return num;
    }

    public static String formatIp(long ipNum) {
        return String.format("%d.%d.%d.%d",
                (ipNum >> 24) & 0xFF,
                (ipNum >> 16) & 0xFF,
                (ipNum >> 8) & 0xFF,
                ipNum & 0xFF);
    }

    public static String toBinary32(long val) {
        StringBuilder sb = new StringBuilder();
        for (int i = 3; i >= 0; i--) {
            int octet = (int) ((val >> (i * 8)) & 0xFF);
            String bin = String.format("%8s", Integer.toBinaryString(octet)).replace(' ', '0');
            sb.append(bin);
            if (i > 0) sb.append(".");
        }
        return sb.toString();
    }

    private static String determineClass(long ipNum) {
        int firstOctet = (int) ((ipNum >> 24) & 0xFF);
        if (firstOctet >= 1 && firstOctet <= 126) return "Class A (Default /8)";
        if (firstOctet == 127) return "Loopback / Diagnostic (127.0.0.0/8)";
        if (firstOctet >= 128 && firstOctet <= 191) return "Class B (Default /16)";
        if (firstOctet >= 192 && firstOctet <= 223) return "Class C (Default /24)";
        if (firstOctet >= 224 && firstOctet <= 239) return "Class D (Multicast)";
        return "Class E (Experimental)";
    }
}
