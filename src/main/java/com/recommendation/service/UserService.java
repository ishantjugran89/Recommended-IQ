package com.recommendation.service;

import com.recommendation.models.*;
import com.recommendation.core.hashtable.CustomHashMap;
import java.util.*;

/**
 * Service class for user management operations
 */
public class UserService {

    private CustomHashMap<Integer, User> userMap;
    private List<Interaction> interactions;

    public UserService(CustomHashMap<Integer, User> userMap, List<Interaction> interactions) {
        this.userMap = userMap;
        this.interactions = interactions;
    }

    /**
     * Get user by ID
     */
    public User getUser(int userId) {
        return userMap.get(userId);
    }

    /**
     * Create new user
     */
    public User createUser(int userId, String username, String email) {
        User user = new User(userId, username, email);
        userMap.put(userId, user);
        return user;
    }

    /**
     * Update user preferences based on interactions
     */
    public void updateUserPreferences(int userId) {
        User user = userMap.get(userId);
        if (user == null) return;

        Map<String, Double> categoryPreferences = new HashMap<>();
        Map<String, Integer> categoryCount = new HashMap<>();

        // Analyze user interactions to build preferences
        for (Interaction interaction : interactions) {
            if (interaction.getUserId() == userId) {
                // This would require product information to get category
                // For now, we'll use a placeholder approach
                String category = "category_" + (interaction.getProductId() % 10); // Placeholder

                double weight = interaction.getInteractionWeight();
                categoryPreferences.merge(category, weight, Double::sum);
                categoryCount.merge(category, 1, Integer::sum);
            }
        }

        // Normalize preferences
        for (Map.Entry<String, Double> entry : categoryPreferences.entrySet()) {
            String category = entry.getKey();
            double totalWeight = entry.getValue();
            int count = categoryCount.get(category);

            double normalizedPreference = totalWeight / count;
            user.updateCategoryPreference(category, normalizedPreference);
        }

        // Update average rating
        double totalRating = 0.0;
        int ratingCount = 0;

        for (Interaction interaction : interactions) {
            if (interaction.getUserId() == userId && interaction.getType() == Interaction.InteractionType.RATING) {
                totalRating += interaction.getValue();
                ratingCount++;
            }
        }

        if (ratingCount > 0) {
            user.setAverageRating(totalRating / ratingCount);
        }
    }

    /**
     * Get user interaction history
     */
    public List<Interaction> getUserInteractions(int userId) {
        List<Interaction> userInteractions = new ArrayList<>();

        for (Interaction interaction : interactions) {
            if (interaction.getUserId() == userId) {
                userInteractions.add(interaction);
            }
        }

        // Sort by timestamp (most recent first)
        userInteractions.sort((i1, i2) -> i2.getTimestamp().compareTo(i1.getTimestamp()));

        return userInteractions;
    }

    /**
     * Get user activity summary
     */
    public Map<String, Object> getUserActivitySummary(int userId) {
        User user = userMap.get(userId);
        if (user == null) {
            return new HashMap<>();
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("user_id", userId);
        summary.put("username", user.getUsername());
        summary.put("total_interactions", user.getTotalInteractions());
        summary.put("viewed_products", user.getViewedProducts().size());
        summary.put("purchased_products", user.getPurchasedProducts().size());
        summary.put("wishlist_items", user.getWishlistProducts().size());
        summary.put("average_rating", user.getAverageRating());
        summary.put("registration_date", user.getRegistrationDate());

        // Interaction type breakdown
        Map<String, Integer> interactionTypes = new HashMap<>();
        for (Interaction interaction : getUserInteractions(userId)) {
            String type = interaction.getType().toString();
            interactionTypes.merge(type, 1, Integer::sum);
        }
        summary.put("interaction_breakdown", interactionTypes);

        // Recent activity (last 7 days)
        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        int recentInteractions = 0;

        for (Interaction interaction : getUserInteractions(userId)) {
            if (interaction.getTimestampMillis() >= oneWeekAgo) {
                recentInteractions++;
            }
        }
        summary.put("recent_interactions_7d", recentInteractions);

        return summary;
    }

    /**
     * Find similar users based on preferences
     */
    public List<Integer> findSimilarUsers(int userId, int count) {
        User targetUser = userMap.get(userId);
        if (targetUser == null) {
            return new ArrayList<>();
        }

        List<Map.Entry<Integer, Double>> similarities = new ArrayList<>();

        for (Integer otherUserId : userMap.keySet()) {
            if (otherUserId.equals(userId)) continue;

            User otherUser = userMap.get(otherUserId);
            if (otherUser == null) continue;

            double similarity = calculateUserSimilarity(targetUser, otherUser);
            if (similarity > 0) {
                similarities.add(new AbstractMap.SimpleEntry<>(otherUserId, similarity));
            }
        }

        // Sort by similarity (highest first)
        similarities.sort(Map.Entry.<Integer, Double>comparingByValue().reversed());

        return similarities.stream()
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Calculate similarity between two users
     */
    private double calculateUserSimilarity(User user1, User user2) {
        // Jaccard similarity on viewed products
        Set<Integer> products1 = new HashSet<>(user1.getViewedProducts());
        products1.addAll(user1.getPurchasedProducts());

        Set<Integer> products2 = new HashSet<>(user2.getViewedProducts());
        products2.addAll(user2.getPurchasedProducts());

        if (products1.isEmpty() && products2.isEmpty()) {
            return 0.0;
        }

        Set<Integer> intersection = new HashSet<>(products1);
        intersection.retainAll(products2);

        Set<Integer> union = new HashSet<>(products1);
        union.addAll(products2);

        double jaccardSimilarity = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

        // Category preference similarity
        Map<String, Double> prefs1 = user1.getCategoryPreferences();
        Map<String, Double> prefs2 = user2.getCategoryPreferences();

        double categorySimiliarity = calculateCategoryPreferenceSimilarity(prefs1, prefs2);

        // Combine similarities
        return jaccardSimilarity * 0.7 + categorySimiliarity * 0.3;
    }

    /**
     * Calculate category preference similarity
     */
    private double calculateCategoryPreferenceSimilarity(Map<String, Double> prefs1, 
                                                        Map<String, Double> prefs2) {
        if (prefs1.isEmpty() && prefs2.isEmpty()) {
            return 1.0;
        }

        if (prefs1.isEmpty() || prefs2.isEmpty()) {
            return 0.0;
        }

        Set<String> allCategories = new HashSet<>(prefs1.keySet());
        allCategories.addAll(prefs2.keySet());

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String category : allCategories) {
            double pref1 = prefs1.getOrDefault(category, 0.0);
            double pref2 = prefs2.getOrDefault(category, 0.0);

            dotProduct += pref1 * pref2;
            norm1 += pref1 * pref1;
            norm2 += pref2 * pref2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Get top categories for user
     */
    public List<String> getTopUserCategories(int userId, int count) {
        User user = userMap.get(userId);
        if (user == null) {
            return new ArrayList<>();
        }

        return user.getCategoryPreferences().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Check if user exists
     */
    public boolean userExists(int userId) {
        return userMap.containsKey(userId);
    }

    /**
     * Get all user IDs
     */
    public Set<Integer> getAllUserIds() {
        return userMap.keySet();
    }

    /**
     * Get user count
     */
    public int getUserCount() {
        return userMap.size();
    }

    /**
     * Get active users (users with at least one interaction)
     */
    public List<Integer> getActiveUsers() {
        List<Integer> activeUsers = new ArrayList<>();

        for (Integer userId : userMap.keySet()) {
            User user = userMap.get(userId);
            if (user != null && user.getTotalInteractions() > 0) {
                activeUsers.add(userId);
            }
        }

        return activeUsers;
    }

    /**
     * Delete user (for testing/demo purposes)
     */
    public boolean deleteUser(int userId) {
        User removedUser = userMap.remove(userId);
        return removedUser != null;
    }
}
