package com.wimn.practicals;

import java.util.List;

public class NetworkCliHelper {
    public record CommandDoc(
            String commandName,
            String windowsSyntax,
            String linuxSyntax,
            String purpose,
            String sampleOutput
    ) {}

    public static List<CommandDoc> getReferenceCommands() {
        return List.of(
            new CommandDoc(
                "ping",
                "ping -n 4 192.168.1.50",
                "ping -c 4 192.168.1.50",
                "Tests Layer-3 network reachability and calculates Round-Trip Time (RTT) using ICMP Echo Request and Echo Reply.",
                """
                Pinging 192.168.1.50 with 32 bytes of data:
                Reply from 192.168.1.50: bytes=32 time=2ms TTL=64
                Reply from 192.168.1.50: bytes=32 time=1ms TTL=64
                Reply from 192.168.1.50: bytes=32 time=2ms TTL=64
                Reply from 192.168.1.50: bytes=32 time=1ms TTL=64

                Ping statistics for 192.168.1.50:
                    Packets: Sent = 4, Received = 4, Lost = 0 (0% loss),
                Approximate round trip times in milli-seconds:
                    Minimum = 1ms, Maximum = 2ms, Average = 1ms
                """
            ),
            new CommandDoc(
                "ipconfig / ifconfig",
                "ipconfig /all",
                "ip addr show  (or ifconfig)",
                "Displays all current TCP/IP network configuration values, IP addresses, subnet masks, default gateways, and MAC addresses.",
                """
                Ethernet adapter Local Area Connection (VLAN 10):
                   Connection-specific DNS Suffix  . : warehouse.local
                   Description . . . . . . . . . . . : Intel(R) I219-LM Gigabit Network
                   Physical Address. . . . . . . . . : 00-1A-2B-10-00-11
                   DHCP Enabled. . . . . . . . . . . : Yes
                   IPv4 Address. . . . . . . . . . . : 192.168.10.11(Preferred)
                   Subnet Mask . . . . . . . . . . . : 255.255.255.0
                   Default Gateway . . . . . . . . . : 192.168.10.1
                   DNS Servers . . . . . . . . . . . : 192.168.1.50
                """
            ),
            new CommandDoc(
                "tracert / traceroute",
                "tracert 192.168.1.50",
                "traceroute 192.168.1.50",
                "Determines the path taken to a destination by sending ICMP/UDP packets with increasing Time-to-Live (TTL) values.",
                """
                Tracing route to inventory.warehouse.local [192.168.1.50]
                over a maximum of 30 hops:

                  1    <1 ms    <1 ms    <1 ms  192.168.10.2 [SW-RCV-01]
                  2     1 ms    <1 ms     1 ms  192.168.1.2  [SW-CORE-01]
                  3     1 ms     1 ms     1 ms  192.168.1.50 [SRV-MAIN-01]

                Trace complete.
                """
            ),
            new CommandDoc(
                "nslookup",
                "nslookup inventory.warehouse.local 192.168.1.50",
                "nslookup inventory.warehouse.local 192.168.1.50",
                "Queries Domain Name System (DNS) servers to find IP address mappings or DNS records.",
                """
                Server:  dns.warehouse.local
                Address:  192.168.1.50

                Name:    inventory.warehouse.local
                Address:  192.168.1.50
                Aliases:  inv-master.warehouse.local
                """
            ),
            new CommandDoc(
                "arp",
                "arp -a",
                "ip neigh show  (or arp -a)",
                "Displays the Address Resolution Protocol (ARP) table mapping IP addresses to physical MAC addresses in the local broadcast domain.",
                """
                Interface: 192.168.10.11 --- 0xb
                  Internet Address      Physical Address      Type
                  192.168.10.1          00-1a-2b-10-00-01     dynamic
                  192.168.10.2          00-1a-2b-10-00-02     dynamic
                  192.168.10.255        ff-ff-ff-ff-ff-ff     static
                """
            ),
            new CommandDoc(
                "netstat",
                "netstat -ano",
                "ss -tulnp  (or netstat -tulnp)",
                "Displays active TCP connections, listening ports, ethernet statistics, routing table, and IP protocol counters.",
                """
                Active Connections

                  Proto  Local Address          Foreign Address        State           PID
                  TCP    127.0.0.1:9191         0.0.0.0:0              LISTENING       4120
                  TCP    192.168.10.11:49152    192.168.1.50:8080      ESTABLISHED     4120
                  UDP    0.0.0.0:53             *:*                                    2810
                """
            )
        );
    }
}
