package com.recommendation.core.graph;

import java.util.*;

/**
 * Graph traversal algorithms for recommendation system
 * Implements BFS, DFS, and path finding algorithms
 */
public class GraphTraversal {

    /**
     * Breadth-First Search to find shortest path between user and product
     */
    public static List<Integer> findShortestPath(UserItemGraph graph, int startUser, int targetProduct) {
        if (!graph.hasInteraction(startUser, targetProduct)) {
            return new ArrayList<>(); // No direct connection
        }

        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();

        queue.offer(startUser);
        visited.add(startUser);
        parent.put(startUser, -1);

        while (!queue.isEmpty()) {
            int current = queue.poll();

            if (current == targetProduct) {
                // Reconstruct path
                return reconstructPath(parent, startUser, targetProduct);
            }

            Set<Integer> neighbors = graph.getUserInteractions(current);
            for (int neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }

        return new ArrayList<>(); // No path found
    }

    private static List<Integer> reconstructPath(Map<Integer, Integer> parent, int start, int end) {
        List<Integer> path = new ArrayList<>();
        int current = end;

        while (current != -1) {
            path.add(current);
            current = parent.get(current);
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * Find k-hop neighbors of a user using BFS
     */
    public static Set<Integer> findKHopNeighbors(UserItemGraph graph, int userId, int k) {
        Set<Integer> neighbors = new HashSet<>();
        if (k <= 0) return neighbors;

        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> distance = new HashMap<>();
        Set<Integer> visited = new HashSet<>();

        queue.offer(userId);
        distance.put(userId, 0);
        visited.add(userId);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            int currentDistance = distance.get(current);

            if (currentDistance >= k) continue;

            Set<Integer> currentNeighbors = graph.getUserInteractions(current);
            for (int neighbor : currentNeighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    distance.put(neighbor, currentDistance + 1);
                    queue.offer(neighbor);

                    if (currentDistance + 1 <= k && neighbor != userId) {
                        neighbors.add(neighbor);
                    }
                }
            }
        }

        return neighbors;
    }

    /**
     * Find connected components in the graph
     */
    public static List<Set<Integer>> findConnectedComponents(UserItemGraph graph) {
        List<Set<Integer>> components = new ArrayList<>();
        Set<Integer> globalVisited = new HashSet<>();

        for (int userId : graph.getAllUsers()) {
            if (!globalVisited.contains(userId)) {
                Set<Integer> component = new HashSet<>();
                dfsComponent(graph, userId, globalVisited, component);
                components.add(component);
            }
        }

        return components;
    }

    private static void dfsComponent(UserItemGraph graph, int userId, 
                                   Set<Integer> globalVisited, Set<Integer> component) {
        globalVisited.add(userId);
        component.add(userId);

        Set<Integer> userProducts = graph.getUserInteractions(userId);
        for (int productId : userProducts) {
            Set<Integer> productUsers = graph.getProductInteractions(productId);
            for (int otherUser : productUsers) {
                if (!globalVisited.contains(otherUser)) {
                    dfsComponent(graph, otherUser, globalVisited, component);
                }
            }
        }
    }

    /**
     * Calculate clustering coefficient for a user
     */
    public static double calculateClusteringCoefficient(UserItemGraph graph, int userId) {
        Set<Integer> userProducts = graph.getUserInteractions(userId);
        if (userProducts.size() < 2) {
            return 0.0;
        }

        // Find all users who interacted with same products
        Set<Integer> neighbors = new HashSet<>();
        for (int productId : userProducts) {
            neighbors.addAll(graph.getProductInteractions(productId));
        }
        neighbors.remove(userId);

        if (neighbors.size() < 2) {
            return 0.0;
        }

        // Count triangles (connections between neighbors)
        int triangles = 0;
        List<Integer> neighborList = new ArrayList<>(neighbors);

        for (int i = 0; i < neighborList.size(); i++) {
            for (int j = i + 1; j < neighborList.size(); j++) {
                int user1 = neighborList.get(i);
                int user2 = neighborList.get(j);

                // Check if user1 and user2 share any products
                Set<Integer> user1Products = graph.getUserInteractions(user1);
                Set<Integer> user2Products = graph.getUserInteractions(user2);

                Set<Integer> sharedProducts = new HashSet<>(user1Products);
                sharedProducts.retainAll(user2Products);

                if (!sharedProducts.isEmpty()) {
                    triangles++;
                }
            }
        }

        int possibleTriangles = neighbors.size() * (neighbors.size() - 1) / 2;
        return possibleTriangles > 0 ? (double) triangles / possibleTriangles : 0.0;
    }

    /**
     * Find influential users based on centrality measures
     */
    public static List<Integer> findInfluentialUsers(UserItemGraph graph, int topK) {
        Map<Integer, Double> centralityScores = new HashMap<>();

        for (int userId : graph.getAllUsers()) {
            double degree = graph.getUserDegree(userId);
            double clustering = calculateClusteringCoefficient(graph, userId);

            // Combine degree centrality and clustering coefficient
            double centralityScore = degree * (1 + clustering);
            centralityScores.put(userId, centralityScore);
        }

        return centralityScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Random walk algorithm for discovering related products
     */
    public static Map<Integer, Double> randomWalk(UserItemGraph graph, int startUser, 
                                                 int walkLength, int numWalks) {
        Map<Integer, Double> productVisits = new HashMap<>();
        Random random = new Random();

        for (int walk = 0; walk < numWalks; walk++) {
            int currentUser = startUser;

            for (int step = 0; step < walkLength; step++) {
                Set<Integer> userProducts = graph.getUserInteractions(currentUser);
                if (userProducts.isEmpty()) break;

                // Randomly select a product
                List<Integer> productList = new ArrayList<>(userProducts);
                int randomProduct = productList.get(random.nextInt(productList.size()));

                // Update visit count
                productVisits.put(randomProduct, 
                    productVisits.getOrDefault(randomProduct, 0.0) + 1.0);

                // Move to a random user who interacted with this product
                Set<Integer> productUsers = graph.getProductInteractions(randomProduct);
                if (productUsers.isEmpty()) break;

                List<Integer> userList = new ArrayList<>(productUsers);
                currentUser = userList.get(random.nextInt(userList.size()));
            }
        }

        // Normalize scores
        double totalVisits = productVisits.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalVisits > 0) {
            productVisits.replaceAll((k, v) -> v / totalVisits);
        }

        return productVisits;
    }
}