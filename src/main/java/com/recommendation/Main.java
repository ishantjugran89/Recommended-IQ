package com.recommendation;

import com.recommendation.models.*;
import com.recommendation.service.RecommendationService;
import com.recommendation.ui.RecommendationDashboard;
import com.recommendation.utils.DataLoader;
import com.recommendation.utils.MetricsCalculator;
import java.util.*;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;



/**
 * Main application entry point for E-commerce Recommendation System
 * Demonstrates all core DSA concepts and recommendation algorithms
 */
public class Main {
    public static void launchDashboard(RecommendationService service) {
    SwingUtilities.invokeLater(() -> {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("🎨 Launching Visual Dashboard...");
        new RecommendationDashboard(service).setVisible(true);
    });
}
    public static void main(String[] args) {
        System.out.println("=== E-commerce Product Recommendation System ===");
        System.out.println("Showcasing Data Structures and Algorithms\n");

        // Initialize the recommendation system
        RecommendationService recommendationService = new RecommendationService();

        // Load or generate sample data
        loadSampleData(recommendationService);

        // Demonstrate different recommendation algorithms
        demonstrateRecommendations(recommendationService);

        // Show system performance metrics
        showSystemMetrics(recommendationService);

        // Demonstrate DSA concepts
        demonstrateDSAConcepts(recommendationService);

        System.out.println("\n=== Demo Complete ===");

            // Launch visual dashboard
    System.out.println("\n🎨 Launching Visual Dashboard...");
        launchDashboard(recommendationService);

    }

    /**
     * Load sample data for demonstration
     */
    private static void loadSampleData(RecommendationService service) {
        System.out.println("📊 Loading Sample Data...");

        // Generate sample users (demonstrating data loading)
        List<User> users = DataLoader.generateSampleUsers(100);
        service.addUsers(users);
        System.out.println("✅ Generated " + users.size() + " sample users");

        // Generate sample products
        List<Product> products = DataLoader.generateSampleProducts(500);
        service.addProducts(products);
        System.out.println("✅ Generated " + products.size() + " sample products");

        // Generate sample interactions
        List<Interaction> interactions = DataLoader.generateSampleInteractions(users, products, 15);
        service.addInteractions(interactions);
        System.out.println("✅ Generated " + interactions.size() + " sample interactions");

        System.out.println();
    }

    /**
     * Demonstrate different recommendation algorithms
     */
    private static void demonstrateRecommendations(RecommendationService service) {
        System.out.println("🤖 Testing Recommendation Algorithms...");

        // Test user with sufficient interaction history
        int testUserId = 5;

        System.out.println("\n--- User #" + testUserId + " Recommendations ---");

        // 1. Collaborative Filtering
        System.out.println("\n🔗 Collaborative Filtering:");
        List<Recommendation> collaborativeRecs = service.getCollaborativeRecommendations(testUserId, 5);
        printRecommendations(collaborativeRecs);

        // 2. Content-Based Filtering
        System.out.println("\n📝 Content-Based Filtering:");
        List<Recommendation> contentRecs = service.getContentBasedRecommendations(testUserId, 5);
        printRecommendations(contentRecs);

        // 3. Hybrid Approach
        System.out.println("\n🔄 Hybrid Recommendations:");
        List<Recommendation> hybridRecs = service.getHybridRecommendations(testUserId, 5);
        printRecommendations(hybridRecs);

        // 4. Diversified Recommendations
        System.out.println("\n🌈 Diversified Recommendations:");
        List<Recommendation> diversifiedRecs = service.getDiversifiedRecommendations(testUserId, 5);
        printRecommendations(diversifiedRecs);

        // 5. Popular Products (for new users)
        System.out.println("\n🔥 Popular Products:");
        List<Recommendation> popularRecs = service.getPopularRecommendations(5);
        printRecommendations(popularRecs);
    }

    /**
     * Show system performance metrics
     */
    private static void showSystemMetrics(RecommendationService service) {
        System.out.println("\n📈 System Performance Metrics...");

        Map<String, Object> stats = service.getSystemStatistics();

        System.out.println("Users: " + stats.get("total_users"));
        System.out.println("Products: " + stats.get("total_products"));
        System.out.println("Interactions: " + stats.get("total_interactions"));
        System.out.println("Active Users: " + stats.get("active_users"));
        System.out.println("Graph Density: " + String.format("%.4f", (Double) stats.get("graph_density")));

        System.out.println("\n💾 Cache Statistics:");
        System.out.println(service.getCacheStatistics());

        // Test recommendation accuracy with sample metrics
        demonstrateAccuracyMetrics(service);
    }

    /**
     * Demonstrate core DSA concepts used in the system
     */
    private static void demonstrateDSAConcepts(RecommendationService service) {
        System.out.println("\n🧮 Core DSA Concepts Demonstration...");

        // 1. Graph Algorithms
        System.out.println("\n📊 Graph Analysis:");
        System.out.println("Graph: " + service.getGraph().toString());

        // Find similar users using graph traversal
        int sampleUserId = 10;
        List<Integer> similarUsers = service.getGraph().findSimilarUsers(sampleUserId, 2);
        System.out.println("Similar users to #" + sampleUserId + ": " + similarUsers.subList(0, Math.min(5, similarUsers.size())));

        // 2. Hash Table Performance
        System.out.println("\n🗂️ Hash Table Performance:");
        System.out.println("User Map: " + service.getUserMap().getStatistics());
        System.out.println("Product Map: " + service.getProductMap().getStatistics());

        // 3. Priority Queue (Top-K) demonstration
        System.out.println("\n⬆️ Top-K Products (using Priority Queue):");
        List<Integer> topProducts = service.getGraph().getMostPopularProducts(10);
        for (int i = 0; i < Math.min(5, topProducts.size()); i++) {
            Product product = service.getProductMap().get(topProducts.get(i));
            if (product != null) {
                System.out.printf("  %d. %s (Score: %.2f)%n", 
                    i + 1, product.getName(), product.getPopularityScore());
            }
        }

        // 4. Similarity Algorithms
        System.out.println("\n🔢 Similarity Calculation Examples:");
        demonstrateSimilarityCalculations();

        // 5. Performance Analysis
        System.out.println("\n⚡ Algorithm Performance:");
        measureAlgorithmPerformance(service);
    }

    /**
     * Print recommendations in a formatted way
     */
    private static void printRecommendations(List<Recommendation> recommendations) {
        if (recommendations.isEmpty()) {
            System.out.println("  No recommendations available");
            return;
        }

        for (int i = 0; i < Math.min(5, recommendations.size()); i++) {
            Recommendation rec = recommendations.get(i);
            System.out.printf("  %d. Product #%d (Score: %.3f, Algorithm: %s)%n", 
                i + 1, rec.getProductId(), rec.getScore(), rec.getAlgorithm());
            if (rec.getExplanation() != null && !rec.getExplanation().isEmpty()) {
                System.out.printf("     → %s%n", rec.getExplanation());
            }
        }
    }

    /**
     * Demonstrate recommendation accuracy metrics
     */
    private static void demonstrateAccuracyMetrics(RecommendationService service) {
        System.out.println("\n🎯 Accuracy Metrics Demonstration:");

        // Create sample test scenario
        int testUserId = 15;
        List<Recommendation> recommendations = service.getRecommendations(testUserId, 10);

        // Simulate relevant items (in real system, this would be from test data)
        Set<Integer> relevantItems = new HashSet<>();
        for (int i = 0; i < Math.min(5, recommendations.size()); i++) {
            if (Math.random() > 0.5) { // 50% chance of being relevant
                relevantItems.add(recommendations.get(i).getProductId());
            }
        }

        if (!recommendations.isEmpty() && !relevantItems.isEmpty()) {
            double precision = MetricsCalculator.calculatePrecision(recommendations, relevantItems);
            double recall = MetricsCalculator.calculateRecall(recommendations, relevantItems);
            double f1 = MetricsCalculator.calculateF1Score(recommendations, relevantItems);

            System.out.printf("  Precision: %.3f%n", precision);
            System.out.printf("  Recall: %.3f%n", recall);
            System.out.printf("  F1-Score: %.3f%n", f1);
        } else {
            System.out.println("  No test data available for accuracy calculation");
        }
    }

    /**
     * Demonstrate similarity calculation algorithms
     */
    private static void demonstrateSimilarityCalculations() {
        // Create sample user interaction vectors
        Map<Integer, Double> user1 = new HashMap<>();
        user1.put(1, 5.0); user1.put(2, 3.0); user1.put(3, 4.0);

        Map<Integer, Double> user2 = new HashMap<>();
        user2.put(1, 4.0); user2.put(2, 5.0); user2.put(4, 3.0);

        // Calculate similarities
        double cosineSim = com.recommendation.core.algorithms.SimilarityCalculator.cosineSimilarity(user1, user2);

        Set<Integer> set1 = user1.keySet();
        Set<Integer> set2 = user2.keySet();
        double jaccardSim = com.recommendation.core.algorithms.SimilarityCalculator.jaccardSimilarity(set1, set2);

        System.out.printf("  Cosine Similarity: %.3f%n", cosineSim);
        System.out.printf("  Jaccard Similarity: %.3f%n", jaccardSim);
    }

    /**
     * Measure and display algorithm performance
     */
    private static void measureAlgorithmPerformance(RecommendationService service) {
        int testUserId = 20;
        int recommendationCount = 10;

        // Measure collaborative filtering performance
        long startTime = System.currentTimeMillis();
        service.getCollaborativeRecommendations(testUserId, recommendationCount);
        long collaborativeTime = System.currentTimeMillis() - startTime;

        // Measure content-based filtering performance
        startTime = System.currentTimeMillis();
        service.getContentBasedRecommendations(testUserId, recommendationCount);
        long contentTime = System.currentTimeMillis() - startTime;

        // Measure hybrid approach performance
        startTime = System.currentTimeMillis();
        service.getHybridRecommendations(testUserId, recommendationCount);
        long hybridTime = System.currentTimeMillis() - startTime;

        System.out.printf("  Collaborative Filtering: %d ms%n", collaborativeTime);
        System.out.printf("  Content-Based Filtering: %d ms%n", contentTime);
        System.out.printf("  Hybrid Approach: %d ms%n", hybridTime);

        // Memory usage estimation
        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("  Memory Usage: %.2f MB%n", memoryUsed / (1024.0 * 1024.0));
    }

    /**
     * Interactive demo mode (optional)
     */
    public static void runInteractiveDemo(RecommendationService service) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n🎮 Interactive Demo Mode");
        System.out.println("Enter user ID to get recommendations (or 'exit' to quit):");

        while (true) {
            System.out.print("User ID: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                break;
            }

            try {
                int userId = Integer.parseInt(input);

                if (service.getUserMap().containsKey(userId)) {
                    System.out.println("\nRecommendations for User #" + userId + ":");
                    List<Recommendation> recommendations = service.getRecommendations(userId, 5);
                    printRecommendations(recommendations);

                    // Show user activity
                    User user = service.getUserMap().get(userId);
                    System.out.println("\nUser Activity:");
                    System.out.println("  Viewed: " + user.getViewedProducts().size() + " products");
                    System.out.println("  Purchased: " + user.getPurchasedProducts().size() + " products");
                    System.out.println("  Wishlist: " + user.getWishlistProducts().size() + " products");
                } else {
                    System.out.println("User not found. Available users: 1-100");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid user ID or 'exit'");
            }

            System.out.println();
        }

        scanner.close();
        System.out.println("Demo completed!");
    }
}
