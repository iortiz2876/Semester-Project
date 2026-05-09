package storage;

import model.SavedCard;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

//stores cards in txt files
//Each user gets their own files
//used for collection and wishlist
public class CardStorage {

    // current user logged in
    // If this is null it uses the normal files
    private static String currentUserId = null;

    // sets the user after login
    public static void setCurrentUser(String userId) {
        currentUserId = userId;
    }

    // Gets the user that is logged in
    public static String getCurrentUser() {
        return currentUserId;
    }

    // makes the collection file name
    private static String getCollectionFile() {
        if (currentUserId == null) return "collection.txt";
        return "collection_" + currentUserId + ".txt";
    }

    // Makes the wishlist file name
    private static String getWishlistFile() {
        if (currentUserId == null) return "wishlist.txt";
        return "wishlist_" + currentUserId + ".txt";
    }

    // loads collection cards
    public static List<SavedCard> loadCollection() {
        return loadFromFile(getCollectionFile());
    }

    // Saves card to collection if not already there
    public static void saveToCollection(SavedCard card) {
        List<SavedCard> collection = loadCollection();
        boolean exists = collection.stream().anyMatch(c -> c.id.equals(card.id));
        if (!exists) {
            collection.add(card);
            saveToFile(getCollectionFile(), collection);
        }
    }

    // removes card from collection
    public static void removeFromCollection(String cardId) {
        List<SavedCard> collection = loadCollection();
        collection.removeIf(c -> c.id.equals(cardId));
        saveToFile(getCollectionFile(), collection);
    }

    // Checks if card is in collection
    public static boolean isInCollection(String cardId) {
        return loadCollection().stream().anyMatch(c -> c.id.equals(cardId));
    }

    //loads collection for a user id
    public static List<SavedCard> loadCollectionForUser(String userId) {
        return loadFromFile("collection_" + userId + ".txt");
    }

    //Loads wishlist cards
    public static List<SavedCard> loadWishlist() {
        return loadFromFile(getWishlistFile());
    }

    //saves card to wishlist if not already there
    public static void saveToWishlist(SavedCard card) {
        List<SavedCard> wishlist = loadWishlist();
        boolean exists = wishlist.stream().anyMatch(c -> c.id.equals(card.id));
        if (!exists) {
            wishlist.add(card);
            saveToFile(getWishlistFile(), wishlist);
        }
    }

    // Removes card from wishlist
    public static void removeFromWishlist(String cardId) {
        List<SavedCard> wishlist = loadWishlist();
        wishlist.removeIf(c -> c.id.equals(cardId));
        saveToFile(getWishlistFile(), wishlist);
    }

    // checks if card is in wishlist
    public static boolean isInWishlist(String cardId) {
        return loadWishlist().stream().anyMatch(c -> c.id.equals(cardId));
    }

    //Loads the cards from the file
    private static List<SavedCard> loadFromFile(String filename) {
        List<SavedCard> cards = new ArrayList<>();
        File file = new File(filename);

        // if no file then no cards
        if (!file.exists()) {
            return cards;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            // Goes through every line
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");

                // card needs 5 parts
                if (parts.length == 5) {
                    cards.add(new SavedCard(parts[0], parts[1], parts[2], parts[3], parts[4]));
                }
            }
        } catch (Exception e) {
            // Shows error if loading fails
            System.out.println("Error loading " + filename + ": " + e.getMessage());
        }

        return cards;
    }

    // writes the cards to the file
    private static void saveToFile(String filename, List<SavedCard> cards) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (SavedCard card : cards) {
                // Separates each card value with |
                writer.write(card.id + "|" +
                        card.name + "|" +
                        card.imageUrl + "|" +
                        card.rarity + "|" +
                        card.marketPrice);
                writer.newLine();
            }
        } catch (Exception e) {
            // shows error if saving fails
            System.out.println("Error saving " + filename + ": " + e.getMessage());
        }
    }
}