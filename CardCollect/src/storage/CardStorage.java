package storage;

import model.SavedCard;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CardStorage {

    private static final String COLLECTION_FILE = "collection.txt";
    private static final String WISHLIST_FILE   = "wishlist.txt";

    // Each card is saved as one line:
    // id|name|imageUrl|rarity|marketPrice

    // -------------------------
    // Collection
    // -------------------------
    public static List<SavedCard> loadCollection() {
        return loadFromFile(COLLECTION_FILE);
    }

    public static void saveToCollection(SavedCard card) {
        List<SavedCard> collection = loadCollection();
        boolean exists = collection.stream().anyMatch(c -> c.id.equals(card.id));
        if (!exists) {
            collection.add(card);
            saveToFile(COLLECTION_FILE, collection);
        }
    }

    public static void removeFromCollection(String cardId) {
        List<SavedCard> collection = loadCollection();
        collection.removeIf(c -> c.id.equals(cardId));
        saveToFile(COLLECTION_FILE, collection);
    }

    public static boolean isInCollection(String cardId) {
        return loadCollection().stream().anyMatch(c -> c.id.equals(cardId));
    }

    // -------------------------
    // Wishlist
    // -------------------------
    public static List<SavedCard> loadWishlist() {
        return loadFromFile(WISHLIST_FILE);
    }

    public static void saveToWishlist(SavedCard card) {
        List<SavedCard> wishlist = loadWishlist();
        boolean exists = wishlist.stream().anyMatch(c -> c.id.equals(card.id));
        if (!exists) {
            wishlist.add(card);
            saveToFile(WISHLIST_FILE, wishlist);
        }
    }

    public static void removeFromWishlist(String cardId) {
        List<SavedCard> wishlist = loadWishlist();
        wishlist.removeIf(c -> c.id.equals(cardId));
        saveToFile(WISHLIST_FILE, wishlist);
    }

    public static boolean isInWishlist(String cardId) {
        return loadWishlist().stream().anyMatch(c -> c.id.equals(cardId));
    }

    // -------------------------
    // File helpers
    // -------------------------
    private static List<SavedCard> loadFromFile(String filename) {
        List<SavedCard> cards = new ArrayList<>();
        File file = new File(filename);

        System.out.println("Looking for file at: " + file.getAbsolutePath()); // debug line

        if (!file.exists()) {
            System.out.println("File not found!"); // debug line
            return cards;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                System.out.println("Read line: " + line); // debug line
                if (parts.length == 5) {
                    cards.add(new SavedCard(parts[0], parts[1], parts[2], parts[3], parts[4]));
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading " + filename + ": " + e.getMessage());
        }

        return cards;
    }

    private static void saveToFile(String filename, List<SavedCard> cards) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (SavedCard card : cards) {
                writer.write(card.id + "|" +
                        card.name + "|" +
                        card.imageUrl + "|" +
                        card.rarity + "|" +
                        card.marketPrice);
                writer.newLine();
            }
        } catch (Exception e) {
            System.out.println("Error saving " + filename + ": " + e.getMessage());
        }
    }
}
