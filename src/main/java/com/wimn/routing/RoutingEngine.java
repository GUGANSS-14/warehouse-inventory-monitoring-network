package com.wimn.routing;

import com.wimn.models.LinkStatus;
import com.wimn.models.NetworkDevice;
import com.wimn.models.NetworkLink;
import com.wimn.topology.WarehouseTopology;

import java.util.*;

public class RoutingEngine {
    private final WarehouseTopology topology;

    public RoutingEngine(WarehouseTopology topology) {
        this.topology = topology;
    }

    public record DijkstraResult(
            boolean pathFound,
            List<String> path,
            double totalCost,
            List<String> stepLogs,
            Map<String, Double> distances
    ) {}

    public record DistanceVectorStep(int iteration, String node, String destination, double cost, String nextHop) {}
    public record DistanceVectorResult(
            boolean converged,
            int totalIterations,
            List<DistanceVectorStep> stepHistory,
            Map<String, Map<String, Double>> distanceMatrix
    ) {}

    /**
     * Dijkstra's Shortest Path Algorithm (Link-State Routing Demonstration)
     */
    public DijkstraResult findShortestPath(String sourceId, String destId) {
        List<String> logs = new ArrayList<>();
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::distance));
        Set<String> visited = new HashSet<>();

        NetworkDevice srcDev = topology.getDevice(sourceId);
        NetworkDevice dstDev = topology.getDevice(destId);

        if (srcDev == null || dstDev == null) {
            logs.add("Routing Error: Source or Destination device does not exist in topology.");
            return new DijkstraResult(false, Collections.emptyList(), Double.POSITIVE_INFINITY, logs, distances);
        }

        if (!srcDev.isOnline()) {
            logs.add("Routing Failure: Source node '" + sourceId + "' is OFFLINE.");
            return new DijkstraResult(false, Collections.emptyList(), Double.POSITIVE_INFINITY, logs, distances);
        }
        if (!dstDev.isOnline()) {
            logs.add("Routing Failure: Destination node '" + destId + "' is OFFLINE.");
            return new DijkstraResult(false, Collections.emptyList(), Double.POSITIVE_INFINITY, logs, distances);
        }

        for (NetworkDevice d : topology.getDevices()) {
            distances.put(d.getId(), Double.POSITIVE_INFINITY);
        }
        distances.put(sourceId, 0.0);
        pq.add(new NodeDistance(sourceId, 0.0));

        logs.add("Starting Dijkstra Shortest Path from [" + sourceId + "] to [" + destId + "]");

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String u = current.nodeId();

            if (visited.contains(u)) continue;
            visited.add(u);

            if (u.equals(destId)) {
                logs.add("Destination [" + destId + "] reached with optimal cost: " + String.format("%.2f ms", current.distance()));
                break;
            }

            for (NetworkLink link : topology.getConnectedLinks(u)) {
                if (link.getStatus() == LinkStatus.DOWN) {
                    logs.add("Link [" + link.getLinkId() + "] is DOWN (Fault Injected) - Skipping.");
                    continue;
                }

                String v = link.getOtherEnd(u);
                NetworkDevice targetDev = topology.getDevice(v);
                if (targetDev == null || !targetDev.isOnline()) {
                    continue; // Skip offline nodes
                }

                // Metric calculation: link latency + queue penalty
                double congestionFactor = (link.getStatus() == LinkStatus.CONGESTED) ? 3.0 : 1.0;
                double edgeWeight = (link.getLatencyMs() + (1000.0 / Math.max(10.0, link.getBandwidthMbps()))) * congestionFactor;
                double newDist = distances.get(u) + edgeWeight;

                if (newDist < distances.get(v)) {
                    distances.put(v, newDist);
                    predecessors.put(v, u);
                    pq.add(new NodeDistance(v, newDist));
                    logs.add("Relaxed edge (" + u + " -> " + v + ") via link [" + link.getLinkId() + "] new cost: " + String.format("%.2f", newDist));
                }
            }
        }

        if (!predecessors.containsKey(destId) && !sourceId.equals(destId)) {
            logs.add("Dijkstra: No viable route found between " + sourceId + " and " + destId + ". Network partition detected.");
            return new DijkstraResult(false, Collections.emptyList(), Double.POSITIVE_INFINITY, logs, distances);
        }

        // Reconstruct path
        List<String> path = new ArrayList<>();
        String curr = destId;
        while (curr != null) {
            path.add(0, curr);
            curr = predecessors.get(curr);
        }

        return new DijkstraResult(true, path, distances.get(destId), logs, distances);
    }

    /**
     * Bellman-Ford Algorithm (Distance-Vector Routing Demonstration)
     */
    public DistanceVectorResult computeDistanceVector() {
        List<DistanceVectorStep> history = new ArrayList<>();
        List<String> nodeIds = topology.getDevices().stream()
                .filter(NetworkDevice::isOnline)
                .map(NetworkDevice::getId)
                .toList();

        Map<String, Map<String, Double>> matrix = new HashMap<>();

        // Initialize table
        for (String u : nodeIds) {
            matrix.put(u, new HashMap<>());
            for (String v : nodeIds) {
                if (u.equals(v)) {
                    matrix.get(u).put(v, 0.0);
                } else {
                    NetworkLink link = topology.findLinkBetween(u, v);
                    if (link != null && link.getStatus() != LinkStatus.DOWN) {
                        double weight = link.getLatencyMs() + (500.0 / Math.max(10.0, link.getBandwidthMbps()));
                        matrix.get(u).put(v, weight);
                    } else {
                        matrix.get(u).put(v, 9999.0); // Infinity representation
                    }
                }
            }
        }

        // Bellman-Ford Iterations (|V| - 1 iterations)
        int maxIterations = Math.max(1, nodeIds.size() - 1);
        int iteration = 0;
        boolean changed = true;

        while (changed && iteration < maxIterations) {
            iteration++;
            changed = false;

            for (String u : nodeIds) {
                for (String v : nodeIds) {
                    if (u.equals(v)) continue;

                    for (String neighbor : nodeIds) {
                        if (neighbor.equals(u)) continue;

                        NetworkLink directLink = topology.findLinkBetween(u, neighbor);
                        if (directLink == null || directLink.getStatus() == LinkStatus.DOWN) continue;

                        double costUtoNeighbor = directLink.getLatencyMs() + (500.0 / Math.max(10.0, directLink.getBandwidthMbps()));
                        double costNeighborToV = matrix.get(neighbor).getOrDefault(v, 9999.0);
                        double candidate = costUtoNeighbor + costNeighborToV;

                        double currentKnown = matrix.get(u).getOrDefault(v, 9999.0);
                        if (candidate < currentKnown - 0.01) {
                            matrix.get(u).put(v, candidate);
                            changed = true;
                            history.add(new DistanceVectorStep(iteration, u, v, candidate, neighbor));
                        }
                    }
                }
            }
        }

        return new DistanceVectorResult(!changed || iteration <= maxIterations, iteration, history, matrix);
    }

    public RoutingTable buildRoutingTable(String deviceId) {
        RoutingTable table = new RoutingTable(deviceId);
        NetworkDevice src = topology.getDevice(deviceId);
        if (src == null || !src.isOnline()) return table;

        for (NetworkDevice dst : topology.getDevices()) {
            if (dst.getId().equals(deviceId) || !dst.isOnline()) continue;
            DijkstraResult res = findShortestPath(deviceId, dst.getId());
            if (res.pathFound() && res.path().size() >= 2) {
                String nextHop = res.path().get(1);
                NetworkLink link = topology.findLinkBetween(deviceId, nextHop);
                String iface = (link != null) ? link.getMediumType() : "eth0";
                table.addEntry(new RoutingEntry(
                        dst.getId(), dst.getIpv4(), nextHop, res.totalCost(), iface, "OSPF Link-State"
                ));
            }
        }
        return table;
    }

    private record NodeDistance(String nodeId, double distance) {}
}
