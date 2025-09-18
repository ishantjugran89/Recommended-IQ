# RecommendIQ : A Product Recommendation System

This is a Java-based product recommendation system using collaborative filtering, content-based filtering, and hybrid approaches. The project is structured as a Maven application.

## Features
- Collaborative Filtering
- Content-Based Filtering
- Hybrid Recommendation
- User-Item Graph Analysis
- Custom Data Structures (Heap, HashMap)
- Graph Visualization (UI)

## Project Structure
- `src/main/java/com/recommendation/` - Main source code
- `core/` - Algorithms, graph, data structures
- `models/` - Data models (User, Product, etc.)
- `service/` - Business logic
- `ui/` - User interface components
- `utils/` - Utility classes

## Getting Started
1. **Build the project:**
   ```sh
   mvn clean install
   ```
2. **Run the application:**
   ```sh
   mvn exec:java "-Dexec.mainClass=com.recommendation.Main"
   ```

## Requirements
- Java 8 or higher
- Maven


