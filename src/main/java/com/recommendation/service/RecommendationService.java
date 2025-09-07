package com.recommendation.service;

import com.recommendation.models.*;
import com.recommendation.core.graph.UserItemGraph;
import com.recommendation.core.hashtable.CustomHashMap;
import com.recommendation.core.algorithms.*;
import java.util.*;

/**
 * Main service class for generating recommendations
 * Provides high-level API for different recommendation strategies
 */
public class RecommendationService {

    private UserItemGraph graph;
    private CustomHashMap<Integer, User> userMap;
    private CustomHashMap<Integer, Product> productMap;
    private List<Interaction> interactions;

    private CollaborativeFiltering collaborativeFilter;
    private ContentBasedFilter contentFilter;
    private HybridRecommender hybridRecommender;

    public RecommendationService() {
        this.graph = new UserItemGraph();
        this.userMap = new CustomHashMap<>();
        this.productMap = new CustomHashMap<>();
        this.interactions = new ArrayList<>();

        initializeAlgorithms();
    }

    /**
     * Initialize recommendation algorithms
     */
    private void initializeAlgorithms() {
        this.collaborativeFilter = new CollaborativeFiltering(graph, userMap, productMap, interactions);
        this.contentFilter = new ContentBasedFilter(userMap, productMap, interactions);
        this.hybridRecommender = new HybridRecommender(graph, userMap, productMap, interactions);
    }

    /**
     * Add a user to the system
     */
    public void addUser(User user) {
        if (user != null && user.getUserId() > 0) {
            userMap.put(user.getUserId(), user);
            graph.addUser(user.getUserId());
        }
    }

    /**
     * Add a product to the system
     */
    public void addProduct(Product product) {
        if (product != null && product.getProductId() > 0) {
            productMap.put(product.getProductId(), product);
            graph.addProduct(product.getProductId());
        }
    }

    /**
     * Add an interaction to the system
     */
    public void addInteraction(Interaction interaction) {
        if (interaction != null && interaction.getUserId() > 0 && interaction.getProductId() > 0) {
            interactions.add(interaction);

            // Update user object
            User user = userMap.get(interaction.getUserId());
            if (user != null) {
                switch (interaction.getType()) {
                    case VIEW:
                        user.addViewedProduct(interaction.getProductId());
                        break;
                    case PURCHASE:
                        user.addPurchasedProduct(interaction.getProductId());
                        break;
                    case WISHLIST:
                        user.addToWishlist(interaction.getProductId());
                        break;
                }
            }

            // Update product object
            Product product = productMap.get(interaction.getProductId());
            if (product != null) {
                switch (interaction.getType()) {
                    case VIEW:
                        product.incrementViewCount();
                        break;
                    case PURCHASE:
                        product.incrementPurchaseCount();
                        break;
                    case WISHLIST:
                        product.incrementWishlistCount();
                        break;
                }
            }

            // Update graph
            double weight = interaction.getInteractionWeight();
            graph.addInteraction(interaction.getUserId(), interaction.getProductId(), weight);
        }
    }

    /**
     * Get recommendations using the best algorithm for the user
     */
    public List<Recommendation> getRecommendations(int userId, int count) {
        User user = userMap.get(userId);
        if (user == null) {
            return new ArrayList<>();
        }

        // Choose algorithm based on user's interaction history
        int totalInteractions = user.getTotalInteractions();

        if (totalInteractions == 0) {
            // New user: use popularity-based recommendations
            return getPopularRecommendations(count);
        } else if (totalInteractions < 5) {
            // Few interactions: use content-based
            return getContentBasedRecommendations(userId, count);
        } else {
            // Sufficient data: use hybrid approach
            return getHybridRecommendations(userId, count);
        }
    }

    /**
     * Get collaborative filtering recommendations
     */
    public List<Recommendation> getCollaborativeRecommendations(int userId, int count) {
        try {
            List<Recommendation> userBased = collaborativeFilter.getUserBasedRecommendations(userId, count/2, 20);
            List<Recommendation> itemBased = collaborativeFilter.getItemBasedRecommendations(userId, count/2);

            // Merge results
            Map<Integer, Recommendation> merged = new HashMap<>();

            for (Recommendation rec : userBased) {
                merged.put(rec.getProductId(), rec);
            }

            for (Recommendation rec : itemBased) {
                if (merged.containsKey(rec.getProductId())) {
                    // Combine scores for same product
                    Recommendation existing = merged.get(rec.getProductId());
                    double combinedScore = (existing.getScore() + rec.getScore()) / 2;
                    existing.setScore(combinedScore);
                    existing.setAlgorithm("collaborative_combined");
                } else {
                    merged.put(rec.getProductId(), rec);
                }
            }

            List<Recommendation> result = new ArrayList<>(merged.values());
            result.sort(Collections.reverseOrder());

            return result.subList(0, Math.min(count, result.size()));
        } catch (Exception e) {
            System.err.println("Error in collaborative filtering: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get content-based recommendations
     */
    public List<Recommendation> getContentBasedRecommendations(int userId, int count) {
        try {
            return contentFilter.getContentBasedRecommendations(userId, count);
        } catch (Exception e) {
            System.err.println("Error in content-based filtering: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get hybrid recommendations
     */
    public List<Recommendation> getHybridRecommendations(int userId, int count) {
        try {
            return hybridRecommender.getHybridRecommendations(userId, count);
        } catch (Exception e) {
            System.err.println("Error in hybrid recommendations: " + e.getMessage());
            return getContentBasedRecommendations(userId, count); // Fallback
        }
    }

    /**
     * Get diversified recommendations
     */
    public List<Recommendation> getDiversifiedRecommendations(int userId, int count) {
        try {
            return hybridRecommender.getDiversifiedRecommendations(userId, count);
        } catch (Exception e) {
            System.err.println("Error in diversified recommendations: " + e.getMessage());
            return getRecommendations(userId, count); // Fallback
        }
    }

    /**
     * Get popular recommendations for new users
     */
    public List<Recommendation> getPopularRecommendations(int count) {
        List<Integer> popularProducts = graph.getMostPopularProducts(count);
        List<Recommendation> recommendations = new ArrayList<>();

        for (int i = 0; i < Math.min(count, popularProducts.size()); i++) {
            int productId = popularProducts.get(i);
            Product product = productMap.get(productId);

            if (product != null) {
                double popularityScore = product.getPopularityScore();
                double normalizedScore = Math.min(1.0, popularityScore / 1000.0);

                Recommendation rec = new Recommendation(0, productId, normalizedScore, "popularity");
                rec.addScoreComponent("popularity", normalizedScore);
                rec.setExplanation("Popular among all users");
                rec.setRank(i + 1);

                recommendations.add(rec);
            }
        }

        return recommendations;
    }

    /**
     * Get trending recommendations in specific category
     */
    public List<Recommendation> getTrendingInCategory(String category, int count) {
        List<Recommendation> trending = new ArrayList<>();

        for (Integer productId : productMap.keySet()) {
            Product product = productMap.get(productId);
            if (product != null && product.getCategory().equalsIgnoreCase(category)) {
                // Calculate trending score based on recent activity
                double trendingScore = calculateTrendingScore(product);

                if (trendingScore > 0) {
                    Recommendation rec = new Recommendation(0, productId, trendingScore, "trending");
                    rec.addScoreComponent("trending", trendingScore);
                    rec.setExplanation("Trending in " + category);
                    trending.add(rec);
                }
            }
        }

        trending.sort(Collections.reverseOrder());
        return trending.subList(0, Math.min(count, trending.size()));
    }

    /**
     * Calculate trending score for a product
     */
    private double calculateTrendingScore(Product product) {
        // Simple trending score based on view/purchase ratio and recency
        double viewScore = Math.min(1.0, product.getViewCount() / 1000.0);
        double purchaseScore = Math.min(1.0, product.getPurchaseCount() / 100.0);
        double ratingScore = product.getRating() / 5.0;

        return (viewScore * 0.4 + purchaseScore * 0.4 + ratingScore * 0.2);
    }

    /**
     * Get recommendations for similar users
     */
    public List<Recommendation> getSimilarUsersRecommendations(int userId, int count) {
        List<Integer> similarUsers = graph.findSimilarUsers(userId, 2);
        if (similarUsers.isEmpty()) {
            return getPopularRecommendations(count);
        }

        Map<Integer, Double> productScores = new HashMap<>();
        Set<Integer> userProducts = graph.getUserInteractions(userId);

        for (int similarUserId : similarUsers.subList(0, Math.min(10, similarUsers.size()))) {
            double userSimilarity = graph.calculateGraphSimilarity(userId, similarUserId);
            Set<Integer> similarUserProducts = graph.getUserInteractions(similarUserId);

            for (int productId : similarUserProducts) {
                if (!userProducts.contains(productId)) {
                    double interactionWeight = graph.getInteractionWeight(similarUserId, productId);
                    double score = userSimilarity * interactionWeight;
                    productScores.merge(productId, score, Double::sum);
                }
            }
        }

        List<Recommendation> recommendations = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : productScores.entrySet()) {
            int productId = entry.getKey();
            double score = entry.getValue();

            Recommendation rec = new Recommendation(userId, productId, score, "similar_users");
            rec.addScoreComponent("user_similarity", score);
            rec.setExplanation("Liked by users with similar preferences");
            recommendations.add(rec);
        }

        recommendations.sort(Collections.reverseOrder());
        return recommendations.subList(0, Math.min(count, recommendations.size()));
    }

    /**
     * Update recommendation algorithm weights
     */
    public void updateAlgorithmWeights(double collaborative, double content, 
                                     double popularity, double trending) {
        hybridRecommender.setWeights(collaborative, content, popularity, trending);
    }

    /**
     * Get system statistics
     */
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total_users", userMap.size());
        stats.put("total_products", productMap.size());
        stats.put("total_interactions", interactions.size());
        stats.put("graph_density", graph.getGraphDensity());

        // User activity statistics
        int activeUsers = 0;
        for (Integer userId : userMap.keySet()) {
            User user = userMap.get(userId);
            if (user != null && user.getTotalInteractions() > 0) {
                activeUsers++;
            }
        }
        stats.put("active_users", activeUsers);

        // Product popularity statistics
        List<Integer> popularProducts = graph.getMostPopularProducts(10);
        stats.put("top_popular_products", popularProducts);

        return stats;
    }

    /**
     * Clear all caches to free memory
     */
    public void clearCaches() {
        if (collaborativeFilter != null) {
            collaborativeFilter.clearCaches();
        }
        if (contentFilter != null) {
            contentFilter.clearCache();
        }
    }

    /**
     * Get cache statistics
     */
    public String getCacheStatistics() {
        StringBuilder stats = new StringBuilder("Cache Statistics:\n");

        if (collaborativeFilter != null) {
            stats.append(collaborativeFilter.getCacheStatistics()).append("\n");
        }
        if (contentFilter != null) {
            stats.append(contentFilter.getCacheStatistics()).append("\n");
        }

        stats.append(userMap.getStatistics()).append("\n");
        stats.append(productMap.getStatistics());

        return stats.toString();
    }

    // Getters
    public UserItemGraph getGraph() { return graph; }
    public CustomHashMap<Integer, User> getUserMap() { return userMap; }
    public CustomHashMap<Integer, Product> getProductMap() { return productMap; }
    public List<Interaction> getInteractions() { return interactions; }

    // Bulk operations
    public void addUsers(List<User> users) {
        users.forEach(this::addUser);
    }

    public void addProducts(List<Product> products) {
        products.forEach(this::addProduct);
    }

    public void addInteractions(List<Interaction> interactions) {
        interactions.forEach(this::addInteraction);
    }
}
