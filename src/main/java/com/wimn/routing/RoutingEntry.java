package com.wimn.routing;

public class RoutingEntry {
    private String destinationNode;
    private String destinationSubnet;
    private String nextHopNode;
    private double costMetric;
    private String outInterface;
    private String routingProtocol; // "OSPF (Link-State)", "RIP (Distance-Vector)", "DIRECT", "STATIC"

    public RoutingEntry(String destinationNode, String destinationSubnet, String nextHopNode,
                        double costMetric, String outInterface, String routingProtocol) {
        this.destinationNode = destinationNode;
        this.destinationSubnet = destinationSubnet;
        this.nextHopNode = nextHopNode;
        this.costMetric = costMetric;
        this.outInterface = outInterface;
        this.routingProtocol = routingProtocol;
    }

    public String getDestinationNode() { return destinationNode; }
    public String getDestinationSubnet() { return destinationSubnet; }
    public String getNextHopNode() { return nextHopNode; }
    public double getCostMetric() { return costMetric; }
    public String getOutInterface() { return outInterface; }
    public String getRoutingProtocol() { return routingProtocol; }

    @Override
    public String toString() {
        return destinationNode + " via " + nextHopNode + " [cost: " + String.format("%.2f", costMetric) + "] (" + routingProtocol + ")";
    }
}
