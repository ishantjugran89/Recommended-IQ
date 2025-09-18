package com.recommendation.core.algorithms;

import com.recommendation.models.*;
import com.recommendation.core.graph.UserItemGraph;
import com.recommendation.core.hashtable.CustomHashMap;
import java.util.*;

/**
 * Collaborative Filtering implementation using graph algorithms and similarity measures
 * Implements both User-based and Item-based collaborative filtering
 */
public class CollaborativeFiltering {

    private UserItemGraph graph;
    private CustomHashMap<Integer, User> userMap;
    private CustomHashMap<Integer, Product> productMap;
    private List<Interaction> interactions;
    private Map<Integer, Map<Integer, Double>> userSimilarityCache;
    private Map<Integer, Map<Integer, Double>> itemSimilarityCache;

    public CollaborativeFiltering(UserItemGraph graph, 
                                CustomHashMap<Integer, User> userMap,
                                CustomHashMap<Integer, Product> productMap,
                                List<Interaction> interactions) {
        this.graph = graph;
        this.userMap = userMap;
        this.productMap = productMap;
        this.interactions = interactions;
        this.userSimilarityCache = new HashMap<>();
        this.itemSimilarityCache = new HashMap<>();
    }

    /**
     * Generate user-based collaborative filtering recommendations
     * Algorithm: Find similar users, recommend items they liked
     */
    public List<Recommendation> getUserBasedRecommendations(int userId, int topK, int similarUserCount) {
        User targetUser = userMap.get(userId);
        if (targetUser == null) {
            return new ArrayList<>();
        }

        // Create user interaction vectors
        Map<Integer, Map<Integer, Double>> userVectors = createUserInteractionVectors();

        // Find similar users using graph traversal and similarity calculation
        List<Integer> similarUsers = findSimilarUsersOptimized(userId, similarUserCount);

        // Collect recommendations from similar users
        Map<Integer, Double> candidateScores = new HashMap<>();
        Set<Integer> userProducts = graph.getUserInteractions(userId);

        for (int similarUserId : similarUsers) {
            double userSimilarity = getUserSimilarity(userId, similarUserId, userVectors);
            if (userSimilarity <= 0) continue;

            Set<Integer> similarUserProducts = graph.getUserInteractions(similarUserId);

            for (int productId : similarUserProducts) {
                // Skip products user has already interacted with
                if (userProducts.contains(productId)) continue;

                double interactionWeight = graph.getInteractionWeight(similarUserId, productId);
                double score = userSimilarity * interactionWeight;

                candidateScores.merge(productId, score, Double::sum);
            }
        }

        // Convert to recommendations and sort
        List<Recommendation> recommendations = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : candidateScores.entrySet()) {
            int productId = entry.getKey();
            double score = entry.getValue();

            Recommendation rec = new Recommendation(userId, productId, score, "user_collaborative");
            rec.addScoreComponent("user_similarity", score);
            rec.setExplanation("Users with similar preferences also liked this product");

            recommendations.add(rec);
        }

        // Sort and return top K
        recommendations.sort(Collections.reverseOrder());
        return recommendations.subList(0, Math.min(topK, recommendations.size()));
    }

    /**
     * Generate item-based collaborative filtering recommendations
     * Algorithm: Find similar items to user's history, recommend most similar
     */
    public List<Recommendation> getItemBasedRecommendations(int userId, int topK) {
        User targetUser = userMap.get(userId);
        if (targetUser == null) {
            return new ArrayList<>();
        }

        Set<Integer> userProducts = graph.getUserInteractions(userId);
        if (userProducts.isEmpty()) {
            return new ArrayList<>();
        }

        // Create item interaction vectors
        Map<Integer, Map<Integer, Double>> itemVectors = createItemInteractionVectors();

        // For each product user hasn't interacted with, calculate similarity score
        Map<Integer, Double> candidateScores = new HashMap<>();
        Set<Integer> allProducts = graph.getAllProducts();

        for (int candidateProduct : allProducts) {
            if (userProducts.contains(candidateProduct)) continue;

            double totalScore = 0.0;
            double totalSimilarity = 0.0;

            for (int userProduct : userProducts) {
                double itemSimilarity = getItemSimilarity(userProduct, candidateProduct, itemVectors);
                if (itemSimilarity > 0) {
                    double userRating = graph.getInteractionWeight(userId, userProduct);
                    totalScore += itemSimilarity * userRating;
                    totalSimilarity += itemSimilarity;
                }
            }

            if (totalSimilarity > 0) {
                candidateScores.put(candidateProduct, totalScore / totalSimilarity);
            }
        }

        // Convert to recommendations
        List<Recommendation> recommendations = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : candidateScores.entrySet()) {
            int productId = entry.getKey();
            double score = entry.getValue();

            Recommendation rec = new Recommendation(userId, productId, score, "item_collaborative");
            rec.addScoreComponent("item_similarity", score);
            rec.setExplanation("Similar to products you've previously liked");

            recommendations.add(rec);
        }

        // Sort and return top K
        recommendations.sort(Collections.reverseOrder());
        return recommendations.subList(0, Math.min(topK, recommendations.size()));
    }

    /**
     * Find similar users using graph traversal and similarity caching
     */
    private List<Integer> findSimilarUsersOptimized(int userId, int count) {
        // First, try graph-based approach to get candidates
        List<Integer> graphSimilarUsers = graph.findSimilarUsers(userId, 2);

        // If not enough from graph, expand search
        if (graphSimilarUsers.size() < count) {
            Set<Integer> expanded = graph.exploreUserNeighborhood(userId);
            graphSimilarUsers.addAll(expanded);
        }

        // Calculate similarities for candidates
        Map<Integer, Map<Integer, Double>> userVectors = createUserInteractionVectors();
        List<Map.Entry<Integer, Double>> similarities = new ArrayList<>();

        for (int candidateUser : graphSimilarUsers) {
            if (candidateUser != userId) {
                double similarity = getUserSimilarity(userId, candidateUser, userVectors);
                if (similarity > 0) {
                    similarities.add(new AbstractMap.SimpleEntry<>(candidateUser, similarity));
                }
            }
        }

        // Sort by similarity and return top candidates
        similarities.sort(Map.Entry.<Integer, Double>comparingByValue().reversed());
        return similarities.stream()
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Calculate or retrieve cached user similarity
     */
    private double getUserSimilarity(int user1, int user2, Map<Integer, Map<Integer, Double>> userVectors) {
        // Check cache first
        if (userSimilarityCache.containsKey(user1) && 
            userSimilarityCache.get(user1).containsKey(user2)) {
            return userSimilarityCache.get(user1).get(user2);
        }

        // Calculate similarity
        Map<Integer, Double> vector1 = userVectors.get(user1);
        Map<Integer, Double> vector2 = userVectors.get(user2);

        double similarity = 0.0;
        if (vector1 != null && vector2 != null) {
            similarity = SimilarityCalculator.cosineSimilarity(vector1, vector2);
        }

        // Cache the result (bidirectional)
        userSimilarityCache.computeIfAbsent(user1, k -> new HashMap<>()).put(user2, similarity);
        userSimilarityCache.computeIfAbsent(user2, k -> new HashMap<>()).put(user1, similarity);

        return similarity;
    }

    /**
     * Calculate or retrieve cached item similarity
     */
    private double getItemSimilarity(int item1, int item2, Map<Integer, Map<Integer, Double>> itemVectors) {
        // Check cache first
        if (itemSimilarityCache.containsKey(item1) && 
            itemSimilarityCache.get(item1).containsKey(item2)) {
            return itemSimilarityCache.get(item1).get(item2);
        }

        // Calculate similarity
        Map<Integer, Double> vector1 = itemVectors.get(item1);
        Map<Integer, Double> vector2 = itemVectors.get(item2);

        double similarity = 0.0;
        if (vector1 != null && vector2 != null) {
            similarity = SimilarityCalculator.cosineSimilarity(vector1, vector2);
        }

        // Cache the result (bidirectional)
        itemSimilarityCache.computeIfAbsent(item1, k -> new HashMap<>()).put(item2, similarity);
        itemSimilarityCache.computeIfAbsent(item2, k -> new HashMap<>()).put(item1, similarity);

        return similarity;
    }

    /**
     * Create user interaction vectors for similarity calculation
     */
    private Map<Integer, Map<Integer, Double>> createUserInteractionVectors() {
        Map<Integer, Map<Integer, Double>> userVectors = new HashMap<>();

        for (int userId : graph.getAllUsers()) {
            User user = userMap.get(userId);
            if (user != null) {
                userVectors.put(userId, SimilarityCalculator.createUserInteractionVector(user, interactions));
            }
        }

        return userVectors;
    }

    /**
     * Create item interaction vectors for similarity calculation
     */
    private Map<Integer, Map<Integer, Double>> createItemInteractionVectors() {
        Map<Integer, Map<Integer, Double>> itemVectors = new HashMap<>();

        for (int productId : graph.getAllProducts()) {
            itemVectors.put(productId, SimilarityCalculator.createItemInteractionVector(productId, interactions));
        }

        return itemVectors;
    }

    /**
     * Matrix factorization approach for collaborative filtering
     * Simplified SVD-like approach
     */
    public List<Recommendation> getMatrixFactorizationRecommendations(int userId, int topK, 
                                                                     int factors, int iterations) {
        // Get user-item matrix
        Map<Integer, Map<Integer, Double>> userItemMatrix = createUserItemMatrix();

        // Initialize factor matrices (simplified approach)
        Map<Integer, double[]> userFactors = new HashMap<>();
        Map<Integer, double[]> itemFactors = new HashMap<>();

        Random random = new Random(42); // Fixed seed for reproducibility

        // Initialize factors randomly
        for (int uid : graph.getAllUsers()) {
            double[] factors_u = new double[factors];
            for (int f = 0; f < factors; f++) {
                factors_u[f] = random.nextGaussian() * 0.1;
            }
            userFactors.put(uid, factors_u);
        }

        for (int pid : graph.getAllProducts()) {
            double[] factors_p = new double[factors];
            for (int f = 0; f < factors; f++) {
                factors_p[f] = random.nextGaussian() * 0.1;
            }
            itemFactors.put(pid, factors_p);
        }

        // Simple gradient descent (this is a simplified version)
        double learningRate = 0.01;
        double regularization = 0.01;

        for (int iter = 0; iter < iterations; iter++) {
            for (Interaction interaction : interactions) {
                int uid = interaction.getUserId();
                int pid = interaction.getProductId();
                double rating = interaction.getInteractionWeight();

                double[] u_factors = userFactors.get(uid);
                double[] p_factors = itemFactors.get(pid);

                if (u_factors != null && p_factors != null) {
                    // Predict rating
                    double predicted = 0.0;
                    for (int f = 0; f < factors; f++) {
                        predicted += u_factors[f] * p_factors[f];
                    }

                    double error = rating - predicted;

                    // Update factors
                    for (int f = 0; f < factors; f++) {
                        double u_f = u_factors[f];
                        double p_f = p_factors[f];

                        u_factors[f] += learningRate * (error * p_f - regularization * u_f);
                        p_factors[f] += learningRate * (error * u_f - regularization * p_f);
                    }
                }
            }
        }

        // Generate recommendations for target user
        double[] targetUserFactors = userFactors.get(userId);
        if (targetUserFactors == null) {
            return new ArrayList<>();
        }

        List<Recommendation> recommendations = new ArrayList<>();
        Set<Integer> userProducts = graph.getUserInteractions(userId);

        for (int productId : graph.getAllProducts()) {
            if (userProducts.contains(productId)) continue;

            double[] productFactors = itemFactors.get(productId);
            if (productFactors != null) {
                double score = 0.0;
                for (int f = 0; f < factors; f++) {
                    score += targetUserFactors[f] * productFactors[f];
                }

                if (score > 0) {
                    Recommendation rec = new Recommendation(userId, productId, score, "matrix_factorization");
                    rec.addScoreComponent("latent_factors", score);
                    rec.setExplanation("Based on latent factor analysis of user preferences");
                    recommendations.add(rec);
                }
            }
        }

        recommendations.sort(Collections.reverseOrder());
        return recommendations.subList(0, Math.min(topK, recommendations.size()));
    }

    /**
     * Create user-item rating matrix
     */
    private Map<Integer, Map<Integer, Double>> createUserItemMatrix() {
        Map<Integer, Map<Integer, Double>> matrix = new HashMap<>();

        for (Interaction interaction : interactions) {
            int userId = interaction.getUserId();
            int productId = interaction.getProductId();
            double weight = interaction.getInteractionWeight();

            matrix.computeIfAbsent(userId, k -> new HashMap<>())
                  .merge(productId, weight, Double::sum);
        }

        return matrix;
    }

    /**
     * Clear similarity caches to free memory
     */
    public void clearCaches() {
        userSimilarityCache.clear();
        itemSimilarityCache.clear();
    }

    /**
     * Get cache statistics
     */
    public String getCacheStatistics() {
        int userCacheSize = userSimilarityCache.values().stream()
                .mapToInt(Map::size)
                .sum();
        int itemCacheSize = itemSimilarityCache.values().stream()
                .mapToInt(Map::size)
                .sum();

        return "CollaborativeFiltering Cache Stats: " +
               "UserSimilarities=" + userCacheSize +
               ", ItemSimilarities=" + itemCacheSize;
    }
}
