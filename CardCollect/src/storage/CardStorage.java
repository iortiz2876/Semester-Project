package storage;

import model.SavedCard;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Simple flat-file persistence layer for the user's saved cards.
// Updated to support per-user storage — each user gets their own collection
// and wishlist files so multiple accounts don't share saved cards.
//
// File naming:  collection_<userId>.txt  /  wishlist_<userId>.txt
//
// Before using any methods, call setCurrentUser(userId) after login so the
// storage layer knows which files to read and write.
public class CardStorage {

    // The currently logged-in user's id, set after login.
    // All file operations use this to determine which files to touch.
    private static String currentUserId = null;

    // Called after login to tell CardStorage which user's files to use.
    // Must be called before any load/save/remove/check methods.
    public static void setCurrentUser(String userId) {
        currentUserId = userId;
    }

    // Returns the current user's id, or null if no one is logged in.
    public static String getCurrentUser() {
        return currentUserId;
    }

    // Builds the filename for this user's collection or wishlist.
    // Falls back to the old single-user filenames if no user is set,
    // so the app doesn't crash if setCurrentUser was missed.
    private static String getCollectionFile() {
        if (currentUserId == null) return "collection.txt";
        return "collection_" + currentUserId + ".txt";
    }

    private static String getWishlistFile() {
        if (currentUserId == null) return "wishlist.txt";
        return "wishlist_" + currentUserId + ".txt";
    }

    // Each card is saved as one line:
    // id|name|imageUrl|rarity|marketPrice

    // -------------------------
    // Collection
    // -------------------------

    // Read the entire collection from disk for the current user.
    public static List<SavedCard> loadCollection() {
        return loadFromFile(getCollectionFile());
    }

    // Add a card to the current user's collection if it isn't already there.
    public static void saveToCollection(SavedCard card) {
        List<SavedCard> collection = loadCollection();
        boolean exists = collection.stream().anyMatch(c -> c.id.equals(card.id));
        if (!exists) {
            collection.add(card);
            saveToFile(getCollectionFile(), collection);
        }
    }

    // Remove a card from the current user's collection by id.
    public static void removeFromCollection(String cardId) {
        List<SavedCard> collection = loadCollection();
        collection.removeIf(c -> c.id.equals(cardId));
        saveToFile(getCollectionFile(), collection);
    }

    // Check if a card is in the current user's collection.
    public static boolean isInCollection(String cardId) {
        return loadCollection().stream().anyMatch(c -> c.id.equals(cardId));
    }

    // -------------------------
    // Wishlist
    // -------------------------

    public static List<SavedCard> loadWishlist() {
        return loadFromFile(getWishlistFile());
    }

    public static void saveToWishlist(SavedCard card) {
        List<SavedCard> wishlist = loadWishlist();
        boolean exists = wishlist.stream().anyMatch(c -> c.id.equals(card.id));
        if (!exists) {
            wishlist.add(card);
            saveToFile(getWishlistFile(), wishlist);
        }
    }

    public static void removeFromWishlist(String cardId) {
        List<SavedCard> wishlist = loadWishlist();
        wishlist.removeIf(c -> c.id.equals(cardId));
        saveToFile(getWishlistFile(), wishlist);
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

        if (!file.exists()) {
            return cards;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
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
