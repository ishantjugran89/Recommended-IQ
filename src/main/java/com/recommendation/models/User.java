package com.recommendation.models;

import java.util.*;

/**
 * User model representing an e-commerce customer
 * Contains user preferences, interaction history, and profile data
 */
public class User {
    private int userId;
    private String username;
    private String email;
    private Map<String, String> preferences; // category -> preference_level
    private Set<Integer> viewedProducts;
    private Set<Integer> purchasedProducts;
    private Set<Integer> wishlistProducts;
    private Map<String, Double> categoryPreferences; // category -> preference_score
    private double averageRating;
    private Date registrationDate;

    public User(int userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.preferences = new HashMap<>();
        this.viewedProducts = new HashSet<>();
        this.purchasedProducts = new HashSet<>();
        this.wishlistProducts = new HashSet<>();
        this.categoryPreferences = new HashMap<>();
        this.averageRating = 0.0;
        this.registrationDate = new Date();
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Map<String, String> getPreferences() { return preferences; }
    public void setPreferences(Map<String, String> preferences) { this.preferences = preferences; }

    public Set<Integer> getViewedProducts() { return viewedProducts; }
    public void setViewedProducts(Set<Integer> viewedProducts) { this.viewedProducts = viewedProducts; }

    public Set<Integer> getPurchasedProducts() { return purchasedProducts; }
    public void setPurchasedProducts(Set<Integer> purchasedProducts) { this.purchasedProducts = purchasedProducts; }

    public Set<Integer> getWishlistProducts() { return wishlistProducts; }
    public void setWishlistProducts(Set<Integer> wishlistProducts) { this.wishlistProducts = wishlistProducts; }

    public Map<String, Double> getCategoryPreferences() { return categoryPreferences; }
    public void setCategoryPreferences(Map<String, Double> categoryPreferences) { this.categoryPreferences = categoryPreferences; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public Date getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Date registrationDate) { this.registrationDate = registrationDate; }

    // Utility methods
    public void addViewedProduct(int productId) {
        viewedProducts.add(productId);
    }

    public void addPurchasedProduct(int productId) {
        purchasedProducts.add(productId);
        viewedProducts.add(productId); // Purchased items are also viewed
    }

    public void addToWishlist(int productId) {
        wishlistProducts.add(productId);
    }

    public void updateCategoryPreference(String category, double score) {
        categoryPreferences.put(category, score);
    }

    public boolean hasInteractedWith(int productId) {
        return viewedProducts.contains(productId) || 
               purchasedProducts.contains(productId) || 
               wishlistProducts.contains(productId);
    }

    public int getTotalInteractions() {
        return viewedProducts.size() + purchasedProducts.size() + wishlistProducts.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + "\'" +
                ", totalInteractions=" + getTotalInteractions() +
                '}';
    }
}
