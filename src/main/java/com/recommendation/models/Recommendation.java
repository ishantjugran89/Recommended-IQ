package com.recommendation.models;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * Recommendation model representing a recommended product for a user
 * Contains recommendation score, explanation, and metadata
 */
public class Recommendation implements Comparable<Recommendation> {
    private int userId;
    private int productId;
    private double score;
    private String algorithm; // collaborative, content-based, hybrid, etc.
    private String explanation; // Why this product was recommended
    private Map<String, Double> scoreComponents; // breakdown of score calculation
    private int rank;
    private long timestamp;
    private boolean accepted; // If user interacted with recommendation

    public Recommendation(int userId, int productId, double score, String algorithm) {
        this.userId = userId;
        this.productId = productId;
        this.score = score;
        this.algorithm = algorithm;
        this.scoreComponents = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
        this.accepted = false;
        this.rank = 0;
        this.explanation = "";
    }

    public Recommendation(int userId, int productId, double score, String algorithm, String explanation) {
        this(userId, productId, score, algorithm);
        this.explanation = explanation;
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public Map<String, Double> getScoreComponents() { return scoreComponents; }
    public void setScoreComponents(Map<String, Double> scoreComponents) { this.scoreComponents = scoreComponents; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    // Utility methods
    public void addScoreComponent(String component, double value) {
        scoreComponents.put(component, value);
    }

    public double getScoreComponent(String component) {
        return scoreComponents.getOrDefault(component, 0.0);
    }

    public void normalizeScore(double maxScore) {
        if (maxScore > 0) {
            this.score = this.score / maxScore;
        }
    }

    public double getConfidence() {
        // Calculate confidence based on score components
        if (scoreComponents.isEmpty()) {
            return score;
        }

        double totalWeight = scoreComponents.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        return Math.min(1.0, totalWeight / scoreComponents.size());
    }

    public boolean isHighConfidence(double threshold) {
        return getConfidence() >= threshold;
    }

    public String generateExplanation(Product product, User user) {
        StringBuilder explanation = new StringBuilder();

        if (algorithm.contains("collaborative")) {
            explanation.append("Users with similar preferences also liked this product. ");
        }

        if (algorithm.contains("content")) {
            explanation.append("Based on your interest in ").append(product.getCategory()).append(". ");
        }

        if (product.getRating() > 4.0) {
            explanation.append("Highly rated product (").append(product.getRating()).append("/5). ");
        }

        if (scoreComponents.containsKey("trending")) {
            explanation.append("Currently trending in ").append(product.getCategory()).append(". ");
        }

        this.explanation = explanation.toString().trim();
        return this.explanation;
    }

    @Override
    public int compareTo(Recommendation other) {
        // Sort by score in descending order (higher scores first)
        return Double.compare(other.score, this.score);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Recommendation that = (Recommendation) obj;
        return userId == that.userId && productId == that.productId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, productId);
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "userId=" + userId +
                ", productId=" + productId +
                ", score=" + String.format("%.3f", score) +
                ", algorithm='" + algorithm + "\'" +
                ", rank=" + rank +
                '}';
    }
}