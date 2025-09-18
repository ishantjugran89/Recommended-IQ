package com.recommendation.utils;

import com.recommendation.models.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility class for loading data from CSV files and generating sample data
 */
public class DataLoader {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Load users from CSV file
     * Format: user_id,username,email,registration_date
     */
    public static List<User> loadUsersFromCSV(String filePath) {
        List<User> users = new ArrayList<>();

        try (FileReader fileReader = new FileReader(filePath);
             CSVReader csvReader = new CSVReaderBuilder(fileReader).withSkipLines(1).build()) {

            String[] values;
            while ((values = csvReader.readNext()) != null) {
                try {
                    int userId = Integer.parseInt(values[0]);
                    String username = values[1];
                    String email = values[2];

                    User user = new User(userId, username, email);

                    if (values.length > 3) {
                        try {
                            Date registrationDate = DATE_FORMAT.parse(values[3]);
                            user.setRegistrationDate(registrationDate);
                        } catch (ParseException e) {
                            // Use default date if parsing fails
                        }
                    }

                    users.add(user);
                } catch (Exception e) {
                    System.err.println("Error parsing user row: " + Arrays.toString(values));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading users from CSV: " + e.getMessage());
        }

        return users;
    }

    /**
     * Load products from CSV file
     * Format: product_id,name,category,brand,price,rating,review_count,description
     */
    public static List<Product> loadProductsFromCSV(String filePath) {
        List<Product> products = new ArrayList<>();

        try (FileReader fileReader = new FileReader(filePath);
             CSVReader csvReader = new CSVReaderBuilder(fileReader).withSkipLines(1).build()) {

            String[] values;
            while ((values = csvReader.readNext()) != null) {
                try {
                    int productId = Integer.parseInt(values[0]);
                    String name = values[1];
                    String category = values[2];
                    String brand = values[3];
                    double price = Double.parseDouble(values[4]);

                    Product product = new Product(productId, name, category, brand, price);

                    if (values.length > 5) {
                        product.setRating(Double.parseDouble(values[5]));
                    }

                    if (values.length > 6) {
                        product.setReviewCount(Integer.parseInt(values[6]));
                    }

                    if (values.length > 7) {
                        product.setDescription(values[7]);
                    }

                    products.add(product);
                } catch (Exception e) {
                    System.err.println("Error parsing product row: " + Arrays.toString(values));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading products from CSV: " + e.getMessage());
        }

        return products;
    }

    /**
     * Load interactions from CSV file
     * Format: user_id,product_id,interaction_type,value,timestamp,context
     */
    public static List<Interaction> loadInteractionsFromCSV(String filePath) {
        List<Interaction> interactions = new ArrayList<>();

        try (FileReader fileReader = new FileReader(filePath);
             CSVReader csvReader = new CSVReaderBuilder(fileReader).withSkipLines(1).build()) {

            String[] values;
            while ((values = csvReader.readNext()) != null) {
                try {
                    int userId = Integer.parseInt(values[0]);
                    int productId = Integer.parseInt(values[1]);
                    String typeString = values[2].toUpperCase();
                    Interaction.InteractionType type = Interaction.InteractionType.valueOf(typeString);

                    Interaction interaction = new Interaction(userId, productId, type);

                    if (values.length > 3 && !values[3].isEmpty()) {
                        interaction.setValue(Double.parseDouble(values[3]));
                    }

                    if (values.length > 4 && !values[4].isEmpty()) {
                        try {
                            Date timestamp = DATE_FORMAT.parse(values[4]);
                            interaction.setTimestamp(timestamp);
                        } catch (ParseException e) {
                            // Use current time if parsing fails
                        }
                    }

                    if (values.length > 5) {
                        interaction.setContext(values[5]);
                    }

                    interactions.add(interaction);
                } catch (Exception e) {
                    System.err.println("Error parsing interaction row: " + Arrays.toString(values));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading interactions from CSV: " + e.getMessage());
        }

        return interactions;
    }

    /**
     * Generate sample users for testing
     */
    public static List<User> generateSampleUsers(int count) {
        List<User> users = new ArrayList<>();
        String[] firstNames = {"John", "Jane", "Mike", "Sarah", "David", "Lisa", "Chris", "Emma", 
                              "Alex", "Maria", "James", "Anna", "Robert", "Emily", "Daniel"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
                             "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez"};

        Random random = new Random(42); // Fixed seed for reproducibility

        for (int i = 1; i <= count; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String username = firstName.toLowerCase() + "_" + lastName.toLowerCase() + "_" + i;
            String email = username + "@example.com";

            User user = new User(i, username, email);

            // Add some random preferences
            String[] categories = {"Electronics", "Clothing", "Books", "Sports", "Home", "Beauty"};
            for (String category : categories) {
                if (random.nextDouble() < 0.3) { // 30% chance to have preference for each category
                    user.updateCategoryPreference(category, random.nextDouble());
                }
            }

            users.add(user);
        }

        return users;
    }

    /**
     * Generate sample products for testing
     */
    public static List<Product> generateSampleProducts(int count) {
        List<Product> products = new ArrayList<>();

        String[] categories = {"Electronics", "Clothing", "Books", "Sports", "Home", "Beauty"};
        String[] brands = {"Apple", "Samsung", "Nike", "Adidas", "Zara", "H&M", "Amazon Basics", 
                          "Sony", "LG", "Dell", "HP", "Canon", "Nikon"};

        String[][] productNames = {
            {"iPhone", "MacBook", "iPad", "AirPods", "Apple Watch"},
            {"Galaxy Phone", "Galaxy Tab", "Smart TV", "Headphones", "Monitor"},
            {"Running Shoes", "Sports Jacket", "Gym Bag", "Fitness Tracker", "Tennis Racket"},
            {"Novel", "Cookbook", "Biography", "Science Book", "Art Book"},
            {"Sofa", "Coffee Table", "Lamp", "Cushions", "Curtains"},
            {"Skincare", "Makeup", "Perfume", "Hair Care", "Body Lotion"}
        };

        Random random = new Random(42);

        for (int i = 1; i <= count; i++) {
            String category = categories[random.nextInt(categories.length)];
            String brand = brands[random.nextInt(brands.length)];

            int categoryIndex = Arrays.asList(categories).indexOf(category);
            String[] names = productNames[Math.min(categoryIndex, productNames.length - 1)];
            String productName = names[random.nextInt(names.length)] + " " + brand + " " + i;

            double basePrice = 100 + random.nextDouble() * 900; // $100 to $1000
            double price = Math.round(basePrice * 100.0) / 100.0; // Round to 2 decimal places

            Product product = new Product(i, productName, category, brand, price);

            // Set random rating and review count
            if (random.nextDouble() < 0.8) { // 80% of products have ratings
                double rating = 2.0 + random.nextDouble() * 3.0; // 2.0 to 5.0
                product.setRating(Math.round(rating * 10.0) / 10.0);
                product.setReviewCount(random.nextInt(500) + 1);
            }

            // Set random view and purchase counts
            product.setViewCount(random.nextInt(1000));
            product.setPurchaseCount(random.nextInt(100));
            product.setWishlistCount(random.nextInt(50));

            // Add some tags
            String[] possibleTags = {"popular", "trending", "bestseller", "new", "sale", "premium"};
            for (String tag : possibleTags) {
                if (random.nextDouble() < 0.2) { // 20% chance for each tag
                    product.addTag(tag);
                }
            }

            product.setDescription("High-quality " + category.toLowerCase() + " from " + brand);

            products.add(product);
        }

        return products;
    }

    /**
     * Generate sample interactions for testing
     */
    public static List<Interaction> generateSampleInteractions(List<User> users, List<Product> products, 
                                                              int interactionsPerUser) {
        List<Interaction> interactions = new ArrayList<>();
        Random random = new Random(42);

        Interaction.InteractionType[] types = Interaction.InteractionType.values();
        double[] typeWeights = {0.5, 0.15, 0.15, 0.1, 0.05, 0.03, 0.02}; // VIEW, PURCHASE, WISHLIST, RATING, etc.

        for (User user : users) {
            Set<Integer> interactedProducts = new HashSet<>();
            int numInteractions = Math.max(1, random.nextInt(interactionsPerUser * 2));

            for (int i = 0; i < numInteractions; i++) {
                // Select random product
                Product product = products.get(random.nextInt(products.size()));

                // Select interaction type based on weights
                Interaction.InteractionType type = selectWeightedType(types, typeWeights, random);

                Interaction interaction = new Interaction(user.getUserId(), product.getProductId(), type);

                // Set value for rating interactions
                if (type == Interaction.InteractionType.RATING) {
                    double rating = 1.0 + random.nextDouble() * 4.0; // 1.0 to 5.0
                    interaction.setValue(Math.round(rating * 10.0) / 10.0);
                }

                // Set timestamp (random time in last 30 days)
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                long randomTime = thirtyDaysAgo + (long) (random.nextDouble() * 30 * 24 * 60 * 60 * 1000);
                interaction.setTimestamp(new Date(randomTime));

                interactions.add(interaction);
                interactedProducts.add(product.getProductId());

                // Update user object
                switch (type) {
                    case VIEW:
                        user.addViewedProduct(product.getProductId());
                        break;
                    case PURCHASE:
                        user.addPurchasedProduct(product.getProductId());
                        break;
                    case WISHLIST:
                        user.addToWishlist(product.getProductId());
                        break;
                }
            }
        }

        return interactions;
    }

    /**
     * Select interaction type based on weights
     */
    private static Interaction.InteractionType selectWeightedType(Interaction.InteractionType[] types,
                                                                 double[] weights, Random random) {
        double totalWeight = Arrays.stream(weights).sum();
        double randomValue = random.nextDouble() * totalWeight;

        double cumulativeWeight = 0.0;
        for (int i = 0; i < types.length && i < weights.length; i++) {
            cumulativeWeight += weights[i];
            if (randomValue <= cumulativeWeight) {
                return types[i];
            }
        }

        return types[0]; // Fallback to first type
    }

    /**
     * Save users to CSV file
     */
    public static void saveUsersToCSV(List<User> users, String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("user_id,username,email,registration_date,total_interactions");

            for (User user : users) {
                writer.printf("%d,%s,%s,%s,%d%n",
                    user.getUserId(),
                    user.getUsername(),
                    user.getEmail(),
                    DATE_FORMAT.format(user.getRegistrationDate()),
                    user.getTotalInteractions()
                );
            }
        } catch (IOException e) {
            System.err.println("Error saving users to CSV: " + e.getMessage());
        }
    }

    /**
     * Save products to CSV file
     */
    public static void saveProductsToCSV(List<Product> products, String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("product_id,name,category,brand,price,rating,review_count,description,view_count,purchase_count");

        for (Product product : products) {
            writer.printf("%d,\"%s\",%s,%s,%.2f,%.1f,%d,\"%s\",%d,%d%n",
                product.getProductId(),
                product.getName().replace("\"", "\"\""), // Escape quotes
                product.getCategory(),
                product.getBrand(),
                product.getPrice(),
                product.getRating(),
                product.getReviewCount(),
                product.getDescription() != null ? product.getDescription().replace("\"", "\"\"") : "",
                product.getViewCount(),
                product.getPurchaseCount());
        }
    } catch (IOException e) {
        System.err.println("Error saving products to CSV: " + e.getMessage());
    }
}

    /**
     * Save interactions to CSV file
     */
    public static void saveInteractionsToCSV(List<Interaction> interactions, String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("user_id,product_id,interaction_type,value,timestamp,context");

            for (Interaction interaction : interactions) {
                writer.printf("%d,%d,%s,%.1f,%s,\"%s\"%n",
                    interaction.getUserId(),
                    interaction.getProductId(),
                    interaction.getType().toString(),
                    interaction.getValue(),
                    DATE_FORMAT.format(interaction.getTimestamp()),
                    interaction.getContext() != null ? interaction.getContext().replace("\"", "\"\"") : ""
                );
            }
        } catch (IOException e) {
            System.err.println("Error saving interactions to CSV: " + e.getMessage());
        }
    }
}
