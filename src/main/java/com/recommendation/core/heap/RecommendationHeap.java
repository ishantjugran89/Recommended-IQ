package com.recommendation.core.heap;

import com.recommendation.models.Recommendation;
import java.util.*;

/**
 * Priority Queue implementation for Top-K recommendations
 * Core DSA: Min-Heap for maintaining top K recommendations efficiently
 */
public class RecommendationHeap {
    private PriorityQueue<Recommendation> minHeap;
    private int maxSize;
    private Set<Integer> seenProducts; // To avoid duplicate recommendations

    public RecommendationHeap(int k) {
        this.maxSize = k;
        // Min-heap: lowest scores at top, so we can remove them when heap is full
        this.minHeap = new PriorityQueue<>(k, Comparator.comparingDouble(Recommendation::getScore));
        this.seenProducts = new HashSet<>();
    }

    /**
     * Add recommendation to heap, maintaining top-K constraint
     * Time Complexity: O(log k)
     */
    public boolean addRecommendation(Recommendation recommendation) {
        if (recommendation == null) {
            return false;
        }

        // Skip if we've already seen this product for this user
        if (seenProducts.contains(recommendation.getProductId())) {
            return false;
        }

        if (minHeap.size() < maxSize) {
            // Heap not full, just add
            minHeap.offer(recommendation);
            seenProducts.add(recommendation.getProductId());
            return true;
        } else {
            // Heap is full, check if new recommendation is better than worst
            Recommendation worst = minHeap.peek();
            if (worst != null && recommendation.getScore() > worst.getScore()) {
                // Remove worst and add new recommendation
                Recommendation removed = minHeap.poll();
                if (removed != null) {
                    seenProducts.remove(removed.getProductId());
                }
                minHeap.offer(recommendation);
                seenProducts.add(recommendation.getProductId());
                return true;
            }
        }

        return false;
    }

    /**
     * Add multiple recommendations efficiently
     */
    public void addRecommendations(Collection<Recommendation> recommendations) {
        for (Recommendation rec : recommendations) {
            addRecommendation(rec);
        }
    }

    /**
     * Get top-K recommendations in descending order of score
     * Time Complexity: O(k log k)
     */
    public List<Recommendation> getTopK() {
        List<Recommendation> result = new ArrayList<>(minHeap);

        // Sort in descending order of score (best recommendations first)
        result.sort(Collections.reverseOrder());

        // Set ranks
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }

        return result;
    }

    /**
     * Get top-K recommendations as array
     */
    public Recommendation[] getTopKArray() {
        List<Recommendation> topK = getTopK();
        return topK.toArray(new Recommendation[0]);
    }

    /**
     * Peek at current worst recommendation (minimum score)
     */
    public Recommendation peekWorst() {
        return minHeap.peek();
    }

    /**
     * Get current size of heap
     */
    public int size() {
        return minHeap.size();
    }

    /**
     * Check if heap is empty
     */
    public boolean isEmpty() {
        return minHeap.isEmpty();
    }

    /**
     * Check if heap is full
     */
    public boolean isFull() {
        return minHeap.size() >= maxSize;
    }

    /**
     * Get maximum capacity
     */
    public int getCapacity() {
        return maxSize;
    }

    /**
     * Clear all recommendations
     */
    public void clear() {
        minHeap.clear();
        seenProducts.clear();
    }

    /**
     * Get minimum score threshold (score of worst recommendation)
     */
    public double getMinScoreThreshold() {
        Recommendation worst = minHeap.peek();
        return worst != null ? worst.getScore() : 0.0;
    }

    /**
     * Get maximum score in current heap
     */
    public double getMaxScore() {
        return minHeap.stream()
                .mapToDouble(Recommendation::getScore)
                .max()
                .orElse(0.0);
    }

    /**
     * Get average score of recommendations in heap
     */
    public double getAverageScore() {
        if (minHeap.isEmpty()) {
            return 0.0;
        }

        return minHeap.stream()
                .mapToDouble(Recommendation::getScore)
                .average()
                .orElse(0.0);
    }

    /**
     * Check if a product is already in recommendations
     */
    public boolean containsProduct(int productId) {
        return seenProducts.contains(productId);
    }

    /**
     * Get all product IDs in current recommendations
     */
    public Set<Integer> getRecommendedProductIds() {
        return new HashSet<>(seenProducts);
    }

    /**
     * Merge with another recommendation heap
     */
    public void merge(RecommendationHeap other) {
        for (Recommendation rec : other.minHeap) {
            addRecommendation(rec);
        }
    }

    /**
     * Filter recommendations by minimum score threshold
     */
    public List<Recommendation> getTopKWithMinScore(double minScore) {
        return getTopK().stream()
                .filter(rec -> rec.getScore() >= minScore)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Get recommendations grouped by algorithm
     */
    public Map<String, List<Recommendation>> getTopKGroupedByAlgorithm() {
        Map<String, List<Recommendation>> grouped = new HashMap<>();

        for (Recommendation rec : getTopK()) {
            grouped.computeIfAbsent(rec.getAlgorithm(), k -> new ArrayList<>()).add(rec);
        }

        return grouped;
    }

    /**
     * Get diversity score of current recommendations
     * Based on number of unique categories/brands
     */
    public double getDiversityScore(Map<Integer, String> productCategories) {
        if (minHeap.isEmpty() || productCategories.isEmpty()) {
            return 0.0;
        }

        Set<String> uniqueCategories = new HashSet<>();
        for (Recommendation rec : minHeap) {
            String category = productCategories.get(rec.getProductId());
            if (category != null) {
                uniqueCategories.add(category);
            }
        }

        return (double) uniqueCategories.size() / minHeap.size();
    }

    /**
     * Create a copy of current heap
     */
    public RecommendationHeap copy() {
        RecommendationHeap copy = new RecommendationHeap(maxSize);
        copy.addRecommendations(this.minHeap);
        return copy;
    }

    @Override
    public String toString() {
        List<Recommendation> topK = getTopK();
        StringBuilder sb = new StringBuilder();
        sb.append("RecommendationHeap[size=").append(size())
          .append(", capacity=").append(maxSize)
          .append(", avgScore=").append(String.format("%.3f", getAverageScore()))
          .append("]");

        for (int i = 0; i < Math.min(5, topK.size()); i++) {
            Recommendation rec = topK.get(i);
            sb.append("  ").append(i + 1).append(". Product ")
              .append(rec.getProductId())
              .append(" (score: ").append(String.format("%.3f", rec.getScore()))
              .append(", algo: ").append(rec.getAlgorithm())
              .append(")");
        }

        if (topK.size() > 5) {
            sb.append("  ... and ").append(topK.size() - 5).append(" more");
        }

        return sb.toString();
    }
}
