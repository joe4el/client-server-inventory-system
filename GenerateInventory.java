import java.io.*;
import java.util.Random;

/**
 * GenerateInventory.java
 * Generates 4 inventory files seeded with student ID: 2199
 * Each file contains 30+ product records in format: ProductID, ProductName, Price
 * One special record per file embeds the student ID as required by project spec.
 */
public class GenerateInventory {

    // ── Personalization ────────────────────────────────────────────────
    private static final int STUDENT_ID = 2199;
    // ──────────────────────────────────────────────────────────────────

    // Product name pools for each category
    private static final String[] ELECTRONICS = {
        "iPhone 16 Pro", "Xiaomi 14 Ultra", "Samsung Galaxy S25", "Blackberry Bold 9900",
        "Webcam", "USB Hub", "Monitor Stand", "Laptop Cooling Pad",
        "Bluetooth Speaker", "Samsung Galaxy Buds", "Xiaomi Smart Watch",
        "iPhone MagSafe Charger", "LED Desk Lamp", "Graphics Tablet", "VR Headset",
        "Blackberry Passport", "Drone", "Samsung Smart Plug", "Digital Thermometer",
        "Laser Pointer", "Xiaomi Mini Projector", "Card Reader", "Cable Organizer",
        "iPhone Screen Protector", "Phone Stand", "Xiaomi Stylus Pen", "Solar Charger",
        "Samsung Fingerprint Scanner", "Blackberry KEYone", "Xiaomi Smart Ring"
    };

    private static final String[] CLOTHING = {
        "Louis Vuitton Monogram Tote", "Versace Medusa T-Shirt", "Baggy Jeans", "Prada Nylon Jacket",
        "Louis Vuitton Belt", "Versace Baroque Shirt", "Baggy Cargo Pants", "Prada Wool Coat",
        "Louis Vuitton Hoodie", "Versace Gold Polo", "Running Shorts", "Prada Cap",
        "Baseball Cap", "Louis Vuitton Scarf", "Raincoat", "Versace Dress Shirt",
        "Swim Trunks", "Thermal Underwear", "Baggy Bomber Jacket", "Prada Chino Pants",
        "Tank Top", "Louis Vuitton Shorts", "Fleece Pullover", "Versace Puffer Vest",
        "Cycling Jersey", "Baggy Hiking Pants", "Prada Graphic Tee", "Versace Cardigan",
        "Louis Vuitton Windbreaker", "Prada Trench Coat"
    };

    private static final String[] GROCERIES = {
        "Organic Milk", "Whole Wheat Bread", "Free Range Eggs", "Olive Oil",
        "Greek Yogurt", "Cheddar Cheese", "Chicken Breast", "Salmon Fillet",
        "Brown Rice", "Pasta", "Tomato Sauce", "Mixed Nuts",
        "Granola Bar", "Orange Juice", "Green Tea", "Dark Chocolate",
        "Almond Butter", "Honey", "Avocado", "Baby Spinach",
        "Cherry Tomatoes", "Blueberries", "Oat Flakes", "Protein Powder",
        "Coconut Water", "Sparkling Water", "Black Coffee", "Quinoa",
        "Lentil Soup", "Canned Tuna"
    };

    private static final String[] BOOKS = {
        "Harry Potter and the Sorcerers Stone", "Harry Potter and the Chamber of Secrets",
        "Harry Potter and the Prisoner of Azkaban", "Harry Potter and the Goblet of Fire",
        "Harry Potter and the Order of the Phoenix", "Harry Potter and the Half-Blood Prince",
        "Harry Potter and the Deathly Hallows", "The Hobbit",
        "The Lord of the Rings: Fellowship", "The Lord of the Rings: Two Towers",
        "The Lord of the Rings: Return of the King", "Home Alone: The Novelization",
        "Clean Code", "The Pragmatic Programmer", "Design Patterns", "Refactoring",
        "Introduction to Algorithms", "Computer Networks", "Operating Systems",
        "Database System Concepts", "Artificial Intelligence", "Deep Learning",
        "Python Crash Course", "Java: The Complete Reference", "Head First Java",
        "Effective Java", "Spring in Action", "Microservices Patterns",
        "The Phoenix Project", "Site Reliability Engineering"
    };

    public static void main(String[] args) {
        generateFile("Electronics.txt", ELECTRONICS, "E");
        generateFile("Clothing.txt",    CLOTHING,    "C");
        generateFile("Groceries.txt",   GROCERIES,   "G");
        generateFile("Books.txt",       BOOKS,       "B");
        System.out.println("All inventory files generated successfully.");
        System.out.println("Student ID embedded: " + STUDENT_ID);
    }

    /**
     * Generates one inventory file.
     * Uses STUDENT_ID as the Random seed so output is reproducible and unique.
     *
     * @param filename   Output file name
     * @param names      Array of product name candidates for this category
     * @param prefix     Single-letter prefix for ProductIDs (E, C, G, B)
     */
    private static void generateFile(String filename, String[] names, String prefix) {
        // Seed = STUDENT_ID XOR hashCode of filename → unique per file, tied to student
        Random rng = new Random((long) STUDENT_ID ^ filename.hashCode());

        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {

            for (int i = 0; i < 30; i++) {
                String productID   = prefix + String.format("%04d", 1000 + i);
                String productName = names[i % names.length];
                // Price: between 1.00 and 199.99, seeded so it's reproducible
                double price = 1.0 + (rng.nextInt(19899)) / 100.0;
                pw.printf("%s, %s, %.2f%n", productID, productName, price);
            }

            // ── Special personalization record (required by spec) ──────────
            pw.printf("P9999, StudentID_%d_Demo, 0.01%n", STUDENT_ID);
            // ──────────────────────────────────────────────────────────────

            System.out.println("Generated: " + filename);

        } catch (IOException e) {
            System.err.println("Error writing " + filename + ": " + e.getMessage());
        }
    }
}
