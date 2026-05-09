package storage;

import model.User;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

//handles saving and loading users
//works by using one txt file
//each user is saved on one line
public class UserStorage {

    private static final String USERS_FILE = "users.txt";

    //changes saved password back to normal text
    private static String decode(String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }

    //changes password so it saves better in the file
    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    //checks if username and password are right
    public static User authenticate(String username, String password) {
        List<User> users = loadUsers();

        for (User user : users) {
            String decodedPassword = decode(user.password);

            //returns the user if login matches
            if (user.username.equals(username) && decodedPassword.equals(password)) {
                return user;
            }
        }

        return null;
    }

    //makes a new user if username is not taken
    public static User register(String username, String password) {
        List<User> users = loadUsers();

        //checks if username already exists
        boolean taken = users.stream().anyMatch(u -> u.username.equalsIgnoreCase(username));
        if (taken) {
            return null;
        }

        //makes a random id for the user
        String id = UUID.randomUUID().toString().substring(0, 8);
        User newUser = new User(id, username, encode(password));

        users.add(newUser);
        saveUsers(users);

        return newUser;
    }

    //gets all users
    public static List<User> getAllUsers() {
        return new ArrayList<>(loadUsers());
    }

    //finds user by id
    public static User findById(String id) {
        for (User user : loadUsers()) {
            if (user.id.equals(id)) {
                return user;
            }
        }

        return null;
    }

    //loads users from the file
    private static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(USERS_FILE);

        //if there is no file then no users yet
        if (!file.exists()) {
            return users;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            //goes through each user line
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");

                //each user should have id username and password
                if (parts.length == 3) {
                    users.add(new User(parts[0], parts[1], parts[2]));
                }
            }
        } catch (Exception e) {
            //prints error if loading users fails
            System.out.println("Error loading users: " + e.getMessage());
        }

        return users;
    }

    // saves all users to the file
    private static void saveUsers(List<User> users) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User user : users) {
                //saves user info with | between each part
                writer.write(user.id + "|" + user.username + "|" + user.password);
                writer.newLine();
            }
        } catch (Exception e) {
            System.out.println("Error saving users: " + e.getMessage()); // Prints error if saving users fails
        }
    }
}