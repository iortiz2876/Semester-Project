package storage;

import model.SavedCard;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Simple flat-file persistence layer for the user's saved cards.
// Stores the collection and wishlist as plain text files in the working directory,
// one card per line, with fields separated by "|".
//
// Format per line:  id|name|imageUrl|rarity|marketPrice
//
// This is intentionally lightweight — no database, no JSON library, no schema.
// Trade-off: any "|" character inside a card field would corrupt the format.
// Pokemon card data doesn't normally contain pipes, so this works in practice.
public class CardStorage {

    private static final String COLLECTION_FILE = "collection.txt";
    private static final String WISHLIST_FILE   = "wishlist.txt";

    // -------------------------
    // Collection
    // -------------------------

    // Read the entire collection from disk. Returns an empty list if the file doesn't exist yet.
    public static List<SavedCard> loadCollection() {
        return loadFromFile(COLLECTION_FILE);
    }

    // Add a card to the collection if it isn't already there. No-op for duplicates,
    // so callers don't have to check first.
    public static void saveToCollection(SavedCard card) {
        List<SavedCard> collection = loadCollection();
        // Dedupe by id — same card from a different search shouldn't get saved twice
        boolean exists = collection.stream().anyMatch(c -> c.id.equals(card.id));
        if (!exists) {
            collection.add(card);
            saveToFile(COLLECTION_FILE, collection);
        }
    }

    // Remove a card from the collection by id. Silently does nothing if it's not present.
    // Note: this rewrites the entire file every time — fine for small collections,
    // but worth knowing if it ever grows large.
    public static void removeFromCollection(String cardId) {
        List<SavedCard> collection = loadCollection();
        collection.removeIf(c -> c.id.equals(cardId));
        saveToFile(COLLECTION_FILE, collection);
    }

    // Quick membership check used by the UI to decide whether to show "Add" or "In Collection".
    // Reads the whole file each call — could be cached if it ever shows up as a perf issue.
    public static boolean isInCollection(String cardId) {
        return loadCollection().stream().anyMatch(c -> c.id.equals(cardId));
    }

    // -------------------------
    // Wishlist
    // -------------------------
    // Mirror of the collection methods, just pointed at a different file.
    // Could be deduped with a shared helper that takes a filename, but the
    // explicit duplication is easier to read at this scale.

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

    // Reads a save file and parses each line into a SavedCard.
    // Returns an empty list (not null) if the file is missing, so callers don't have to null-check.
    private static List<SavedCard> loadFromFile(String filename) {
        List<SavedCard> cards = new ArrayList<>();
        File file = new File(filename); // resolved relative to the JVM's working directory

        System.out.println("Looking for file at: " + file.getAbsolutePath()); // debug line

        // Missing file just means the user hasn't saved anything yet — not an error
        if (!file.exists()) {
            System.out.println("File not found!"); // debug line
            return cards;
        }

        // try-with-resources auto-closes the reader even if an exception is thrown
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Split on the literal "|" character. The double backslash is needed because
                // split() takes a regex, and "|" is a regex metacharacter.
                String[] parts = line.split("\\|");
                System.out.println("Read line: " + line); // debug line

                // Skip malformed lines (wrong field count) instead of crashing on them.
                // A future field addition would silently drop old saves — worth bumping
                // a version field if the format ever changes.
                if (parts.length == 5) {
                    cards.add(new SavedCard(parts[0], parts[1], parts[2], parts[3], parts[4]));
                }
            }
        } catch (Exception e) {
            // Logged but not rethrown — the UI gets an empty list back and keeps working
            System.out.println("Error loading " + filename + ": " + e.getMessage());
        }

        return cards;
    }

    // Writes the entire card list to disk, overwriting whatever was there.
    // FileWriter without the append flag truncates the file, which is exactly what we want
    // since callers always pass the full updated list.
    private static void saveToFile(String filename, List<SavedCard> cards) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (SavedCard card : cards) {
                // Build the pipe-delimited line. Mirror of the split() in loadFromFile.
                writer.write(card.id + "|" +
                        card.name + "|" +
                        card.imageUrl + "|" +
                        card.rarity + "|" +
                        card.marketPrice);
                writer.newLine();
            }
        } catch (Exception e) {
            // Same swallow-and-log pattern as loadFromFile — failures shouldn't crash the UI
            System.out.println("Error saving " + filename + ": " + e.getMessage());
        }
    }
}
