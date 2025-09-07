package com.recommendation.models;

import java.util.Date;
import java.util.Objects;

/**
 * Interaction model representing user-product interactions
 * Tracks different types of user behaviors with products
 */
public class Interaction {
    public enum InteractionType {
        VIEW, PURCHASE, WISHLIST, RATING, SEARCH, CART_ADD, CART_REMOVE
    }

    private int interactionId;
    private int userId;
    private int productId;
    private InteractionType type;
    private double value; // rating value, price for purchase, etc.
    private Date timestamp;
    private String context; // search query, referrer, etc.
    private int sessionId;

    public Interaction(int userId, int productId, InteractionType type) {
        this.userId = userId;
        this.productId = productId;
        this.type = type;
        this.timestamp = new Date();
        this.value = 0.0;
        this.context = "";
        this.sessionId = 0;
    }

    public Interaction(int userId, int productId, InteractionType type, double value) {
        this(userId, productId, type);
        this.value = value;
    }

    public Interaction(int userId, int productId, InteractionType type, double value, String context) {
        this(userId, productId, type, value);
        this.context = context;
    }

    // Getters and Setters
    public int getInteractionId() { return interactionId; }
    public void setInteractionId(int interactionId) { this.interactionId = interactionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public InteractionType getType() { return type; }
    public void setType(InteractionType type) { this.type = type; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    // Utility methods
    public double getInteractionWeight() {
        // Different interaction types have different weights
        switch (type) {
            case VIEW: return 1.0;
            case WISHLIST: return 2.0;
            case CART_ADD: return 3.0;
            case PURCHASE: return 5.0;
            case RATING: return value; // Use rating value as weight
            default: return 1.0;
        }
    }

    public boolean isImplicitFeedback() {
        return type == InteractionType.VIEW || 
               type == InteractionType.SEARCH || 
               type == InteractionType.CART_ADD;
    }

    public boolean isExplicitFeedback() {
        return type == InteractionType.RATING || 
               type == InteractionType.PURCHASE || 
               type == InteractionType.WISHLIST;
    }

    public long getTimestampMillis() {
        return timestamp.getTime();
    }

    public boolean isRecentInteraction(long thresholdHours) {
        long currentTime = System.currentTimeMillis();
        long interactionTime = timestamp.getTime();
        long hoursDiff = (currentTime - interactionTime) / (1000 * 60 * 60);
        return hoursDiff <= thresholdHours;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Interaction interaction = (Interaction) obj;
        return userId == interaction.userId &&
               productId == interaction.productId &&
               type == interaction.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, productId, type);
    }

    @Override
    public String toString() {
        return "Interaction{" +
                "userId=" + userId +
                ", productId=" + productId +
                ", type=" + type +
                ", value=" + value +
                ", timestamp=" + timestamp +
                '}';
    }
}
