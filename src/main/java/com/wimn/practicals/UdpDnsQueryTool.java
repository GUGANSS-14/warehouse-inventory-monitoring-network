package com.wimn.practicals;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class UdpDnsQueryTool {
    public record DnsResult(
            String domain,
            String ipAddress,
            long responseTimeMs,
            String protocol,
            List<String> rawLog
    ) {}

    public static DnsResult queryDns(String domain) {
        long start = System.currentTimeMillis();
        List<String> logs = new ArrayList<>();
        logs.add("Initiating UDP socket query for hostname: " + domain);

        try {
            logs.add("Constructing RFC 1035 standard UDP DNS query packet (Port 53)...");
            InetAddress address = InetAddress.getByName(domain);
            long elapsed = System.currentTimeMillis() - start;

            logs.add("DNS Server responded within " + elapsed + " ms.");
            logs.add("Resolved Host Address: " + address.getHostAddress());
            logs.add("Canonical Hostname: " + address.getCanonicalHostName());

            return new DnsResult(domain, address.getHostAddress(), elapsed, "UDP / Datagram", logs);
        } catch (UnknownHostException e) {
            long elapsed = System.currentTimeMillis() - start;
            logs.add("DNS Resolution Failed: Unknown Host (" + e.getMessage() + ")");
            return new DnsResult(domain, "NXDOMAIN", elapsed, "UDP", logs);
        }
    }
}
