package storage;

import model.User;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Handles reading and writing user accounts to a flat file.
// Works the same way as CardStorage, one user per line, pipe-delimited fields.
//
// File format per line:  id|username|password

public class UserStorage {

    private static final String USERS_FILE = "users.txt";

    // Attempts to log in with the given credentials.
    // Returns the matching User if the username and password are correct, null otherwise.
    public static User authenticate(String username, String password) {
        List<User> users = loadUsers();
        for (User user : users) {
            if (user.username.equals(username) && user.password.equals(password)) {
                return user;
            }
        }
        return null;
    }

    // Registers a new user account. Returns the created User on success,
    // or null if the username is already taken.
    public static User register(String username, String password) {
        List<User> users = loadUsers();

        // Check if username already exists
        boolean taken = users.stream().anyMatch(u -> u.username.equalsIgnoreCase(username));
        if (taken) {
            return null;
        }

        // Generate a unique id for the new user
        String id = UUID.randomUUID().toString().substring(0, 8);
        User newUser = new User(id, username, password);
        users.add(newUser);
        saveUsers(users);
        return newUser;
    }

    // Checks whether a username is already registered in the system.
    public static boolean usernameExists(String username) {
        return loadUsers().stream().anyMatch(u -> u.username.equalsIgnoreCase(username));
    }

    public static List<User> getAllUsers() {
        return new ArrayList<>(loadUsers());
    }

    public static User findById(String id) {
        for (User user : loadUsers()) {
            if (user.id.equals(id)) {
                return user;
            }
        }
        return null;
    }

    // -------------------------
    // File helpers
    // -------------------------

    // Reads all users from the file. Returns an empty list if the file doesn't exist yet.
    private static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(USERS_FILE);

        if (!file.exists()) {
            return users;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    users.add(new User(parts[0], parts[1], parts[2]));
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading users: " + e.getMessage());
        }

        return users;
    }

    // Writes the full user list to disk, overwriting the existing file.
    private static void saveUsers(List<User> users) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User user : users) {
                writer.write(user.id + "|" + user.username + "|" + user.password);
                writer.newLine();
            }
        } catch (Exception e) {
            System.out.println("Error saving users: " + e.getMessage());
        }
    }
}