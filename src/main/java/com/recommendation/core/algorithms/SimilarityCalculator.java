package com.recommendation.core.algorithms;

import com.recommendation.models.User;
import com.recommendation.models.Product;
import com.recommendation.models.Interaction;
import java.util.*;

/**
 * Similarity calculation algorithms for recommendation systems
 * Implements Cosine, Jaccard, and Pearson correlation similarities
 */
public class SimilarityCalculator {

    /**
     * Calculate Cosine similarity between two users based on their interactions
     * Time Complexity: O(min(|A|, |B|)) where A, B are user interaction sets
     */
    public static double cosineSimilarity(Map<Integer, Double> userA, Map<Integer, Double> userB) {
        if (userA.isEmpty() || userB.isEmpty()) {
            return 0.0;
        }

        // Find common items
        Set<Integer> commonItems = new HashSet<>(userA.keySet());
        commonItems.retainAll(userB.keySet());

        if (commonItems.isEmpty()) {
            return 0.0;
        }

        // Calculate dot product and magnitudes
        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int item : commonItems) {
            double ratingA = userA.get(item);
            double ratingB = userB.get(item);

            dotProduct += ratingA * ratingB;
        }

        // Calculate full magnitudes
        for (double rating : userA.values()) {
            magnitudeA += rating * rating;
        }

        for (double rating : userB.values()) {
            magnitudeB += rating * rating;
        }

        magnitudeA = Math.sqrt(magnitudeA);
        magnitudeB = Math.sqrt(magnitudeB);

        if (magnitudeA == 0.0 || magnitudeB == 0.0) {
            return 0.0;
        }

        return dotProduct / (magnitudeA * magnitudeB);
    }

    /**
         * Calculate Jaccard similarity between two sets
         * Jaccard = |intersection| / |union|
         */
        public static double jaccardSimilarity(Set<?> setA, Set<?> setB) {
            if (setA.isEmpty() && setB.isEmpty()) {
                return 1.0;
            }
    
            if (setA.isEmpty() || setB.isEmpty()) {
                return 0.0;
            }
    
            Set<Object> intersection = new HashSet<>(setA);
            intersection.retainAll(setB);
    
            Set<Object> union = new HashSet<>(setA);
            union.addAll(setB);
    
            return (double) intersection.size() / union.size();
        }

    /**
     * Calculate Pearson correlation coefficient between two users
     */
    public static double pearsonCorrelation(Map<Integer, Double> userA, Map<Integer, Double> userB) {
        Set<Integer> commonItems = new HashSet<>(userA.keySet());
        commonItems.retainAll(userB.keySet());

        if (commonItems.size() < 2) {
            return 0.0;
        }

        // Calculate means for common items
        double sumA = 0.0, sumB = 0.0;
        for (int item : commonItems) {
            sumA += userA.get(item);
            sumB += userB.get(item);
        }

        double meanA = sumA / commonItems.size();
        double meanB = sumB / commonItems.size();

        // Calculate Pearson correlation
        double numerator = 0.0;
        double sumSqA = 0.0, sumSqB = 0.0;

        for (int item : commonItems) {
            double diffA = userA.get(item) - meanA;
            double diffB = userB.get(item) - meanB;

            numerator += diffA * diffB;
            sumSqA += diffA * diffA;
            sumSqB += diffB * diffB;
        }

        double denominator = Math.sqrt(sumSqA * sumSqB);
        if (denominator == 0.0) {
            return 0.0;
        }

        return numerator / denominator;
    }

    /**
     * Calculate item-item similarity using cosine similarity
     */
    public static double itemCosineSimilarity(Map<Integer, Double> itemA, Map<Integer, Double> itemB) {
        return cosineSimilarity(itemA, itemB);
    }

    /**
     * Calculate content-based similarity between two products
     */
    public static double contentSimilarity(Product productA, Product productB) {
        double similarity = 0.0;
        double totalWeight = 0.0;

        // Category similarity (weight: 0.4)
        double categoryWeight = 0.4;
        if (productA.getCategory().equalsIgnoreCase(productB.getCategory())) {
            similarity += categoryWeight * 1.0;
        }
        totalWeight += categoryWeight;

        // Brand similarity (weight: 0.3)
        double brandWeight = 0.3;
        if (productA.getBrand().equalsIgnoreCase(productB.getBrand())) {
            similarity += brandWeight * 1.0;
        }
        totalWeight += brandWeight;

        // Price similarity (weight: 0.2)
        double priceWeight = 0.2;
        double priceA = productA.getPrice();
        double priceB = productB.getPrice();
        if (priceA > 0 && priceB > 0) {
            double priceDiff = Math.abs(priceA - priceB);
            double avgPrice = (priceA + priceB) / 2;
            double priceSimilarity = Math.max(0, 1 - (priceDiff / avgPrice));
            similarity += priceWeight * priceSimilarity;
        }
        totalWeight += priceWeight;

        // Tag similarity (weight: 0.1)
        double tagWeight = 0.1;
        Set<String> tagsA = productA.getTags();
        Set<String> tagsB = productB.getTags();
        if (!tagsA.isEmpty() && !tagsB.isEmpty()) {
            double tagSimilarity = jaccardSimilarity(tagsA, tagsB);
            similarity += tagWeight * tagSimilarity;
        }
        totalWeight += tagWeight;

        return totalWeight > 0 ? similarity / totalWeight : 0.0;
    }

    /**
     * Calculate similarity between user preferences and product features
     */
    public static double userProductSimilarity(User user, Product product) {
        double similarity = 0.0;
        double totalWeight = 0.0;

        // Category preference matching (weight: 0.6)
        double categoryWeight = 0.6;
        Map<String, Double> categoryPrefs = user.getCategoryPreferences();
        if (categoryPrefs.containsKey(product.getCategory())) {
            double preference = categoryPrefs.get(product.getCategory());
            similarity += categoryWeight * preference;
        }
        totalWeight += categoryWeight;

        // Rating compatibility (weight: 0.2)
        double ratingWeight = 0.2;
        if (product.getRating() > 0) {
            double ratingScore = Math.min(1.0, product.getRating() / 5.0);
            similarity += ratingWeight * ratingScore;
        }
        totalWeight += ratingWeight;

        // Price preference (weight: 0.2)
        double priceWeight = 0.2;
        // Simple heuristic: assume user prefers products in reasonable price range
        if (product.getPrice() > 0) {
            // Normalize price preference (this could be made more sophisticated)
            double priceScore = Math.max(0, 1 - (product.getPrice() / 10000)); // Assuming max price around 10k
            similarity += priceWeight * priceScore;
        }
        totalWeight += priceWeight;

        return totalWeight > 0 ? similarity / totalWeight : 0.0;
    }

    /**
     * Calculate temporal similarity based on interaction recency
     */
    public static double temporalSimilarity(List<Interaction> interactionsA, List<Interaction> interactionsB) {
        if (interactionsA.isEmpty() || interactionsB.isEmpty()) {
            return 0.0;
        }

        // Get most recent interactions
        long maxTimeA = interactionsA.stream()
                .mapToLong(Interaction::getTimestampMillis)
                .max()
                .orElse(0);

        long maxTimeB = interactionsB.stream()
                .mapToLong(Interaction::getTimestampMillis)
                .max()
                .orElse(0);

        // Calculate time difference in hours
        long timeDiff = Math.abs(maxTimeA - maxTimeB) / (1000 * 60 * 60);

        // Exponential decay: similarity decreases as time difference increases
        return Math.exp(-timeDiff / 168.0); // Decay over one week (168 hours)
    }

    /**
     * Calculate weighted similarity combining multiple similarity measures
     */
    public static double weightedSimilarity(Map<String, Double> similarities, Map<String, Double> weights) {
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, Double> entry : similarities.entrySet()) {
            String type = entry.getKey();
            double similarity = entry.getValue();
            double weight = weights.getOrDefault(type, 1.0);

            weightedSum += similarity * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
    }

    /**
     * Create user interaction vector for similarity calculation
     */
    public static Map<Integer, Double> createUserInteractionVector(User user, List<Interaction> interactions) {
        Map<Integer, Double> vector = new HashMap<>();

        for (Interaction interaction : interactions) {
            if (interaction.getUserId() == user.getUserId()) {
                int productId = interaction.getProductId();
                double weight = interaction.getInteractionWeight();

                // Combine multiple interactions for same product
                vector.merge(productId, weight, Double::sum);
            }
        }

        return vector;
    }

    /**
     * Create item interaction vector (users who interacted with item)
     */
    public static Map<Integer, Double> createItemInteractionVector(int productId, List<Interaction> interactions) {
        Map<Integer, Double> vector = new HashMap<>();

        for (Interaction interaction : interactions) {
            if (interaction.getProductId() == productId) {
                int userId = interaction.getUserId();
                double weight = interaction.getInteractionWeight();

                // Combine multiple interactions from same user
                vector.merge(userId, weight, Double::sum);
            }
        }

        return vector;
    }

    /**
     * Find most similar users to a given user
     */
    public static List<Map.Entry<Integer, Double>> findMostSimilarUsers(
            int targetUserId, Map<Integer, Map<Integer, Double>> userVectors, int topK) {

        Map<Integer, Double> targetVector = userVectors.get(targetUserId);
        if (targetVector == null) {
            return new ArrayList<>();
        }

        List<Map.Entry<Integer, Double>> similarities = new ArrayList<>();

        for (Map.Entry<Integer, Map<Integer, Double>> entry : userVectors.entrySet()) {
            int userId = entry.getKey();
            if (userId != targetUserId) {
                Map<Integer, Double> userVector = entry.getValue();
                double similarity = cosineSimilarity(targetVector, userVector);
                similarities.add(new AbstractMap.SimpleEntry<>(userId, similarity));
            }
        }

        // Sort by similarity descending and return top K
        similarities.sort(Map.Entry.<Integer, Double>comparingByValue().reversed());
        return similarities.subList(0, Math.min(topK, similarities.size()));
    }

    /**
     * Normalize similarity score to [0, 1] range
     */
    public static double normalizeSimilarity(double similarity) {
        return Math.max(0.0, Math.min(1.0, similarity));
    }
}