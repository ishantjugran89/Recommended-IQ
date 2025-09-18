package com.recommendation.models;

import java.util.*;

/**
 * Product model representing items in the e-commerce catalog
 * Contains product metadata, categories, and interaction metrics
 */
public class Product {
    private int productId;
    private String name;
    private String description;
    private String category;
    private String brand;
    private double price;
    private double rating;
    private int reviewCount;
    private Set<String> tags;
    private Map<String, String> attributes; // color, size, material, etc.
    private int viewCount;
    private int purchaseCount;
    private int wishlistCount;
    private Date addedDate;
    private boolean inStock;

    public Product(int productId, String name, String category, String brand, double price) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.price = price;
        this.rating = 0.0;
        this.reviewCount = 0;
        this.tags = new HashSet<>();
        this.attributes = new HashMap<>();
        this.viewCount = 0;
        this.purchaseCount = 0;
        this.wishlistCount = 0;
        this.addedDate = new Date();
        this.inStock = true;
    }

    // Getters and Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }

    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getPurchaseCount() { return purchaseCount; }
    public void setPurchaseCount(int purchaseCount) { this.purchaseCount = purchaseCount; }

    public int getWishlistCount() { return wishlistCount; }
    public void setWishlistCount(int wishlistCount) { this.wishlistCount = wishlistCount; }

    public Date getAddedDate() { return addedDate; }
    public void setAddedDate(Date addedDate) { this.addedDate = addedDate; }

    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }

    // Utility methods
    public void addTag(String tag) {
        tags.add(tag.toLowerCase());
    }

    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public void incrementViewCount() {
        viewCount++;
    }

    public void incrementPurchaseCount() {
        purchaseCount++;
    }

    public void incrementWishlistCount() {
        wishlistCount++;
    }

    public double getPopularityScore() {
        // Simple popularity score based on interactions and rating
        return (viewCount * 0.1 + purchaseCount * 0.5 + wishlistCount * 0.3 + rating * reviewCount * 0.1);
    }

    public boolean matchesCategory(String categoryPattern) {
        return category.toLowerCase().contains(categoryPattern.toLowerCase());
    }

    public boolean hasTags(Set<String> searchTags) {
        return !Collections.disjoint(tags, searchTags);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;
        return productId == product.productId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", name='" + name + "\'" +
                ", category='" + category + "\'" +
                ", brand='" + brand + "\'" +
                ", price=" + price +
                ", rating=" + rating +
                '}';
    }
}
