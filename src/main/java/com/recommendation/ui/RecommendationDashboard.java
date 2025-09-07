package com.recommendation.ui;

import com.recommendation.models.*;
import com.recommendation.service.RecommendationService;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * Visual Dashboard for E-commerce Recommendation System
 * Shows user interactions and recommendations with category-based pie charts
 */
public class RecommendationDashboard extends JFrame {
    
    private RecommendationService recommendationService;
    private JComboBox<String> userComboBox;
    private JTable userInteractionTable;
    private JTable recommendationTable;
    private JPanel chartPanel;
    private JTextArea explanationArea;
    
    public RecommendationDashboard(RecommendationService service) {
        this.recommendationService = service;
        initializeComponents();
        setupLayout();
        loadInitialData();
    }
    
    private void initializeComponents() {
        setTitle("E-commerce Recommendation System - Visual Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // User selection
        userComboBox = new JComboBox<>();
        userComboBox.addActionListener(new UserSelectionListener());
        
        // Tables
        userInteractionTable = new JTable();
        recommendationTable = new JTable();
        
        // Chart panel
        chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBackground(Color.WHITE);
        
        // Explanation area
        explanationArea = new JTextArea(10, 30);
        explanationArea.setEditable(false);
        explanationArea.setFont(new Font("Arial", Font.PLAIN, 12));
        explanationArea.setBackground(new Color(248, 248, 248));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Top panel - User selection and controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(new JLabel("Select User:"));
        topPanel.add(userComboBox);
        
        JButton refreshButton = new JButton("Generate Recommendations");
        refreshButton.setBackground(new Color(70, 130, 180));
        refreshButton.setForeground(Color.BLACK);
        refreshButton.addActionListener(e -> generateRecommendations());
        topPanel.add(refreshButton);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Main content - Split into sections
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // Left side - User interaction data
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("User Interaction History"));
        leftPanel.add(new JScrollPane(userInteractionTable), BorderLayout.CENTER);
        
        // Right side - Recommendations and analysis
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Top right - Recommendations table
        JPanel recPanel = new JPanel(new BorderLayout());
        recPanel.setBorder(BorderFactory.createTitledBorder("Current Recommendations"));
        recPanel.add(new JScrollPane(recommendationTable), BorderLayout.CENTER);
        
        // Bottom right - Charts and explanations in tabs
        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.addTab("📊 Category Analysis", chartPanel);
        rightTabs.addTab("📝 Detailed Explanation", new JScrollPane(explanationArea));
        
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setTopComponent(recPanel);
        rightSplit.setBottomComponent(rightTabs);
        rightSplit.setDividerLocation(300);
        
        rightPanel.add(rightSplit, BorderLayout.CENTER);
        
        mainSplit.setLeftComponent(leftPanel);
        mainSplit.setRightComponent(rightPanel);
        mainSplit.setDividerLocation(450);
        
        add(mainSplit, BorderLayout.CENTER);
    }
    
    private void loadInitialData() {
        // Populate user dropdown
        Set<Integer> userIds = recommendationService.getUserMap().keySet();
        List<Integer> sortedUserIds = new ArrayList<>(userIds);
        Collections.sort(sortedUserIds);
        
        for (Integer userId : sortedUserIds) {
            User user = recommendationService.getUserMap().get(userId);
            if (user != null) {
                userComboBox.addItem("User " + userId + " (" + user.getUsername() + ")");
            }
        }
        
        if (userComboBox.getItemCount() > 0) {
            userComboBox.setSelectedIndex(0);
            loadUserData(getUserIdFromSelection());
        }
    }
    
    private int getUserIdFromSelection() {
        String selected = (String) userComboBox.getSelectedItem();
        if (selected != null) {
            return Integer.parseInt(selected.split(" ")[1]);
        }
        return 1;
    }
    
    private void loadUserData(int userId) {
        User user = recommendationService.getUserMap().get(userId);
        if (user == null) return;
        
        // Load interaction history table
        String[] columns = {"Product ID", "Product Name", "Category", "Interaction Type", "Rating"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        List<Interaction> interactions = recommendationService.getInteractions();
        for (Interaction interaction : interactions) {
            if (interaction.getUserId() == userId) {
                Product product = recommendationService.getProductMap().get(interaction.getProductId());
                String productName = product != null ? product.getName() : "Unknown Product";
                String category = product != null ? product.getCategory() : "Unknown";
                
                model.addRow(new Object[]{
                    interaction.getProductId(),
                    productName,
                    category,
                    interaction.getType().toString(),
                    interaction.getValue() > 0 ? String.format("%.1f", interaction.getValue()) : "-"
                });
            }
        }
        
        userInteractionTable.setModel(model);
        
        // Show initial user interaction pie chart
        createUserInteractionPieChart(user);
    }
    
    private void generateRecommendations() {
        int userId = getUserIdFromSelection();
        List<Recommendation> recommendations = recommendationService.getHybridRecommendations(userId, 10);
        User user = recommendationService.getUserMap().get(userId);
        
        // Load recommendations table
        String[] columns = {"Rank", "Product ID", "Product Name", "Category", "Score", "Algorithm", "Explanation"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        for (int i = 0; i < recommendations.size(); i++) {
            Recommendation rec = recommendations.get(i);
            Product product = recommendationService.getProductMap().get(rec.getProductId());
            
            model.addRow(new Object[]{
                i + 1,
                rec.getProductId(),
                product != null ? product.getName() : "Unknown Product",
                product != null ? product.getCategory() : "Unknown",
                String.format("%.3f", rec.getScore()),
                rec.getAlgorithm(),
                rec.getExplanation()
            });
        }
        
        recommendationTable.setModel(model);
        
        // Create side-by-side comparison charts
        createComparisonCharts(user, recommendations);
        
        // Generate detailed explanation
        generateExplanation(userId, recommendations);
    }
    
    /**
     * Create pie chart showing user's interaction history by product category
     */
    private void createUserInteractionPieChart(User user) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        Map<String, Integer> categoryCounts = new HashMap<>();
        
        // Count user interactions by product category
        List<Interaction> interactions = recommendationService.getInteractions();
        for (Interaction interaction : interactions) {
            if (interaction.getUserId() == user.getUserId()) {
                Product product = recommendationService.getProductMap().get(interaction.getProductId());
                if (product != null) {
                    String category = product.getCategory();
                    categoryCounts.merge(category, 1, Integer::sum);
                }
            }
        }
        
        // Add data to pie chart
        if (categoryCounts.isEmpty()) {
            dataset.setValue("No Interactions Yet", 1);
        } else {
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                dataset.setValue(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            }
        }
        
        // Create pie chart
        JFreeChart chart = ChartFactory.createPieChart(
            "User's Interaction History by Category",
            dataset, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        
        // Update chart panel
        chartPanel.removeAll();
        chartPanel.add(new ChartPanel(chart), BorderLayout.CENTER);
        chartPanel.revalidate();
        chartPanel.repaint();
    }
    
    /**
     * Create side-by-side comparison of user interactions vs recommendations by category
     */
    private void createComparisonCharts(User user, List<Recommendation> recommendations) {
        // Create datasets for both charts
        DefaultPieDataset userDataset = new DefaultPieDataset();
        DefaultPieDataset recDataset = new DefaultPieDataset();
        
        // User interactions by category
        Map<String, Integer> userCategoryCounts = new HashMap<>();
        List<Interaction> interactions = recommendationService.getInteractions();
        for (Interaction interaction : interactions) {
            if (interaction.getUserId() == user.getUserId()) {
                Product product = recommendationService.getProductMap().get(interaction.getProductId());
                if (product != null) {
                    userCategoryCounts.merge(product.getCategory(), 1, Integer::sum);
                }
            }
        }
        
        // Add user interaction data
        if (userCategoryCounts.isEmpty()) {
            userDataset.setValue("No Past Interactions", 1);
        } else {
            for (Map.Entry<String, Integer> entry : userCategoryCounts.entrySet()) {
                userDataset.setValue(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            }
        }
        
        // Recommendations by category
        Map<String, Integer> recCategoryCounts = new HashMap<>();
        for (Recommendation rec : recommendations) {
            Product product = recommendationService.getProductMap().get(rec.getProductId());
            if (product != null) {
                recCategoryCounts.merge(product.getCategory(), 1, Integer::sum);
            }
        }
        
        // Add recommendation data
        if (recCategoryCounts.isEmpty()) {
            recDataset.setValue("No Recommendations", 1);
        } else {
            for (Map.Entry<String, Integer> entry : recCategoryCounts.entrySet()) {
                recDataset.setValue(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            }
        }
        
        // Create both pie charts
        JFreeChart userChart = ChartFactory.createPieChart(
            "User's Past Interactions by Category", userDataset, true, true, false);
        JFreeChart recChart = ChartFactory.createPieChart(
            "Current Recommendations by Category", recDataset, true, true, false);
        
        // Set chart backgrounds
        userChart.setBackgroundPaint(Color.WHITE);
        recChart.setBackgroundPaint(Color.WHITE);
        
        // Create panel with both charts side by side
        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        chartsPanel.setBackground(Color.WHITE);
        chartsPanel.add(new ChartPanel(userChart));
        chartsPanel.add(new ChartPanel(recChart));
        
        // Create comparison analysis text
        JPanel analysisPanel = new JPanel(new BorderLayout());
        analysisPanel.setBackground(Color.WHITE);
        
        JTextArea comparisonText = new JTextArea(8, 50);
        comparisonText.setEditable(false);
        comparisonText.setFont(new Font("Arial", Font.PLAIN, 11));
        comparisonText.setBackground(new Color(248, 248, 248));
        comparisonText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("📊 CATEGORY COMPARISON ANALYSIS:\n");
        analysis.append(String.join("", Collections.nCopies(50, "="))).append("\n\n");
        
        // Find matching categories
        Set<String> userCategories = userCategoryCounts.keySet();
        Set<String> recCategories = recCategoryCounts.keySet();
        Set<String> commonCategories = new HashSet<>(userCategories);
        commonCategories.retainAll(recCategories);
        
        analysis.append("✅ MATCHING CATEGORIES: ").append(commonCategories.size()).append("\n");
        if (commonCategories.isEmpty()) {
            analysis.append("   No matching categories - exploring new interests!\n");
        } else {
            for (String category : commonCategories) {
                int userCount = userCategoryCounts.get(category);
                int recCount = recCategoryCounts.get(category);
                analysis.append("   • ").append(category).append(": ")
                        .append(userCount).append(" past interactions → ")
                        .append(recCount).append(" recommendations\n");
            }
        }
        
        // Find new categories in recommendations
        Set<String> newCategories = new HashSet<>(recCategories);
        newCategories.removeAll(userCategories);
        if (!newCategories.isEmpty()) {
            analysis.append("\n🔍 NEW CATEGORIES DISCOVERED: ").append(newCategories.size()).append("\n");
            for (String category : newCategories) {
                int recCount = recCategoryCounts.get(category);
                analysis.append("   • ").append(category).append(" (").append(recCount).append(" recommendations - broadening horizons!)\n");
            }
        }
        
        // Calculate relevance score
        double relevanceScore = userCategories.isEmpty() ? 0.0 : 
                               (double) commonCategories.size() / userCategories.size();
        analysis.append("\n📈 PERSONALIZATION SCORE: ").append(String.format("%.1f%%", relevanceScore * 100));
        analysis.append(" (how well recommendations match past behavior)\n");
        
        // Calculate diversity score
        double diversityScore = recCategories.isEmpty() ? 0.0 :
                               (double) newCategories.size() / recCategories.size();
        analysis.append("🌟 DISCOVERY SCORE: ").append(String.format("%.1f%%", diversityScore * 100));
        analysis.append(" (how much the system encourages exploration)");
        
        comparisonText.setText(analysis.toString());
        
        // Combine charts and analysis
        analysisPanel.add(chartsPanel, BorderLayout.CENTER);
        analysisPanel.add(new JScrollPane(comparisonText), BorderLayout.SOUTH);
        
        // Update main chart panel
        chartPanel.removeAll();
        chartPanel.add(analysisPanel, BorderLayout.CENTER);
        chartPanel.revalidate();
        chartPanel.repaint();
    }
    
    private void generateExplanation(int userId, List<Recommendation> recommendations) {
        StringBuilder explanation = new StringBuilder();
        User user = recommendationService.getUserMap().get(userId);
        
        explanation.append("=== DETAILED RECOMMENDATION ANALYSIS ===\n");
        explanation.append(String.join("", Collections.nCopies(50, "="))).append("\n\n");
        
        explanation.append("👤 USER PROFILE:\n");
        explanation.append("User: ").append(user.getUsername()).append(" (ID: ").append(userId).append(")\n");
        explanation.append("Registration Date: ").append(user.getRegistrationDate()).append("\n");
        explanation.append("Total Interactions: ").append(user.getTotalInteractions()).append("\n\n");
        
        explanation.append("📊 USER BEHAVIOR BREAKDOWN:\n");
        explanation.append("• Viewed Products: ").append(user.getViewedProducts().size()).append("\n");
        explanation.append("• Purchased Products: ").append(user.getPurchasedProducts().size()).append("\n");
        explanation.append("• Wishlist Items: ").append(user.getWishlistProducts().size()).append("\n\n");
        
        // Calculate conversion rate
        int totalViews = user.getViewedProducts().size();
        int totalPurchases = user.getPurchasedProducts().size();
        double conversionRate = totalViews > 0 ? (double) totalPurchases / totalViews * 100 : 0;
        explanation.append("💰 Purchase Conversion Rate: ").append(String.format("%.1f%%", conversionRate)).append("\n\n");
        
        explanation.append("🤖 RECOMMENDATION STRATEGY:\n");
        Map<String, Integer> algorithmCounts = new HashMap<>();
        for (Recommendation rec : recommendations) {
            algorithmCounts.merge(rec.getAlgorithm(), 1, Integer::sum);
        }
        
        for (Map.Entry<String, Integer> entry : algorithmCounts.entrySet()) {
            explanation.append("• ").append(entry.getKey()).append(": ")
                      .append(entry.getValue()).append(" recommendations\n");
        }
        
        explanation.append("\n🎯 TOP 5 RECOMMENDATIONS ANALYSIS:\n");
        for (int i = 0; i < Math.min(5, recommendations.size()); i++) {
            Recommendation rec = recommendations.get(i);
            Product product = recommendationService.getProductMap().get(rec.getProductId());
            
            explanation.append("\n").append(i + 1).append(". ");
            explanation.append(product != null ? product.getName() : "Product " + rec.getProductId()).append("\n");
            explanation.append("   📊 Score: ").append(String.format("%.3f", rec.getScore())).append("/1.000\n");
            explanation.append("   🏷️ Category: ").append(product != null ? product.getCategory() : "Unknown").append("\n");
            explanation.append("   🤖 Algorithm: ").append(rec.getAlgorithm()).append("\n");
            explanation.append("   💡 Reason: ").append(rec.getExplanation()).append("\n");
            
            // Check for potential issues
            if (user.hasInteractedWith(rec.getProductId())) {
                explanation.append("   ⚠️  WARNING: User already interacted with this product!\n");
            }
            
            if (product != null && product.getPrice() > 0) {
                explanation.append("   💲 Price: $").append(String.format("%.2f", product.getPrice())).append("\n");
            }
        }
        
        explanation.append("\n📈 SYSTEM PERFORMANCE INSIGHTS:\n");
        explanation.append("• Total recommendations generated: ").append(recommendations.size()).append("\n");
        
        // Check for diversity
        Set<String> uniqueCategories = new HashSet<>();
        for (Recommendation rec : recommendations) {
            Product product = recommendationService.getProductMap().get(rec.getProductId());
            if (product != null) {
                uniqueCategories.add(product.getCategory());
            }
        }
        explanation.append("• Category diversity: ").append(uniqueCategories.size()).append(" different categories\n");
        explanation.append("• Average recommendation score: ");
        
        double avgScore = recommendations.stream().mapToDouble(Recommendation::getScore).average().orElse(0.0);
        explanation.append(String.format("%.3f", avgScore)).append("\n");
        
        explanationArea.setText(explanation.toString());
    }
    
    private class UserSelectionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int userId = getUserIdFromSelection();
            loadUserData(userId);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // This would normally be passed from your main application
            RecommendationService service = new RecommendationService();
            // Load your sample data here...
            
            new RecommendationDashboard(service).setVisible(true);
        });
    }
}
