package com.wimn.protocols;

import java.util.ArrayList;
import java.util.List;

public class TCPSession {
    public enum State {
        CLOSED,
        SYN_SENT,
        SYN_RECEIVED,
        ESTABLISHED,
        FIN_WAIT_1,
        FIN_WAIT_2,
        TIME_WAIT
    }

    public record TCPExchange(
            int step,
            String direction, // "Client -> Server" or "Server -> Client"
            String flags,     // "SYN", "SYN+ACK", "ACK", "PSH+ACK", "FIN", "FIN+ACK"
            long seqNum,
            long ackNum,
            int windowSize,
            int cwnd,
            String description
    ) {}

    private State currentState = State.CLOSED;
    private long clientSeq = 1000;
    private long serverSeq = 5000;
    private int advertisedWindow = 65535;
    private int cwnd = 1; // Congestion Window in MSS units (Slow Start starts at 1)
    private int ssthresh = 16; // Slow start threshold

    public List<TCPExchange> simulateThreeWayHandshake() {
        List<TCPExchange> list = new ArrayList<>();
        currentState = State.CLOSED;

        // Step 1: Client sends SYN
        currentState = State.SYN_SENT;
        list.add(new TCPExchange(
                1, "Client -> Server", "SYN", clientSeq, 0, advertisedWindow, cwnd,
                "Connection Request: Client selects ISN = " + clientSeq + ", SYN flag set."
        ));

        // Step 2: Server responds with SYN+ACK
        currentState = State.SYN_RECEIVED;
        serverSeq = 5000;
        long ackForClient = clientSeq + 1;
        list.add(new TCPExchange(
                2, "Server -> Client", "SYN, ACK", serverSeq, ackForClient, advertisedWindow, cwnd,
                "Acknowledgment & Handshake: Server selects ISN = " + serverSeq + ", ACKs client ISN+1 (" + ackForClient + ")."
        ));

        // Step 3: Client sends final ACK
        currentState = State.ESTABLISHED;
        clientSeq = ackForClient;
        long ackForServer = serverSeq + 1;
        list.add(new TCPExchange(
                3, "Client -> Server", "ACK", clientSeq, ackForServer, advertisedWindow, cwnd,
                "Connection Established: Client ACKs server ISN+1 (" + ackForServer + "). Full-Duplex Socket is now ESTABLISHED."
        ));

        return list;
    }

    public List<TCPExchange> simulateDataTransferWithCongestionControl(int segmentsCount, int packetLossAt) {
        List<TCPExchange> list = new ArrayList<>();
        if (currentState != State.ESTABLISHED) {
            simulateThreeWayHandshake();
        }

        int step = 4;
        long currentClientSeq = clientSeq;
        long currentServerSeq = serverSeq + 1;

        for (int i = 1; i <= segmentsCount; i++) {
            boolean lost = (i == packetLossAt);

            list.add(new TCPExchange(
                    step++, "Client -> Server", "PSH, ACK", currentClientSeq, currentServerSeq,
                    advertisedWindow, cwnd,
                    "Transmitting Segment " + i + " [Seq: " + currentClientSeq + ", " + 1460 + " bytes] " +
                    (lost ? " **PACKET LOST IN TRANSIT**" : "")
            ));

            if (lost) {
                // Triple duplicate ACK or timeout -> Fast Retransmit
                ssthresh = Math.max(2, cwnd / 2);
                cwnd = 1; // Multiplicative decrease
                list.add(new TCPExchange(
                        step++, "Server -> Client", "DUP ACK (Fast Retransmit)", currentServerSeq, currentClientSeq,
                        advertisedWindow, cwnd,
                        "Duplicate ACK received for missing segment! Fast recovery: ssthresh set to " + ssthresh + ", CWND collapsed to 1 MSS."
                ));
                // Retransmission
                list.add(new TCPExchange(
                        step++, "Client -> Server", "RETRANSMIT", currentClientSeq, currentServerSeq,
                        advertisedWindow, cwnd,
                        "Retransmitting lost Segment " + i + " [Seq: " + currentClientSeq + "]."
                ));
            }

            // Normal ACK
            currentClientSeq += 1460;
            currentServerSeq += 1;

            // Congestion Window growth
            if (cwnd < ssthresh) {
                cwnd *= 2; // Slow Start exponential growth
            } else {
                cwnd += 1; // Congestion Avoidance additive linear increase
            }

            list.add(new TCPExchange(
                    step++, "Server -> Client", "ACK", currentServerSeq, currentClientSeq,
                    advertisedWindow, cwnd,
                    "Server ACKs data up to Seq " + currentClientSeq + ". CWND updated to " + cwnd + " MSS."
            ));
        }

        return list;
    }

    public List<TCPExchange> simulateTeardown() {
        List<TCPExchange> list = new ArrayList<>();
        int step = 20;

        // Step 1: Client sends FIN
        currentState = State.FIN_WAIT_1;
        list.add(new TCPExchange(
                step++, "Client -> Server", "FIN, ACK", clientSeq, serverSeq, advertisedWindow, cwnd,
                "Active Close: Client sends FIN. No more data to send."
        ));

        // Step 2: Server sends ACK
        currentState = State.FIN_WAIT_2;
        list.add(new TCPExchange(
                step++, "Server -> Client", "ACK", serverSeq, clientSeq + 1, advertisedWindow, cwnd,
                "Passive Close: Server ACKs client FIN. Connection half-closed."
        ));

        // Step 3: Server sends FIN
        list.add(new TCPExchange(
                step++, "Server -> Client", "FIN, ACK", serverSeq + 1, clientSeq + 1, advertisedWindow, cwnd,
                "Server completes pending transmissions and sends its own FIN."
        ));

        // Step 4: Client final ACK
        currentState = State.TIME_WAIT;
        list.add(new TCPExchange(
                step++, "Client -> Server", "ACK", clientSeq + 1, serverSeq + 2, advertisedWindow, cwnd,
                "Client sends final ACK and enters TIME_WAIT (2*MSL) before closing socket."
        ));
        currentState = State.CLOSED;

        return list;
    }

    public State getCurrentState() { return currentState; }
    public int getCwnd() { return cwnd; }
}
