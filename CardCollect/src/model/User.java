package model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// Represents a registered user account in the system.
// Holds the basic credentials needed for login and the unique id
// used to tie collection/wishlist files to a specific user.
public class User {

    public final String id;       // unique identifier (generated at registration)
    public final String username; // display name and login name
    public final String password; // stored as plain text for now — should be hashed in production

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }

    public User(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }
}
