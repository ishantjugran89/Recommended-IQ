package com.recommendation.service;

import com.recommendation.models.*;
import com.recommendation.core.hashtable.CustomHashMap;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for product management operations
 */
public class ProductService {

    private CustomHashMap<Integer, Product> productMap;
    private List<Interaction> interactions;

    public ProductService(CustomHashMap<Integer, Product> productMap, List<Interaction> interactions) {
        this.productMap = productMap;
        this.interactions = interactions;
    }

    /**
     * Get product by ID
     */
    public Product getProduct(int productId) {
        return productMap.get(productId);
    }

    /**
     * Create new product
     */
    public Product createProduct(int productId, String name, String category, String brand, double price) {
        Product product = new Product(productId, name, category, brand, price);
        productMap.put(productId, product);
        return product;
    }

    /**
     * Search products by name
     */
    public List<Product> searchProducts(String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String lowercaseQuery = query.toLowerCase().trim();
        List<Product> results = new ArrayList<>();

        for (Integer productId : productMap.keySet()) {
            Product product = productMap.get(productId);
            if (product != null) {
                String productName = product.getName().toLowerCase();
                String productDescription = product.getDescription() != null ? 
                                         product.getDescription().toLowerCase() : "";

                // Check if query matches name, description, or tags
                if (productName.contains(lowercaseQuery) || 
                    productDescription.contains(lowercaseQuery) ||
                    product.getTags().stream().anyMatch(tag -> 
                        tag.toLowerCase().contains(lowercaseQuery))) {

                    results.add(product);
                    if (results.size() >= maxResults) {
                        break;
                    }
                }
            }
        }

        // Sort by relevance (exact matches first, then by rating)
        results.sort((p1, p2) -> {
            String name1 = p1.getName().toLowerCase();
            String name2 = p2.getName().toLowerCase();

            boolean exact1 = name1.equals(lowercaseQuery);
            boolean exact2 = name2.equals(lowercaseQuery);

            if (exact1 && !exact2) return -1;
            if (!exact1 && exact2) return 1;

            // If both exact or both not exact, sort by rating
            return Double.compare(p2.getRating(), p1.getRating());
        });

        return results;
    }

    /**
     * Get products by category
     */
    public List<Product> getProductsByCategory(String category, int maxResults) {
        if (category == null || category.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Product> results = new ArrayList<>();

        for (Integer productId : productMap.keySet()) {
            Product product = productMap.get(productId);
            if (product != null && product.getCategory().equalsIgnoreCase(category)) {
                results.add(product);
            }
        }

        // Sort by rating descending
        results.sort((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));

        return results.subList(0, Math.min(maxResults, results.size()));
    }

    /**
     * Get products by brand
     */
    public List<Product> getProductsByBrand(String brand, int maxResults) {
        if (brand == null || brand.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Product> results = new ArrayList<>();

        for (Integer productId : productMap.keySet()) {
            Product product = productMap.get(productId);
            if (product != null && product.getBrand().equalsIgnoreCase(brand)) {
                results.add(product);
            }
        }

        // Sort by popularity
        results.sort((p1, p2) -> Double.compare(p2.getPopularityScore(), p1.getPopularityScore()));

        return results.subList(0, Math.min(maxResults, results.size()));
    }

    /**
     * Get products in price range
     */
    public List<Product> getProductsInPriceRange(double minPrice, double maxPrice, int maxResults) {
        List<Product> results = new ArrayList<>();

        for (Integer productId : productMap.keySet()) {
            Product product = productMap.get(productId);
            if (product != null && product.getPrice() >= minPrice && product.getPrice() <= maxPrice) {
                results.add(product);
            }
        }

        // Sort by value (rating/price ratio)
        results.sort((p1, p2) -> {
            double value1 = p1.getRating() / Math.max(1.0, p1.getPrice() / 1000.0);
            double value2 = p2.getRating() / Math.max(1.0, p2.getPrice() / 1000.0);
            return Double.compare(value2, value1);
        });

        return results.subList(0, Math.min(maxResults, results.size()));
    }

    /**
     * Get top rated products
     */
    public List<Product> getTopRatedProducts(int count) {
        return productMap.values().stream()
                .filter(product -> product.getRating() > 0 && product.getReviewCount() >= 5)
                .sorted((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()))
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Get most popular products
     */
    public List<Product> getMostPopularProducts(int count) {
        return productMap.values().stream()
                .sorted((p1, p2) -> Double.compare(p2.getPopularityScore(), p1.getPopularityScore()))
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Get trending products (high recent activity)
     */
    public List<Product> getTrendingProducts(int count) {
        // Calculate trending score based on recent interactions
        Map<Integer, Double> trendingScores = new HashMap<>();

        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);

        for (Interaction interaction : interactions) {
            if (interaction.getTimestampMillis() >= oneWeekAgo) {
                int productId = interaction.getProductId();
                double weight = interaction.getInteractionWeight();

                // More recent interactions get higher weight
                long daysSince = (System.currentTimeMillis() - interaction.getTimestampMillis()) / (24 * 60 * 60 * 1000);
                double recencyMultiplier = Math.max(0.1, 1.0 - (daysSince / 7.0));

                trendingScores.merge(productId, weight * recencyMultiplier, Double::sum);
            }
        }

        return trendingScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(count)
                .map(entry -> productMap.get(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Update product statistics based on interactions
     */
    public void updateProductStatistics(int productId) {
        Product product = productMap.get(productId);
        if (product == null) return;

        int viewCount = 0;
        int purchaseCount = 0;
        int wishlistCount = 0;
        double totalRating = 0.0;
        int ratingCount = 0;

        for (Interaction interaction : interactions) {
            if (interaction.getProductId() == productId) {
                switch (interaction.getType()) {
                    case VIEW:
                        viewCount++;
                        break;
                    case PURCHASE:
                        purchaseCount++;
                        break;
                    case WISHLIST:
                        wishlistCount++;
                        break;
                    case RATING:
                        totalRating += interaction.getValue();
                        ratingCount++;
                        break;
                }
            }
        }

        product.setViewCount(viewCount);
        product.setPurchaseCount(purchaseCount);
        product.setWishlistCount(wishlistCount);

        if (ratingCount > 0) {
            product.setRating(totalRating / ratingCount);
            product.setReviewCount(ratingCount);
        }
    }

    /**
     * Get product analytics
     */
    public Map<String, Object> getProductAnalytics(int productId) {
        Product product = productMap.get(productId);
        if (product == null) {
            return new HashMap<>();
        }

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("product_id", productId);
        analytics.put("name", product.getName());
        analytics.put("category", product.getCategory());
        analytics.put("brand", product.getBrand());
        analytics.put("price", product.getPrice());
        analytics.put("rating", product.getRating());
        analytics.put("review_count", product.getReviewCount());
        analytics.put("view_count", product.getViewCount());
        analytics.put("purchase_count", product.getPurchaseCount());
        analytics.put("wishlist_count", product.getWishlistCount());
        analytics.put("popularity_score", product.getPopularityScore());

        // Interaction breakdown
        Map<String, Integer> interactionTypes = new HashMap<>();
        for (Interaction interaction : interactions) {
            if (interaction.getProductId() == productId) {
                String type = interaction.getType().toString();
                interactionTypes.merge(type, 1, Integer::sum);
            }
        }
        analytics.put("interaction_breakdown", interactionTypes);

        // Conversion rate (purchases/views)
        double conversionRate = product.getViewCount() > 0 ? 
                               (double) product.getPurchaseCount() / product.getViewCount() : 0.0;
        analytics.put("conversion_rate", conversionRate);

        return analytics;
    }

    /**
     * Get all categories
     */
    public Set<String> getAllCategories() {
        Set<String> categories = new HashSet<>();
        for (Integer productId : productMap.keySet()) {
            Product product = productMap.get(productId);
            if (product != null && product.getCategory() != null) {
                categories.add(product.getCategory());
            }
        }
        return categories;
    }

    /**
     * Get all brands
     */
    public Set<String> getAllBrands() {
        Set<String> brands = new HashSet<>();
        for (Integer productId : productMap.keySet()) {
            Product product = productMap.get(productId);
            if (product != null && product.getBrand() != null) {
                brands.add(product.getBrand());
            }
        }
        return brands;
    }

    /**
     * Get products needing restocking
     */
    public List<Product> getOutOfStockProducts() {
        List<Product> outOfStock = new ArrayList<>();
        for (Integer productId : productMap.keySet()) {
            Product product = productMap.get(productId);
            if (product != null && !product.isInStock()) {
                outOfStock.add(product);
            }
        }
        return outOfStock;
    }

    /**
     * Check if product exists
     */
    public boolean productExists(int productId) {
        return productMap.containsKey(productId);
    }

    /**
     * Get product count
     */
    public int getProductCount() {
        return productMap.size();
    }

    /**
     * Get all product IDs
     */
    public Set<Integer> getAllProductIds() {
        return productMap.keySet();
    }

    /**
     * Delete product (for testing/demo purposes)
     */
    public boolean deleteProduct(int productId) {
        Product removedProduct = productMap.remove(productId);
        return removedProduct != null;
    }
}