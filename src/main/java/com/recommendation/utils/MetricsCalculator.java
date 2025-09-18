package com.recommendation.utils;

import com.recommendation.models.*;
import java.util.*;

/**
 * Utility class for calculating recommendation system metrics
 * Includes precision, recall, F1-score, diversity, and novelty metrics
 */
public class MetricsCalculator {

    /**
     * Calculate precision for recommendations
     * Precision = True Positives / (True Positives + False Positives)
     */
    public static double calculatePrecision(List<Recommendation> recommendations, Set<Integer> relevantItems) {
        if (recommendations.isEmpty()) {
            return 0.0;
        }

        int truePositives = 0;
        for (Recommendation rec : recommendations) {
            if (relevantItems.contains(rec.getProductId())) {
                truePositives++;
            }
        }

        return (double) truePositives / recommendations.size();
    }

    /**
     * Calculate recall for recommendations
     * Recall = True Positives / (True Positives + False Negatives)
     */
    public static double calculateRecall(List<Recommendation> recommendations, Set<Integer> relevantItems) {
        if (relevantItems.isEmpty()) {
            return 0.0;
        }

        int truePositives = 0;
        for (Recommendation rec : recommendations) {
            if (relevantItems.contains(rec.getProductId())) {
                truePositives++;
            }
        }

        return (double) truePositives / relevantItems.size();
    }

    /**
     * Calculate F1-score for recommendations
     * F1 = 2 * (Precision * Recall) / (Precision + Recall)
     */
    public static double calculateF1Score(List<Recommendation> recommendations, Set<Integer> relevantItems) {
        double precision = calculatePrecision(recommendations, relevantItems);
        double recall = calculateRecall(recommendations, relevantItems);

        if (precision + recall == 0) {
            return 0.0;
        }

        return 2 * (precision * recall) / (precision + recall);
    }

    /**
     * Calculate Mean Absolute Error for rating predictions
     */
    public static double calculateMAE(Map<Integer, Double> predictions, Map<Integer, Double> actualRatings) {
        if (predictions.isEmpty()) {
            return 0.0;
        }

        double totalError = 0.0;
        int count = 0;

        for (Map.Entry<Integer, Double> entry : predictions.entrySet()) {
            Integer productId = entry.getKey();
            Double predictedRating = entry.getValue();
            Double actualRating = actualRatings.get(productId);

            if (actualRating != null) {
                totalError += Math.abs(predictedRating - actualRating);
                count++;
            }
        }

        return count > 0 ? totalError / count : 0.0;
    }

    /**
     * Calculate Root Mean Square Error for rating predictions
     */
    public static double calculateRMSE(Map<Integer, Double> predictions, Map<Integer, Double> actualRatings) {
        if (predictions.isEmpty()) {
            return 0.0;
        }

        double totalSquaredError = 0.0;
        int count = 0;

        for (Map.Entry<Integer, Double> entry : predictions.entrySet()) {
            Integer productId = entry.getKey();
            Double predictedRating = entry.getValue();
            Double actualRating = actualRatings.get(productId);

            if (actualRating != null) {
                double error = predictedRating - actualRating;
                totalSquaredError += error * error;
                count++;
            }
        }

        return count > 0 ? Math.sqrt(totalSquaredError / count) : 0.0;
    }

    /**
     * Calculate diversity of recommendations based on categories
     */
    public static double calculateCategoryDiversity(List<Recommendation> recommendations, 
                                                   Map<Integer, String> productCategories) {
        if (recommendations.isEmpty()) {
            return 0.0;
        }

        Set<String> uniqueCategories = new HashSet<>();

        for (Recommendation rec : recommendations) {
            String category = productCategories.get(rec.getProductId());
            if (category != null) {
                uniqueCategories.add(category);
            }
        }

        return (double) uniqueCategories.size() / recommendations.size();
    }

    /**
     * Calculate intra-list diversity (average pairwise dissimilarity)
     */
    public static double calculateIntraListDiversity(List<Recommendation> recommendations,
                                                    Map<Integer, Map<String, Double>> productFeatures) {
        if (recommendations.size() < 2) {
            return 0.0;
        }

        double totalDissimilarity = 0.0;
        int pairCount = 0;

        for (int i = 0; i < recommendations.size(); i++) {
            for (int j = i + 1; j < recommendations.size(); j++) {
                int product1 = recommendations.get(i).getProductId();
                int product2 = recommendations.get(j).getProductId();

                Map<String, Double> features1 = productFeatures.get(product1);
                Map<String, Double> features2 = productFeatures.get(product2);

                if (features1 != null && features2 != null) {
                    double similarity = calculateCosineSimilarity(features1, features2);
                    double dissimilarity = 1.0 - similarity;
                    totalDissimilarity += dissimilarity;
                    pairCount++;
                }
            }
        }

        return pairCount > 0 ? totalDissimilarity / pairCount : 0.0;
    }

    /**
     * Calculate novelty of recommendations
     * Novelty is based on how popular the recommended items are (less popular = more novel)
     */
    public static double calculateNovelty(List<Recommendation> recommendations,
                                        Map<Integer, Integer> productPopularity) {
        if (recommendations.isEmpty()) {
            return 0.0;
        }

        double totalNovelty = 0.0;
        int totalPopularity = productPopularity.values().stream().mapToInt(Integer::intValue).sum();

        for (Recommendation rec : recommendations) {
            Integer popularity = productPopularity.get(rec.getProductId());
            if (popularity != null && totalPopularity > 0) {
                double itemProbability = (double) popularity / totalPopularity;
                double novelty = -Math.log(itemProbability) / Math.log(2); // Information content
                totalNovelty += novelty;
            }
        }

        return totalNovelty / recommendations.size();
    }

    /**
     * Calculate coverage of recommendations (fraction of all items recommended)
     */
    public static double calculateCoverage(List<List<Recommendation>> allRecommendations,
                                         int totalProducts) {
        Set<Integer> recommendedProducts = new HashSet<>();

        for (List<Recommendation> recommendations : allRecommendations) {
            for (Recommendation rec : recommendations) {
                recommendedProducts.add(rec.getProductId());
            }
        }

        return totalProducts > 0 ? (double) recommendedProducts.size() / totalProducts : 0.0;
    }

    /**
     * Calculate serendipity (unexpected but relevant recommendations)
     */
    public static double calculateSerendipity(List<Recommendation> recommendations,
                                            Set<Integer> expectedItems,
                                            Set<Integer> relevantItems) {
        if (recommendations.isEmpty()) {
            return 0.0;
        }

        int serendipitousCount = 0;

        for (Recommendation rec : recommendations) {
            int productId = rec.getProductId();
            // Serendipitous if relevant but not expected
            if (relevantItems.contains(productId) && !expectedItems.contains(productId)) {
                serendipitousCount++;
            }
        }

        return (double) serendipitousCount / recommendations.size();
    }

    /**
     * Calculate Normalized Discounted Cumulative Gain (NDCG)
     */
    public static double calculateNDCG(List<Recommendation> recommendations,
                                     Map<Integer, Double> relevanceScores,
                                     int k) {
        if (recommendations.isEmpty() || k <= 0) {
            return 0.0;
        }

        // Calculate DCG
        double dcg = 0.0;
        for (int i = 0; i < Math.min(k, recommendations.size()); i++) {
            int productId = recommendations.get(i).getProductId();
            Double relevance = relevanceScores.get(productId);
            if (relevance != null) {
                dcg += relevance / (Math.log(i + 2) / Math.log(2)); // i+2 because rank starts from 1
            }
        }

        // Calculate IDCG (Ideal DCG)
        List<Double> sortedRelevances = new ArrayList<>(relevanceScores.values());
        sortedRelevances.sort(Collections.reverseOrder());

        double idcg = 0.0;
        for (int i = 0; i < Math.min(k, sortedRelevances.size()); i++) {
            idcg += sortedRelevances.get(i) / (Math.log(i + 2) / Math.log(2));
        }

        return idcg > 0 ? dcg / idcg : 0.0;
    }

    /**
     * Calculate hit rate (fraction of users for whom at least one relevant item was recommended)
     */
    public static double calculateHitRate(Map<Integer, List<Recommendation>> userRecommendations,
                                        Map<Integer, Set<Integer>> userRelevantItems) {
        if (userRecommendations.isEmpty()) {
            return 0.0;
        }

        int hits = 0;

        for (Map.Entry<Integer, List<Recommendation>> entry : userRecommendations.entrySet()) {
            int userId = entry.getKey();
            List<Recommendation> recommendations = entry.getValue();
            Set<Integer> relevantItems = userRelevantItems.get(userId);

            if (relevantItems != null && !relevantItems.isEmpty()) {
                boolean hasHit = recommendations.stream()
                    .anyMatch(rec -> relevantItems.contains(rec.getProductId()));

                if (hasHit) {
                    hits++;
                }
            }
        }

        return (double) hits / userRecommendations.size();
    }

    /**
     * Calculate Mean Reciprocal Rank (MRR)
     */
    public static double calculateMRR(Map<Integer, List<Recommendation>> userRecommendations,
                                    Map<Integer, Set<Integer>> userRelevantItems) {
        if (userRecommendations.isEmpty()) {
            return 0.0;
        }

        double totalReciprocalRank = 0.0;
        int userCount = 0;

        for (Map.Entry<Integer, List<Recommendation>> entry : userRecommendations.entrySet()) {
            int userId = entry.getKey();
            List<Recommendation> recommendations = entry.getValue();
            Set<Integer> relevantItems = userRelevantItems.get(userId);

            if (relevantItems != null && !relevantItems.isEmpty()) {
                for (int i = 0; i < recommendations.size(); i++) {
                    if (relevantItems.contains(recommendations.get(i).getProductId())) {
                        totalReciprocalRank += 1.0 / (i + 1); // Rank is 1-based
                        break; // Only count the first relevant item
                    }
                }
                userCount++;
            }
        }

        return userCount > 0 ? totalReciprocalRank / userCount : 0.0;
    }

    /**
     * Helper method to calculate cosine similarity between feature vectors
     */
    private static double calculateCosineSimilarity(Map<String, Double> features1, 
                                                   Map<String, Double> features2) {
        Set<String> commonFeatures = new HashSet<>(features1.keySet());
        commonFeatures.retainAll(features2.keySet());

        if (commonFeatures.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String feature : commonFeatures) {
            double value1 = features1.get(feature);
            double value2 = features2.get(feature);

            dotProduct += value1 * value2;
        }

        for (double value : features1.values()) {
            norm1 += value * value;
        }

        for (double value : features2.values()) {
            norm2 += value * value;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Calculate comprehensive evaluation metrics
     */
    public static Map<String, Double> calculateComprehensiveMetrics(
            Map<Integer, List<Recommendation>> userRecommendations,
            Map<Integer, Set<Integer>> userRelevantItems,
            Map<Integer, String> productCategories) {

        Map<String, Double> metrics = new HashMap<>();

        // Calculate average precision, recall, F1
        double totalPrecision = 0.0;
        double totalRecall = 0.0;
        double totalF1 = 0.0;
        double totalDiversity = 0.0;

        int validUserCount = 0;

        for (Map.Entry<Integer, List<Recommendation>> entry : userRecommendations.entrySet()) {
            int userId = entry.getKey();
            List<Recommendation> recommendations = entry.getValue();
            Set<Integer> relevantItems = userRelevantItems.get(userId);

            if (relevantItems != null && !relevantItems.isEmpty() && !recommendations.isEmpty()) {
                double precision = calculatePrecision(recommendations, relevantItems);
                double recall = calculateRecall(recommendations, relevantItems);
                double f1 = calculateF1Score(recommendations, relevantItems);
                double diversity = calculateCategoryDiversity(recommendations, productCategories);

                totalPrecision += precision;
                totalRecall += recall;
                totalF1 += f1;
                totalDiversity += diversity;
                validUserCount++;
            }
        }

        if (validUserCount > 0) {
            metrics.put("avg_precision", totalPrecision / validUserCount);
            metrics.put("avg_recall", totalRecall / validUserCount);
            metrics.put("avg_f1", totalF1 / validUserCount);
            metrics.put("avg_diversity", totalDiversity / validUserCount);
        }

        // Calculate hit rate and MRR
        metrics.put("hit_rate", calculateHitRate(userRecommendations, userRelevantItems));
        metrics.put("mrr", calculateMRR(userRecommendations, userRelevantItems));

        return metrics;
    }
}