package com.recommendation.core.algorithms;

import com.recommendation.models.*;
import com.recommendation.core.hashtable.CustomHashMap;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Content-based filtering implementation
 * Recommends items based on product features and user preferences
 */
public class ContentBasedFilter {

    private CustomHashMap<Integer, User> userMap;
    private CustomHashMap<Integer, Product> productMap;
    private List<Interaction> interactions;
    private Map<Integer, Map<String, Double>> userProfileCache;

    public ContentBasedFilter(CustomHashMap<Integer, User> userMap,
                             CustomHashMap<Integer, Product> productMap,
                             List<Interaction> interactions) {
        this.userMap = userMap;
        this.productMap = productMap;
        this.interactions = interactions;
        this.userProfileCache = new HashMap<>();
    }

    /**
     * Generate content-based recommendations for a user
     */
    public List<Recommendation> getContentBasedRecommendations(int userId, int topK) {
        User user = userMap.get(userId);
        if (user == null) {
            return new ArrayList<>();
        }

        // Build or get user profile
        Map<String, Double> userProfile = getUserProfile(userId);
        if (userProfile.isEmpty()) {
            return new ArrayList<>();
        }

        // Score all products user hasn't interacted with
        Set<Integer> userProducts = user.getViewedProducts();
        userProducts.addAll(user.getPurchasedProducts());

        List<Recommendation> recommendations = new ArrayList<>();

        for (Integer productId : productMap.keySet()) {
            if (userProducts.contains(productId)) continue;

            Product product = productMap.get(productId);
            if (product == null) continue;

            double score = calculateContentScore(userProfile, product);
            if (score > 0) {
                Recommendation rec = new Recommendation(userId, productId, score, "content_based");
                rec.addScoreComponent("content_similarity", score);

                // Generate explanation based on matching features
                String explanation = generateContentExplanation(userProfile, product);
                rec.setExplanation(explanation);

                recommendations.add(rec);
            }
        }

        // Sort by score and return top K
        recommendations.sort(Collections.reverseOrder());
        return recommendations.subList(0, Math.min(topK, recommendations.size()));
    }

    /**
     * Build user profile based on interaction history
     */
    private Map<String, Double> getUserProfile(int userId) {
        // Check cache first
        if (userProfileCache.containsKey(userId)) {
            return userProfileCache.get(userId);
        }

        User user = userMap.get(userId);
        if (user == null) {
            return new HashMap<>();
        }

        Map<String, Double> profile = new HashMap<>();

        // Analyze user's interaction history
        List<Interaction> userInteractions = interactions.stream()
                .filter(i -> i.getUserId() == userId)
                .collect(Collectors.toList());

        if (userInteractions.isEmpty()) {
            userProfileCache.put(userId, profile);
            return profile;
        }

        // Build profile from interacted products
        Map<String, Double> categoryWeights = new HashMap<>();
        Map<String, Double> brandWeights = new HashMap<>();
        Map<String, Double> priceRangeWeights = new HashMap<>();
        Map<String, Double> tagWeights = new HashMap<>();

        double totalInteractionWeight = 0.0;

        for (Interaction interaction : userInteractions) {
            Product product = productMap.get(interaction.getProductId());
            if (product == null) continue;

            double interactionWeight = interaction.getInteractionWeight();
            totalInteractionWeight += interactionWeight;

            // Category preferences
            String category = product.getCategory();
            if (category != null && !category.isEmpty()) {
                categoryWeights.merge(category.toLowerCase(), interactionWeight, Double::sum);
            }

            // Brand preferences
            String brand = product.getBrand();
            if (brand != null && !brand.isEmpty()) {
                brandWeights.merge(brand.toLowerCase(), interactionWeight, Double::sum);
            }

            // Price range preferences
            String priceRange = getPriceRange(product.getPrice());
            priceRangeWeights.merge(priceRange, interactionWeight, Double::sum);

            // Tag preferences
            for (String tag : product.getTags()) {
                if (tag != null && !tag.isEmpty()) {
                    tagWeights.merge(tag.toLowerCase(), interactionWeight, Double::sum);
                }
            }
        }

        // Normalize weights to get preferences
        if (totalInteractionWeight > 0) {
            // Category preferences (weight: 0.4)
            for (Map.Entry<String, Double> entry : categoryWeights.entrySet()) {
                profile.put("category:" + entry.getKey(), 
                           (entry.getValue() / totalInteractionWeight) * 0.4);
            }

            // Brand preferences (weight: 0.25)
            for (Map.Entry<String, Double> entry : brandWeights.entrySet()) {
                profile.put("brand:" + entry.getKey(), 
                           (entry.getValue() / totalInteractionWeight) * 0.25);
            }

            // Price range preferences (weight: 0.2)
            for (Map.Entry<String, Double> entry : priceRangeWeights.entrySet()) {
                profile.put("price_range:" + entry.getKey(), 
                           (entry.getValue() / totalInteractionWeight) * 0.2);
            }

            // Tag preferences (weight: 0.15)
            for (Map.Entry<String, Double> entry : tagWeights.entrySet()) {
                profile.put("tag:" + entry.getKey(), 
                           (entry.getValue() / totalInteractionWeight) * 0.15);
            }
        }

        // Cache the profile
        userProfileCache.put(userId, profile);

        return profile;
    }

    /**
     * Calculate content-based score for a product given user profile
     */
    private double calculateContentScore(Map<String, Double> userProfile, Product product) {
        double score = 0.0;

        // Category matching
        String category = product.getCategory();
        if (category != null && !category.isEmpty()) {
            String categoryKey = "category:" + category.toLowerCase();
            score += userProfile.getOrDefault(categoryKey, 0.0);
        }

        // Brand matching
        String brand = product.getBrand();
        if (brand != null && !brand.isEmpty()) {
            String brandKey = "brand:" + brand.toLowerCase();
            score += userProfile.getOrDefault(brandKey, 0.0);
        }

        // Price range matching
        String priceRange = getPriceRange(product.getPrice());
        String priceKey = "price_range:" + priceRange;
        score += userProfile.getOrDefault(priceKey, 0.0);

        // Tag matching
        for (String tag : product.getTags()) {
            if (tag != null && !tag.isEmpty()) {
                String tagKey = "tag:" + tag.toLowerCase();
                score += userProfile.getOrDefault(tagKey, 0.0);
            }
        }

        // Bonus for highly rated products
        if (product.getRating() > 4.0) {
            score += 0.1;
        }

        // Bonus for popular products (normalized by view count)
        if (product.getViewCount() > 100) {
            score += 0.05;
        }

        return Math.max(0.0, Math.min(1.0, score)); // Normalize to [0, 1]
    }

    /**
     * Get price range category for a product
     */
    private String getPriceRange(double price) {
        if (price < 500) return "budget";
        else if (price < 1500) return "low";
        else if (price < 3000) return "mid";
        else if (price < 5000) return "high";
        else return "premium";
    }

    /**
     * Generate explanation for content-based recommendation
     */
    private String generateContentExplanation(Map<String, Double> userProfile, Product product) {
        List<String> reasons = new ArrayList<>();

        // Find strongest matching features
        String category = product.getCategory();
        if (category != null) {
            String categoryKey = "category:" + category.toLowerCase();
            double categoryPref = userProfile.getOrDefault(categoryKey, 0.0);
            if (categoryPref > 0.1) {
                reasons.add("matches your interest in " + category);
            }
        }

        String brand = product.getBrand();
        if (brand != null) {
            String brandKey = "brand:" + brand.toLowerCase();
            double brandPref = userProfile.getOrDefault(brandKey, 0.0);
            if (brandPref > 0.1) {
                reasons.add("from " + brand + " which you like");
            }
        }

        if (product.getRating() > 4.0) {
            reasons.add("highly rated (" + String.format("%.1f", product.getRating()) + "/5)");
        }

        if (reasons.isEmpty()) {
            return "Based on your browsing preferences";
        }

        return "Recommended because it " + String.join(", ", reasons);
    }

    /**
     * Get trending products in user's preferred categories
     */
    public List<Recommendation> getTrendingInPreferences(int userId, int topK) {
        Map<String, Double> userProfile = getUserProfile(userId);
        if (userProfile.isEmpty()) {
            return new ArrayList<>();
        }

        // Find user's top categories
        Set<String> preferredCategories = userProfile.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("category:") && entry.getValue() > 0.1)
                .map(entry -> entry.getKey().substring("category:".length()))
                .collect(Collectors.toSet());

        if (preferredCategories.isEmpty()) {
            return new ArrayList<>();
        }

        // Get trending products in preferred categories
        List<Recommendation> recommendations = new ArrayList<>();
        User user = userMap.get(userId);
        Set<Integer> userProducts = user != null ? 
                new HashSet<>(user.getViewedProducts()) : new HashSet<>();

        for (Integer productId : productMap.keySet()) {
            if (userProducts.contains(productId)) continue;

            Product product = productMap.get(productId);
            if (product == null) continue;

            String productCategory = product.getCategory();
            if (productCategory != null && 
                preferredCategories.contains(productCategory.toLowerCase())) {

                // Calculate trending score
                double trendingScore = product.getPopularityScore();
                double categoryPref = userProfile.getOrDefault("category:" + productCategory.toLowerCase(), 0.0);
                double finalScore = trendingScore * categoryPref;

                if (finalScore > 0) {
                    Recommendation rec = new Recommendation(userId, productId, finalScore, "trending_content");
                    rec.addScoreComponent("trending", trendingScore);
                    rec.addScoreComponent("category_preference", categoryPref);
                    rec.setExplanation("Trending in " + productCategory + ", which matches your interests");

                    recommendations.add(rec);
                }
            }
        }

        recommendations.sort(Collections.reverseOrder());
        return recommendations.subList(0, Math.min(topK, recommendations.size()));
    }

    /**
     * Get similar products to those user has interacted with
     */
    public List<Recommendation> getSimilarProducts(int userId, int topK) {
        User user = userMap.get(userId);
        if (user == null) {
            return new ArrayList<>();
        }

        Set<Integer> userProducts = new HashSet<>();
        userProducts.addAll(user.getViewedProducts());
        userProducts.addAll(user.getPurchasedProducts());

        if (userProducts.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, Double> candidateScores = new HashMap<>();

        // For each product user interacted with, find similar products
        for (int userProductId : userProducts) {
            Product userProduct = productMap.get(userProductId);
            if (userProduct == null) continue;

            for (Integer candidateId : productMap.keySet()) {
                if (userProducts.contains(candidateId)) continue;

                Product candidate = productMap.get(candidateId);
                if (candidate == null) continue;

                double similarity = SimilarityCalculator.contentSimilarity(userProduct, candidate);
                if (similarity > 0.3) { // Threshold for similarity
                    candidateScores.merge(candidateId, similarity, Double::max);
                }
            }
        }

        // Convert to recommendations
        List<Recommendation> recommendations = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : candidateScores.entrySet()) {
            int productId = entry.getKey();
            double score = entry.getValue();

            Recommendation rec = new Recommendation(userId, productId, score, "similar_content");
            rec.addScoreComponent("content_similarity", score);

            Product product = productMap.get(productId);
            rec.setExplanation("Similar to products you've viewed in " + 
                              (product != null ? product.getCategory() : "your preferred categories"));

            recommendations.add(rec);
        }

        recommendations.sort(Collections.reverseOrder());
        return recommendations.subList(0, Math.min(topK, recommendations.size()));
    }

    /**
     * Clear user profile cache
     */
    public void clearCache() {
        userProfileCache.clear();
    }

    /**
     * Get cache statistics
     */
    public String getCacheStatistics() {
        return "ContentBasedFilter Cache: " + userProfileCache.size() + " user profiles cached";
    }

    /**
     * Get user profile for analysis
     */
    public Map<String, Double> getUserProfileForAnalysis(int userId) {
        return new HashMap<>(getUserProfile(userId));
    }
}
