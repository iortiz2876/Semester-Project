package storage;

import model.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MessageStorage {
    private static final String MESSAGES_FILE = "messages.txt";

    public static synchronized void sendMessage(String fromUserId, String toUserId, String body) {
        Message msg = new Message(
                UUID.randomUUID().toString().substring(0, 8),
                fromUserId,
                toUserId,
                body,
                System.currentTimeMillis()
        );

        File file = new File(MESSAGES_FILE);

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(file, StandardCharsets.UTF_8, true))) {
            writer.write(msg.id + "|" + msg.fromUserId + "|" + msg.toUserId + "|" +
                    encode(msg.body) + "|" + msg.timestamp);
            writer.newLine();
        } catch (Exception e) {
            System.out.println("Error saving message: " + e.getMessage());
        }
    }

    public static synchronized List<Message> getConversation(String userA, String userB) {
        List<Message> result = new ArrayList<>();
        for (Message msg : loadMessages()) {
            boolean forward = msg.fromUserId.equals(userA) && msg.toUserId.equals(userB);
            boolean reverse = msg.fromUserId.equals(userB) && msg.toUserId.equals(userA);
            if (forward || reverse) {
                result.add(msg);
            }
        }
        result.sort(Comparator.comparingLong(m -> m.timestamp));
        return result;
    }

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

    private static List<Message> loadMessages() {
        List<Message> messages = new ArrayList<>();
        File file = new File(MESSAGES_FILE);
        if (!file.exists()) {
            return messages;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 5);
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
            System.out.println("Error loading messages: " + e.getMessage());
        }

        return messages;
    }

    private static void saveMessages(List<Message> messages) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MESSAGES_FILE, StandardCharsets.UTF_8))) {
            for (Message msg : messages) {
                writer.write(msg.id + "|" + msg.fromUserId + "|" + msg.toUserId + "|" + encode(msg.body) + "|" + msg.timestamp);
                writer.newLine();
            }
        } catch (Exception e) {
            System.out.println("Error saving messages: " + e.getMessage());
        }
    }

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }
}
