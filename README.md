# Warehouse Inventory Monitoring Network (WIMN)
### Java-Based Computer Networks Capstone Simulation | Subject Code: BE23CS402

---

## 1. Project Overview

The **Warehouse Inventory Monitoring Network (WIMN)** is an enterprise-grade, interactive computer networks simulation and visualization platform built in Java. It models and demonstrates how modern computer networking principles govern communication across five operational warehouse departments:

1. **Receiving Department** (VLAN 10, `192.168.10.0/24`) – Inbound inventory intake, handheld barcode scanners (Wi-Fi 6), and put-away notifications.
2. **Storage Department** (VLAN 20, `192.168.20.0/24`) – High-bay pallet racks, automated RFID gate readers (865MHz), and low-stock alerts.
3. **Dispatch Department** (VLAN 30, `192.168.30.0/24`) – Outbound order fulfillment, transactional stock deduction, and cross-department verification.
4. **Security Office** (VLAN 40, `192.168.40.0/24`) – 24/7 CCTV RTSP surveillance streams, access-control terminals, and firewall network segmentation.
5. **Manager Office** (VLAN 50, `192.168.50.0/24`) – Executive operational dashboard, SLA availability metrics, and CSV reporting.
6. **Core Infrastructure / Server Room** (VLAN 1, `192.168.1.0/24`) – Enterprise edge router (`RTR-CORE-01`), Layer-3 core switch (`SW-CORE-01`), and central inventory & DNS/DHCP server (`SRV-MAIN-01`).

---

## 2. Technical Stack & Architecture

- **Language:** Java 21 LTS
- **UI Framework:** JavaFX 21 (Rich Industrial Dark Navy & Enterprise Light Themes)
- **3D Graphics:** JavaFX 3D (`SubScene`, `PerspectiveCamera`, Orbit/Zoom Controls) with a seamless 2D canvas fallback
- **Persistence:** SQLite with JDBC (`sqlite-jdbc` 3.45.1.0) with WAL concurrency mode and automated schema creation & demo seeding
- **Build Tool:** Apache Maven 3.9+ (`pom.xml`)
- **Unit Testing:** JUnit 5 (`junit-jupiter-api` 5.10.2)
- **Design Aesthetic:** Industrial technology operations center with dark navy surfaces, vibrant cyan/emerald/amber/red indicators, and real-time animations.

```
                              [ External Gateway / Internet ]
                                             |
                                  [ RTR-CORE-01 Router ]
                                  (192.168.1.1 / VLAN 1)
                                             |  Fiber 10G
                                   [ SW-CORE-01 Switch ]
                                   (192.168.1.2 / VLAN 1)
                                      /      |       \  Fiber 10G
             +-----------------------+       |        +-----------------------+
             |                               |                                |
  [ SW-RCV-01 Switch ]              [ SRV-MAIN-01 ]               [ SW-DSP-01 Switch ]
 (192.168.10.2 / VLAN 10)         (192.168.1.50 / VLAN 1)        (192.168.30.2 / VLAN 30)
      |          |               (Inventory, DNS, DHCP)                |          |
  [WS-RCV-01] [AP-RCV-01]                    |                     [WS-DSP-01] [SCN-DSP-01]
                 |                           |
            [SCN-RCV-01]            +--------+--------+
                                    |                 |
                          [ SW-STR-01 Switch ]   [ SW-SEC-01 Switch ]   [ SW-MGR-01 Switch ]
                         (192.168.20.2 / VLAN 20)(192.168.40.2 / VLAN 40)(192.168.50.2 / VLAN 50)
                              |          |            |          |            |
                         [RFID-STR]  [IOT-STR]    [WS-SEC-01] [CAM-SEC]   [WS-MGR-01]
```

---

## 3. Academic Syllabus & Course Outcome (CO) Mapping

The project provides dedicated interactive modules covering the entire **BE23CS402 Computer Networks** syllabus:

| Unit | Syllabus Topics Covered | Application Module | Course Outcome | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Unit-I** | Building a Network, Edge vs Core, OSI 7-Layer, TCP/IP Suite, Physical Transmission Media, Signal Encoding (NRZ, Manchester), Performance Metrics (Delay, Throughput) | `Topology2DView`<br>`Topology3DView`<br>`AnalyticsView`<br>`PracticalsView` | **CO1**: Apply networking concepts to design basic infrastructure models. | **COMPLETE** |
| **Unit-II** | Framing (HDLC Bit-Stuffing & Byte-Stuffing), Flow Control (Stop-and-Wait, Sliding Window Go-Back-N, Selective Repeat), Error Control (CRC-16, CRC-32, Checksum), Ethernet MAC Address Learning, Wireless (Wi-Fi 6 vs Bluetooth vs Zigbee) | `DataLinkLabView`<br>`WarehouseOpsView` | **CO2**: Apply link-layer services in network design and troubleshooting. | **COMPLETE** |
| **Unit-III** | Circuit vs Packet Switching, IPv4 Addressing, Subnetting Calculator (/0 to /32 with /31 RFC 3021 & /32 host routes), Classless CIDR, Distance Vector (Bellman-Ford), Link State (Dijkstra Shortest Path), IPv6 Compression (RFC 5952) | `SubnettingLabView`<br>`RoutingLabView`<br>`Topology2DView` | **CO3**: Evaluate routing algorithms for efficient data forwarding. | **COMPLETE** |
| **Unit-IV** | Process-to-Process Transport, UDP Datagrams, TCP 3-Way Handshake (SYN/ACK/ISN), Reliable Byte-Stream Delivery, TCP Congestion Control (Slow Start, CWND, Fast Retransmit), SCTP Multi-Streaming, TCP vs. UDP Live Benchmark | `TransportLabView` | **CO4**: Describe protocols for various network functions. | **COMPLETE** |
| **Unit-V** | Application Principles, HTTP/REST API Simulation, FTP Control/Data, Email Dispatch Notifications (SMTP/POP3/IMAP), DHCP Address Allocation (DORA), DNS Resolution & Cache, DASH Adaptive Video Bitrate, QUIC Protocol, Stateful Firewall ACL Filtering | `AppServicesLabView`<br>`SecurityFirewallView`<br>`WarehouseOpsView` | **CO5**: Manage and secure network applications and services. | **COMPLETE** |
| **Practicals** | Java TCP Sockets Localhost Chat, Java UDP DNS Resolution, Packet Header Inspector (Layer 2/3/4/7 Decoding), Network CLI Commands Tutor (`ping`, `ipconfig`, `tracert`, `nslookup`, `arp`, `netstat`) | `PracticalsView` | **CO4 / CO1** | **COMPLETE** |

---

## 4. Key Application Features

### 1. Executive Operations Dashboard
- Real-time KPI metric cards: Total Connected Devices, Online/Offline Ratio, Active Links, Availability Percentage (SLA), Average Packet Latency, Packet Delivery Ratio (PDR %), Inventory Items, and Low-Stock Warnings.
- Live Simulation Engine Clock with single-click **Start Simulation / Pause Simulation** controls.
- Dynamic charts: Real-Time Latency & Jitter Line Chart, Device Health Doughnut Chart.
- Live stream of recent network, inventory, and security audit events.

### 2. Multi-Department Warehouse Operations
- **Receiving Dept:** Interactive inbound intake form. Generates simulated barcode scanner transmissions from `SCN-RCV-01` to `SRV-MAIN-01` on VLAN 10 and alerts Storage.
- **Storage Dept:** Storage aisles and rack location ledger (`Rack-A1` to `Rack-D4`). Includes interactive RFID gate scan simulator and low-stock warning tables.
- **Dispatch Dept:** Order creation with strict stock validation (denies orders greater than available stock). Traces packet flow: Dispatch -> Core -> Server -> Storage.
- **Security Office:** Simulated RTSP CCTV feeds for warehouse perimeter, gates, and loading bays. Demonstrates VLAN 40 isolation.
- **Manager Office:** Consolidated stock valuations, department health monitors, and one-click CSV export.

### 3. Interactive 3D Warehouse Visualization
- Built using JavaFX 3D with full 360° mouse drag orbit rotation, scroll wheel zoom, and pan controls.
- Renders warehouse floor, 3D multi-tier storage racks, equipment meshes, and 3D link conduits.
- Clickable 3D nodes with instant side telemetry inspection.
- Instant toggle between 3D warehouse view and 2D logical topology.

### 4. 2D Network Topology & Packet Animation
- Hierarchical star topology canvas with animated glowing packet particles moving across links.
- Color-coded links: Emerald Green (Healthy), Amber (Congested), Red (Link Severed).
- Interactive "Send Test Packet" tool: select source and destination, choose TCP/UDP/ICMP/HTTP, and watch the packet travel hop-by-hop.

### 5. Network Device Management Registry
- Complete device inventory: ID, Name, Type, Department, IPv4, IPv6, MAC, Subnet Mask, Gateway, VLAN, Connection Type, and Utilization.
- Full CRUD operations: Add Device, Edit Parameters, Toggle Online/Offline, and Delete.
- Live search bar and department/device-type filters.

### 6. Interactive Subnet Calculator & CIDR Tool
- Full IPv4 subnet calculator accepting any prefix length from `/0` to `/32`.
- Correctly computes Network Address, Subnet Mask, Broadcast Address, Usable Host Range, Usable Host Count, Total Addresses, and Wildcard Mask.
- Handles edge cases: `/31` (RFC 3021 point-to-point links) and `/32` (single host loopback).
- Interactive 32-bit binary representation showing network bits vs. host bits.
- IPv6 RFC 5952 zero-compressor (`::`) and scope classifier.

### 7. Routing Simulator (Link-State & Distance-Vector)
- **Dijkstra Shortest Path (OSPF):** Computes lowest-cost path, link weights, and step-by-step relaxation logs. Automatically detects severed links and routes through backup paths!
- **Bellman-Ford (RIP):** Computes distributed distance vectors with iteration logs and convergence verification.
- **FIB Table Inspector:** Inspect the complete forwarding table of any router or switch in the warehouse.

### 8. Data Link Layer Lab
- **Framing:** Interactive HDLC bit-stuffing (inserts `0` after five consecutive `1`s) and destuffing.
- **Error Control:** CRC-16 (0x1021), CRC-32 (IEEE 802.3 Ethernet), and Checksum calculators with single-click bit-inversion corruption injection.
- **Flow Control:** Stop-and-Wait, Go-Back-N, and Selective Repeat with configurable window size $N$ (1 to 8).
- **MAC Learning:** Switch dynamic MAC address learning table demonstration with learning, unicast forwarding, and unknown destination flooding.

### 9. Transport Layer Lab
- **TCP Handshake:** 3-Way Handshake step-by-step simulator (`SYN` $\to$ `SYN-ACK` $\to$ `ACK`) with initial sequence numbers (ISN) and state tracking (`CLOSED` to `ESTABLISHED`).
- **Congestion Control:** Demonstrates Congestion Window (`cwnd`) growth during Slow Start, Congestion Avoidance, and Fast Retransmit multiplicative decrease upon packet loss.
- **TCP vs. UDP Live Benchmark:** Interactive slider-driven tool comparing throughput, latency, loss tolerance, and protocol overhead side-by-side.

### 10. Application Services Lab
- **HTTP / REST API:** Local request/response engine demonstrating GET and POST methods, status codes (200, 201, 404), headers, and JSON payloads.
- **DHCP DORA Simulation:** Four-step lease allocation (`Discover` $\to$ `Offer` $\to$ `Request` $\to$ `Acknowledge`) with IP pool tracking.
- **DNS Resolution:** Simulates local DNS resolver cache, authoritative query, TTL expiration, and `NXDOMAIN` handling.
- **DASH Video Streaming:** Demonstrates Dynamic Adaptive Streaming over HTTP by automatically adjusting video resolution (240p to 1080p) based on simulated bandwidth.

### 11. Security, Firewall & Fault Injection
- **Firewall ACL Engine:** Stateful rule evaluation based on source/destination subnet, protocol, and port. Demonstrates `ALLOW` and `DENY` decisions with match logs.
- **Fault Injection Controller:** Sever any physical link or crash any network device on the fly. Watch the topology turn red and observe routing algorithms automatically reroute traffic via backup links.

### 12. Practical Experiments Lab
- **Java TCP Sockets:** Fully functional local TCP Chat Server (`ServerSocket`) and Client (`Socket`) running on `127.0.0.1:9191` with message broadcasting.
- **Java UDP DNS Tool:** Real UDP datagram resolution tool measuring DNS query latency in milliseconds.
- **Packet Header Inspector:** Decodes simulated warehouse packets across OSI layers 2, 3, 4, and 7, including binary hex dumps.
- **Network CLI Reference:** Interactive guide covering `ping`, `ipconfig`, `tracert`, `nslookup`, `arp`, and `netstat` with Windows/Linux syntax and sample outputs.

---

## 5. How to Compile, Test, and Run

### Prerequisites
- Operating System: Windows, macOS, or Linux.
- OpenJDK 21 LTS (or higher).
- Apache Maven 3.9+ (or use included Maven scripts).

### Quick Launch on Windows
Double-click `run.bat` in the project root directory, or execute in PowerShell:
```powershell
.\run.bat
```

### Running via Standard Maven Commands
To compile and launch the JavaFX application:
```powershell
mvn clean javafx:run
```

### Running Automated Unit Tests
To run all 14 automated tests covering subnetting, Dijkstra/Bellman-Ford routing, firewall rules, inventory transactions, and CRC error detection:
```powershell
mvn test
```
Or simply double-click `test.bat`.

---

## 6. Directory Structure

```
warehouse-inventory-network/
├── pom.xml                               # Maven project configuration
├── run.bat                               # Windows launcher script
├── test.bat                              # Windows test runner script
├── README.md                             # Comprehensive project documentation
├── warehouse_network.db                  # Persistent SQLite database (auto-seeded)
├── src/
│   ├── main/
│   │   ├── java/com/wimn/
│   │   │   ├── MainApp.java              # JavaFX Application entry point
│   │   │   ├── models/                   # NetworkDevice, Link, Packet, Inventory, Firewall models
│   │   │   ├── devices/                  # Specialized Router, Switch, Server, Camera, Scanner devices
│   │   │   ├── topology/                 # WarehouseTopology & graph construction
│   │   │   ├── routing/                  # RoutingEngine (Dijkstra, Bellman-Ford, RoutingTable)
│   │   │   ├── protocols/                # SubnetCalculator, TCPSession, CRCUtils, Framing, Encoding
│   │   │   ├── networkservices/          # DHCPSimulator, DNSSimulator, HTTPSimulator, DASHOptimizer
│   │   │   ├── security/                 # FirewallEngine (ALLOW/DENY ACLs)
│   │   │   ├── inventory/                # InventoryService (Stock validation & transactions)
│   │   │   ├── repositories/             # DatabaseManager, SQLite repositories
│   │   │   ├── practicals/               # Real TCP Chat Server/Client, UDP DNS Tool, Packet Inspector
│   │   │   ├── controllers/              # NavigationManager
│   │   │   ├── views/                    # 15 JavaFX application views & 3D visualization
│   │   │   └── utilities/                # CsvExporter, ThemeManager
│   │   └── resources/
│   │       └── css/
│   │           ├── dark-theme.css        # Industrial dark navy enterprise CSS
│   │           └── light-theme.css       # Clean enterprise light CSS
│   └── test/
│       └── java/com/wimn/
│           ├── routing/RoutingEngineTest.java
│           ├── simulation/SubnetCalculatorTest.java
│           ├── security/FirewallEngineTest.java
│           ├── inventory/InventoryServiceTest.java
│           └── protocols/CRCUtilsTest.java
```

---

## 7. Troubleshooting Guide

1. **Missing Java / Maven:**
   - If `java` or `mvn` are not in your system PATH, the project comes with pre-configured portable tools in `.tools/`. The `run.bat` and `test.bat` scripts automatically detect and bind them.
2. **JavaFX Graphics Driver Compatibility:**
   - If your graphics hardware does not support hardware-accelerated 3D rendering, the application provides an instant **Switch to 2D Logical View** button on the 3D topology page, which uses a 100% compatible 2D canvas.
3. **Database Reset:**
   - If you wish to restore the SQLite database to its factory demo state, navigate to the **Settings and Syllabus** page and click **Reset Demo SQLite Database**.
4. **Local TCP Chat Port Conflict:**
   - The TCP Chat experiment binds to `127.0.0.1:9191`. If port 9191 is in use by another application, ensure to stop any existing server process before starting.

---

**Developed for BE23CS402 Computer Networks Capstone Evaluation.**
