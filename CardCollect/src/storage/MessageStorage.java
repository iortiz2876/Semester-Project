package storage;

import model.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MessageStorage {
    private static final String MESSAGES_FILE = "messages.txt";

    // sends a message and adds it to the file
    public static synchronized void sendMessage(String fromUserId, String toUserId, String body) {
        Message msg = new Message(
                UUID.randomUUID().toString().substring(0, 8),
                fromUserId,
                toUserId,
                body,
                System.currentTimeMillis()
        );

        File file = new File(MESSAGES_FILE);

        // Writes the message at the end of the file
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(file, StandardCharsets.UTF_8, true))) {
            writer.write(msg.id + "|" + msg.fromUserId + "|" + msg.toUserId + "|" +
                    encode(msg.body) + "|" + msg.timestamp);
            writer.newLine();
        } catch (Exception e) {
            // shows error if message does not save
            System.out.println("Error saving message: " + e.getMessage());
        }
    }

    // Gets all messages between two users
    public static synchronized List<Message> getConversation(String userA, String userB) {
        List<Message> result = new ArrayList<>();

        for (Message msg : loadMessages()) {
            boolean forward = msg.fromUserId.equals(userA) && msg.toUserId.equals(userB);
            boolean reverse = msg.fromUserId.equals(userB) && msg.toUserId.equals(userA);

            // adds message if either user sent it
            if (forward || reverse) {
                result.add(msg);
            }
        }

        // Sorts messages by time
        result.sort(Comparator.comparingLong(m -> m.timestamp));
        return result;
    }

    // gets the users that have messages with this user
    public static synchronized Set<String> getConversationPartnerIds(String userId) {
        Set<String> ids = new LinkedHashSet<>();

        for (Message msg : loadMessages()) {
            if (msg.fromUserId.equals(userId)) {
                ids.add(msg.toUserId);
            } else if (msg.toUserId.equals(userId)) {
                ids.add(msg.fromUserId);
            }
        }

        return ids;
    }

    // Loads all messages from the file
    private static List<Message> loadMessages() {
        List<Message> messages = new ArrayList<>();
        File file = new File(MESSAGES_FILE);

        // if file does not exist then there are no messages
        if (!file.exists()) {
            return messages;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 5);

                // message needs all 5 parts
                if (parts.length == 5) {
                    messages.add(new Message(
                            parts[0],
                            parts[1],
                            parts[2],
                            decode(parts[3]),
                            Long.parseLong(parts[4])
                    ));
                }
            }
        } catch (Exception e) {
            // Shows error if messages can not load
            System.out.println("Error loading messages: " + e.getMessage());
        }

        return messages;
    }

    // saves all messages to the file
    private static void saveMessages(List<Message> messages) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MESSAGES_FILE, StandardCharsets.UTF_8))) {
            for (Message msg : messages) {
                writer.write(msg.id + "|" + msg.fromUserId + "|" + msg.toUserId + "|" + encode(msg.body) + "|" + msg.timestamp);
                writer.newLine();
            }
        } catch (Exception e) {
            // shows error if saving messages fails
            System.out.println("Error saving messages: " + e.getMessage());
        }
    }

    // Changes the text so | does not mess up the file
    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    // changes the saved text back to normal
    private static String decode(String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }
}
