package com.recommendation.core.graph;

import java.util.*;

/**
 * Graph node representing either a User or Product in bipartite graph
 */
public class Node {
    public enum NodeType {
        USER, PRODUCT
    }

    private int id;
    private NodeType type;
    private Map<String, Object> properties;
    private Set<Integer> neighbors;
    private double weight;

    public Node(int id, NodeType type) {
        this.id = id;
        this.type = type;
        this.properties = new HashMap<>();
        this.neighbors = new HashSet<>();
        this.weight = 1.0;
    }

    public Node(int id, NodeType type, double weight) {
        this(id, type);
        this.weight = weight;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }

    public Set<Integer> getNeighbors() { return neighbors; }
    public void setNeighbors(Set<Integer> neighbors) { this.neighbors = neighbors; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    // Utility methods
    public void addNeighbor(int neighborId) {
        neighbors.add(neighborId);
    }

    public void removeNeighbor(int neighborId) {
        neighbors.remove(neighborId);
    }

    public boolean hasNeighbor(int neighborId) {
        return neighbors.contains(neighborId);
    }

    public int getDegree() {
        return neighbors.size();
    }

    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return id == node.id && type == node.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", type=" + type +
                ", degree=" + getDegree() +
                ", weight=" + weight +
                '}';
    }
}