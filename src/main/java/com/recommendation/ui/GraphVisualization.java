package com.recommendation.ui;

import com.recommendation.core.graph.UserItemGraph;
import com.recommendation.models.User;
import com.recommendation.models.Product;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.Random;

public class GraphVisualization extends JPanel {
    
    private UserItemGraph graph;
    
    public GraphVisualization(UserItemGraph graph) {
        this.graph = graph;
        setLayout(new BorderLayout());
        createVisualization();
    }
    
    private void createVisualization() {
        // Create a scatter plot to represent user-item relationships
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        XYSeries userSeries = new XYSeries("Users");
        XYSeries productSeries = new XYSeries("Products");
        XYSeries connectionSeries = new XYSeries("Connections");
        
        Random random = new Random(42);
        
        // Plot users
        Set<Integer> users = graph.getAllUsers();
        for (Integer userId : users) {
            double x = random.nextDouble() * 100;
            double y = random.nextDouble() * 50;
            userSeries.add(x, y);
        }
        
        // Plot products
        Set<Integer> products = graph.getAllProducts();
        for (Integer productId : products) {
            double x = random.nextDouble() * 100;
            double y = 50 + random.nextDouble() * 50;
            productSeries.add(x, y);
        }
        
        dataset.addSeries(userSeries);
        dataset.addSeries(productSeries);
        
        JFreeChart chart = ChartFactory.createScatterPlot(
            "User-Item Interaction Graph",
            "X Coordinate", 
            "Y Coordinate",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        ChartPanel chartPanel = new ChartPanel(chart);
        add(chartPanel, BorderLayout.CENTER);
        
        // Add statistics panel
        JPanel statsPanel = new JPanel(new GridLayout(3, 2));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Graph Statistics"));
        
        statsPanel.add(new JLabel("Total Users:"));
        statsPanel.add(new JLabel(String.valueOf(graph.getTotalUsers())));
        
        statsPanel.add(new JLabel("Total Products:"));
        statsPanel.add(new JLabel(String.valueOf(graph.getTotalProducts())));
        
        statsPanel.add(new JLabel("Total Interactions:"));
        statsPanel.add(new JLabel(String.valueOf(graph.getTotalEdges())));
        
        add(statsPanel, BorderLayout.SOUTH);
    }
}
