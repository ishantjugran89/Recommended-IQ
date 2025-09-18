package com.recommendation.core.graph;

import java.util.*;

/**
 * Bipartite Graph implementation for User-Item relationships
 * Core DSA: Graph Data Structure with Adjacency Lists
 */
public class UserItemGraph {
    private Map<Integer, Node> userNodes;
    private Map<Integer, Node> productNodes;
    private Map<Integer, Map<Integer, Double>> userToProductWeights; // user -> product -> weight
    private Map<Integer, Map<Integer, Double>> productToUserWeights; // product -> user -> weight
    private int totalEdges;

    public UserItemGraph() {
        this.userNodes = new HashMap<>();
        this.productNodes = new HashMap<>();
        this.userToProductWeights = new HashMap<>();
        this.productToUserWeights = new HashMap<>();
        this.totalEdges = 0;
    }

    // Graph construction methods
    public void addUser(int userId) {
        if (!userNodes.containsKey(userId)) {
            userNodes.put(userId, new Node(userId, Node.NodeType.USER));
            userToProductWeights.put(userId, new HashMap<>());
        }
    }

    public void addProduct(int productId) {
        if (!productNodes.containsKey(productId)) {
            productNodes.put(productId, new Node(productId, Node.NodeType.PRODUCT));
            productToUserWeights.put(productId, new HashMap<>());
        }
    }

    public void addInteraction(int userId, int productId, double weight) {
        addUser(userId);
        addProduct(productId);

        // Add bidirectional edges
        userNodes.get(userId).addNeighbor(productId);
        productNodes.get(productId).addNeighbor(userId);

        // Store weights
        userToProductWeights.get(userId).put(productId, weight);
        productToUserWeights.get(productId).put(userId, weight);

        totalEdges++;
    }

    public void updateInteractionWeight(int userId, int productId, double newWeight) {
        if (hasInteraction(userId, productId)) {
            userToProductWeights.get(userId).put(productId, newWeight);
            productToUserWeights.get(productId).put(userId, newWeight);
        }
    }

    // Graph query methods
    public boolean hasInteraction(int userId, int productId) {
        return userNodes.containsKey(userId) && 
               userNodes.get(userId).hasNeighbor(productId);
    }

    public double getInteractionWeight(int userId, int productId) {
        if (!hasInteraction(userId, productId)) {
            return 0.0;
        }
        return userToProductWeights.get(userId).get(productId);
    }

    public Set<Integer> getUserInteractions(int userId) {
        if (!userNodes.containsKey(userId)) {
            return new HashSet<>();
        }
        return new HashSet<>(userNodes.get(userId).getNeighbors());
    }

    public Set<Integer> getProductInteractions(int productId) {
        if (!productNodes.containsKey(productId)) {
            return new HashSet<>();
        }
        return new HashSet<>(productNodes.get(productId).getNeighbors());
    }

    public int getUserDegree(int userId) {
        return userNodes.containsKey(userId) ? userNodes.get(userId).getDegree() : 0;
    }

    public int getProductDegree(int productId) {
        return productNodes.containsKey(productId) ? productNodes.get(productId).getDegree() : 0;
    }

    // BFS algorithm for finding similar users
    public List<Integer> findSimilarUsers(int userId, int maxDepth) {
        if (!userNodes.containsKey(userId)) {
            return new ArrayList<>();
        }

        List<Integer> similarUsers = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> depth = new HashMap<>();

        queue.offer(userId);
        visited.add(userId);
        depth.put(userId, 0);

        while (!queue.isEmpty()) {
            int currentUser = queue.poll();
            int currentDepth = depth.get(currentUser);

            if (currentDepth >= maxDepth) continue;

            // Get all products this user interacted with
            Set<Integer> userProducts = getUserInteractions(currentUser);

            // Find other users who interacted with same products
            for (int productId : userProducts) {
                Set<Integer> productUsers = getProductInteractions(productId);

                for (int otherUser : productUsers) {
                    if (!visited.contains(otherUser) && otherUser != userId) {
                        visited.add(otherUser);
                        queue.offer(otherUser);
                        depth.put(otherUser, currentDepth + 1);
                        similarUsers.add(otherUser);
                    }
                }
            }
        }

        return similarUsers;
    }

    // DFS algorithm for exploring user neighborhoods
    public Set<Integer> exploreUserNeighborhood(int userId) {
        if (!userNodes.containsKey(userId)) {
            return new HashSet<>();
        }

        Set<Integer> neighborhood = new HashSet<>();
        Set<Integer> visited = new HashSet<>();

        dfsExplore(userId, visited, neighborhood);
        neighborhood.remove(userId); // Remove self

        return neighborhood;
    }

    private void dfsExplore(int userId, Set<Integer> visited, Set<Integer> neighborhood) {
        visited.add(userId);
        neighborhood.add(userId);

        // Get all products this user interacted with
        Set<Integer> userProducts = getUserInteractions(userId);

        // Find other users through shared products
        for (int productId : userProducts) {
            Set<Integer> productUsers = getProductInteractions(productId);

            for (int otherUser : productUsers) {
                if (!visited.contains(otherUser)) {
                    dfsExplore(otherUser, visited, neighborhood);
                }
            }
        }
    }

    // Calculate graph-based similarity between two users
    public double calculateGraphSimilarity(int user1, int user2) {
        if (!userNodes.containsKey(user1) || !userNodes.containsKey(user2)) {
            return 0.0;
        }

        Set<Integer> user1Products = getUserInteractions(user1);
        Set<Integer> user2Products = getUserInteractions(user2);

        // Jaccard similarity: intersection / union
        Set<Integer> intersection = new HashSet<>(user1Products);
        intersection.retainAll(user2Products);

        Set<Integer> union = new HashSet<>(user1Products);
        union.addAll(user2Products);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    // Graph statistics
    public int getTotalUsers() {
        return userNodes.size();
    }

    public int getTotalProducts() {
        return productNodes.size();
    }

    public int getTotalEdges() {
        return totalEdges;
    }

    public double getGraphDensity() {
        int maxPossibleEdges = userNodes.size() * productNodes.size();
        return maxPossibleEdges > 0 ? (double) totalEdges / maxPossibleEdges : 0.0;
    }

    public Set<Integer> getAllUsers() {
        return new HashSet<>(userNodes.keySet());
    }

    public Set<Integer> getAllProducts() {
        return new HashSet<>(productNodes.keySet());
    }

    // Graph analysis methods
    public List<Integer> getMostPopularProducts(int topK) {
        return productNodes.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getDegree(), e1.getValue().getDegree()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<Integer> getMostActiveUsers(int topK) {
        return userNodes.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getDegree(), e1.getValue().getDegree()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public String toString() {
        return "UserItemGraph{" +
                "users=" + userNodes.size() +
                ", products=" + productNodes.size() +
                ", edges=" + totalEdges +
                ", density=" + String.format("%.4f", getGraphDensity()) +
                '}';
    }
}
