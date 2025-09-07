package com.recommendation.core.algorithms;

import com.recommendation.models.*;
import com.recommendation.core.graph.UserItemGraph;
import com.recommendation.core.hashtable.CustomHashMap;
import com.recommendation.core.heap.RecommendationHeap;
import java.util.*;

/**
 * Hybrid Recommendation System combining multiple algorithms
 * Implements weighted combination and cascade hybrid approaches
 */
public class HybridRecommender {

    private CollaborativeFiltering collaborativeFilter;
    private ContentBasedFilter contentFilter;
    private UserItemGraph graph;
    private CustomHashMap<Integer, User> userMap;
    private CustomHashMap<Integer, Product> productMap;

    // Default weights for different algorithms
    private double collaborativeWeight = 0.4;
    private double contentWeight = 0.3;
    private double popularityWeight = 0.2;
    private double trendingWeight = 0.1;

    public HybridRecommender(UserItemGraph graph,
                           CustomHashMap<Integer, User> userMap,
                           CustomHashMap<Integer, Product> productMap,
                           List<Interaction> interactions) {
        this.graph = graph;
        this.userMap = userMap;
        this.productMap = productMap;

        this.collaborativeFilter = new CollaborativeFiltering(graph, userMap, productMap, interactions);
        this.contentFilter = new ContentBasedFilter(userMap, productMap, interactions);
    }

    /**
     * Generate hybrid recommendations using weighted combination
     */
    public List<Recommendation> getHybridRecommendations(int userId, int topK) {
        User user = userMap.get(userId);
        if (user == null) {
            return new ArrayList<>();
        }

        // Use recommendation heap to efficiently maintain top-K
        RecommendationHeap recommendationHeap = new RecommendationHeap(topK * 2); // Buffer for merging

        // Get recommendations from different algorithms
        List<Recommendation> collaborativeRecs = getCollaborativeRecommendations(userId, topK);
        List<Recommendation> contentRecs = getContentRecommendations(userId, topK);
        List<Recommendation> popularityRecs = getPopularityRecommendations(userId, topK);
        List<Recommendation> trendingRecs = getTrendingRecommendations(userId, topK);

        // Combine recommendations with weights
        Map<Integer, CombinedRecommendation> combinedScores = new HashMap<>();

        // Process collaborative filtering results
        for (Recommendation rec : collaborativeRecs) {
            CombinedRecommendation combined = combinedScores.computeIfAbsent(
                rec.getProductId(), k -> new CombinedRecommendation(userId, rec.getProductId()));
            combined.addAlgorithmScore("collaborative", rec.getScore(), collaborativeWeight);
        }

        // Process content-based results
        for (Recommendation rec : contentRecs) {
            CombinedRecommendation combined = combinedScores.computeIfAbsent(
                rec.getProductId(), k -> new CombinedRecommendation(userId, rec.getProductId()));
            combined.addAlgorithmScore("content", rec.getScore(), contentWeight);
        }

        // Process popularity results
        for (Recommendation rec : popularityRecs) {
            CombinedRecommendation combined = combinedScores.computeIfAbsent(
                rec.getProductId(), k -> new CombinedRecommendation(userId, rec.getProductId()));
            combined.addAlgorithmScore("popularity", rec.getScore(), popularityWeight);
        }

        // Process trending results
        for (Recommendation rec : trendingRecs) {
            CombinedRecommendation combined = combinedScores.computeIfAbsent(
                rec.getProductId(), k -> new CombinedRecommendation(userId, rec.getProductId()));
            combined.addAlgorithmScore("trending", rec.getScore(), trendingWeight);
        }

        // Convert to final recommendations
        for (CombinedRecommendation combined : combinedScores.values()) {
            Recommendation finalRec = combined.toRecommendation();
            recommendationHeap.addRecommendation(finalRec);
        }

        return recommendationHeap.getTopK();
    }

    /**
     * Get cascade hybrid recommendations (try algorithms in order until enough results)
     */
    public List<Recommendation> getCascadeRecommendations(int userId, int topK) {
        List<Recommendation> results = new ArrayList<>();

        // Try collaborative filtering first (best for users with interaction history)
        List<Recommendation> collaborativeRecs = getCollaborativeRecommendations(userId, topK);
        results.addAll(collaborativeRecs);

        // If not enough recommendations, try content-based
        if (results.size() < topK) {
            List<Recommendation> contentRecs = getContentRecommendations(userId, topK - results.size());
            // Remove duplicates
            Set<Integer> existingProducts = results.stream()
                    .map(Recommendation::getProductId)
                    .collect(HashSet::new, HashSet::add, HashSet::addAll);

            contentRecs.stream()
                    .filter(rec -> !existingProducts.contains(rec.getProductId()))
                    .forEach(results::add);
        }

        // If still not enough, add popular items
        if (results.size() < topK) {
            List<Recommendation> popularityRecs = getPopularityRecommendations(userId, topK - results.size());
            Set<Integer> existingProducts = results.stream()
                    .map(Recommendation::getProductId)
                    .collect(HashSet::new, HashSet::add, HashSet::addAll);

            popularityRecs.stream()
                    .filter(rec -> !existingProducts.contains(rec.getProductId()))
                    .forEach(results::add);
        }

        return results.subList(0, Math.min(topK, results.size()));
    }

    /**
     * Get adaptive recommendations based on user interaction history
     */
    public List<Recommendation> getAdaptiveRecommendations(int userId, int topK) {
        User user = userMap.get(userId);
        if (user == null) {
            return new ArrayList<>();
        }

        int totalInteractions = user.getTotalInteractions();

        // Adjust weights based on user's interaction history
        double adaptiveCollaborativeWeight = collaborativeWeight;
        double adaptiveContentWeight = contentWeight;
        double adaptivePopularityWeight = popularityWeight;

        if (totalInteractions < 5) {
            // New user: rely more on popularity and content
            adaptiveCollaborativeWeight = 0.1;
            adaptiveContentWeight = 0.4;
            adaptivePopularityWeight = 0.4;
            trendingWeight = 0.1;
        } else if (totalInteractions > 50) {
            // Active user: rely more on collaborative filtering
            adaptiveCollaborativeWeight = 0.6;
            adaptiveContentWeight = 0.2;
            adaptivePopularityWeight = 0.1;
            trendingWeight = 0.1;
        }

        // Temporarily update weights
        double originalCollaborativeWeight = this.collaborativeWeight;
        double originalContentWeight = this.contentWeight;
        double originalPopularityWeight = this.popularityWeight;

        this.collaborativeWeight = adaptiveCollaborativeWeight;
        this.contentWeight = adaptiveContentWeight;
        this.popularityWeight = adaptivePopularityWeight;

        // Get recommendations with adaptive weights
        List<Recommendation> recommendations = getHybridRecommendations(userId, topK);

        // Restore original weights
        this.collaborativeWeight = originalCollaborativeWeight;
        this.contentWeight = originalContentWeight;
        this.popularityWeight = originalPopularityWeight;

        return recommendations;
    }

    /**
     * Get diversified recommendations to avoid filter bubbles
     */
    public List<Recommendation> getDiversifiedRecommendations(int userId, int topK) {
        // Get more recommendations than needed
        List<Recommendation> candidates = getHybridRecommendations(userId, topK * 3);

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        List<Recommendation> diversified = new ArrayList<>();
        Set<String> usedCategories = new HashSet<>();
        Set<String> usedBrands = new HashSet<>();

        // Select recommendations to maximize diversity
        for (Recommendation rec : candidates) {
            if (diversified.size() >= topK) break;

            Product product = productMap.get(rec.getProductId());
            if (product == null) continue;

            String category = product.getCategory();
            String brand = product.getBrand();

            // Check diversity criteria
            boolean categoryDiverse = category == null || !usedCategories.contains(category) 
                                    || usedCategories.size() < topK / 3;
            boolean brandDiverse = brand == null || !usedBrands.contains(brand) 
                                 || usedBrands.size() < topK / 2;

            if (categoryDiverse && brandDiverse) {
                diversified.add(rec);
                if (category != null) usedCategories.add(category);
                if (brand != null) usedBrands.add(brand);

                // Update explanation to mention diversity
                rec.setExplanation(rec.getExplanation() + " (diverse selection)");
            }
        }

        // Fill remaining slots with best remaining candidates
        Set<Integer> usedProducts = diversified.stream()
                .map(Recommendation::getProductId)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        for (Recommendation rec : candidates) {
            if (diversified.size() >= topK) break;
            if (!usedProducts.contains(rec.getProductId())) {
                diversified.add(rec);
            }
        }

        return diversified;
    }

    /**
     * Get collaborative filtering recommendations
     */
    private List<Recommendation> getCollaborativeRecommendations(int userId, int count) {
        try {
            List<Recommendation> userBased = collaborativeFilter.getUserBasedRecommendations(userId, count/2, 20);
            List<Recommendation> itemBased = collaborativeFilter.getItemBasedRecommendations(userId, count/2);

            // Merge and deduplicate
            RecommendationHeap heap = new RecommendationHeap(count);
            heap.addRecommendations(userBased);
            heap.addRecommendations(itemBased);

            return heap.getTopK();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Get content-based recommendations
     */
    private List<Recommendation> getContentRecommendations(int userId, int count) {
        try {
            List<Recommendation> contentRecs = contentFilter.getContentBasedRecommendations(userId, count/2);
            List<Recommendation> similarRecs = contentFilter.getSimilarProducts(userId, count/2);

            RecommendationHeap heap = new RecommendationHeap(count);
            heap.addRecommendations(contentRecs);
            heap.addRecommendations(similarRecs);

            return heap.getTopK();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Get popularity-based recommendations
     */
    private List<Recommendation> getPopularityRecommendations(int userId, int count) {
        User user = userMap.get(userId);
        if (user == null) {
            return new ArrayList<>();
        }

        Set<Integer> userProducts = new HashSet<>();
        userProducts.addAll(user.getViewedProducts());
        userProducts.addAll(user.getPurchasedProducts());

        List<Integer> popularProducts = graph.getMostPopularProducts(count * 2);
        List<Recommendation> recommendations = new ArrayList<>();

        for (int productId : popularProducts) {
            if (userProducts.contains(productId)) continue;

            Product product = productMap.get(productId);
            if (product == null) continue;

            double popularityScore = product.getPopularityScore();
            double normalizedScore = Math.min(1.0, popularityScore / 1000.0); // Normalize

            Recommendation rec = new Recommendation(userId, productId, normalizedScore, "popularity");
            rec.addScoreComponent("popularity", normalizedScore);
            rec.setExplanation("Popular among all users");

            recommendations.add(rec);

            if (recommendations.size() >= count) break;
        }

        return recommendations;
    }

    /**
     * Get trending recommendations
     */
    private List<Recommendation> getTrendingRecommendations(int userId, int count) {
        return contentFilter.getTrendingInPreferences(userId, count);
    }

    /**
     * Set algorithm weights
     */
    public void setWeights(double collaborative, double content, double popularity, double trending) {
        double total = collaborative + content + popularity + trending;
        if (total <= 0) {
            throw new IllegalArgumentException("Total weight must be positive");
        }

        this.collaborativeWeight = collaborative / total;
        this.contentWeight = content / total;
        this.popularityWeight = popularity / total;
        this.trendingWeight = trending / total;
    }

    /**
     * Get current weights
     */
    public Map<String, Double> getWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("collaborative", collaborativeWeight);
        weights.put("content", contentWeight);
        weights.put("popularity", popularityWeight);
        weights.put("trending", trendingWeight);
        return weights;
    }

    /**
     * Inner class for combining multiple algorithm scores
     */
    private static class CombinedRecommendation {
        private int userId;
        private int productId;
        private Map<String, Double> algorithmScores = new HashMap<>();
        private Map<String, Double> weights = new HashMap<>();
        private double totalWeightedScore = 0.0;
        private double totalWeight = 0.0;

        public CombinedRecommendation(int userId, int productId) {
            this.userId = userId;
            this.productId = productId;
        }

        public void addAlgorithmScore(String algorithm, double score, double weight) {
            algorithmScores.put(algorithm, score);
            weights.put(algorithm, weight);
            totalWeightedScore += score * weight;
            totalWeight += weight;
        }

        public Recommendation toRecommendation() {
            double finalScore = totalWeight > 0 ? totalWeightedScore / totalWeight : 0.0;

            Recommendation rec = new Recommendation(userId, productId, finalScore, "hybrid");

            // Add score components
            for (Map.Entry<String, Double> entry : algorithmScores.entrySet()) {
                rec.addScoreComponent(entry.getKey(), entry.getValue());
            }

            // Generate explanation
            List<String> algorithms = new ArrayList<>(algorithmScores.keySet());
            String explanation = "Recommended by " + String.join(", ", algorithms) + " algorithms";
            rec.setExplanation(explanation);

            return rec;
        }
    }
}
