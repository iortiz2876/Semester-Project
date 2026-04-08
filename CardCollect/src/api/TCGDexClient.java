package api;

import model.CardResult;
import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class TCGDexClient {

    private static final String BASE_URL = "https://api.tcgdex.net/v2/en";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    // Search cards by name, returns list with images loaded in parallel
    public static List<CardResult> searchCardsWithImages(String name) throws Exception {
        String url = BASE_URL + "/cards?name=" + name.replace(" ", "%20");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        List<String[]> cardData = parseCardData(response.body());

        List<Future<CardResult>> futures = new ArrayList<>();
        for (String[] data : cardData) {
            String cardName = data[0];
            String imageUrl = data[1];
            String cardId   = data[2];

            Future<CardResult> future = executor.submit(() -> {
                try {
                    ImageIcon icon = getCardImage(imageUrl);
                    return new CardResult(cardName, cardId, icon);
                } catch (Exception e) {
                    return new CardResult(cardName, cardId, null);
                }
            });
            futures.add(future);
        }

        List<CardResult> results = new ArrayList<>();
        for (Future<CardResult> future : futures) {
            try {
                results.add(future.get(5, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                System.out.println("Image timed out, skipping.");
            }
        }

        return results;
    }

    // Get full details for a single card by ID
    public static String getCardDetails(String cardId) throws Exception {
        String url = BASE_URL + "/cards/" + cardId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    // Fetch and scale a card image from a URL
    public static ImageIcon getCardImage(String imageUrl) throws Exception {
        String fullUrl = imageUrl + "/low.png";
        URL url = new URL(fullUrl);
        ImageIcon icon = new ImageIcon(url);
        Image scaled = icon.getImage().getScaledInstance(160, 220, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    // Scale an existing ImageIcon to a new size
    public static ImageIcon scaleImage(ImageIcon icon, int width, int height) {
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    // Parse name, imageUrl, and id from JSON response
    private static List<String[]> parseCardData(String json) {
        List<String[]> results = new ArrayList<>();
        String[] entries = json.split("\\{");

        for (String entry : entries) {
            if (!entry.contains("\"name\"") || !entry.contains("\"image\"")) continue;
            try {
                int nameIndex = entry.indexOf("\"name\":\"");
                String cardName = entry.substring(nameIndex + 8,
                        entry.indexOf("\"", nameIndex + 8));

                int imageIndex = entry.indexOf("\"image\":\"");
                String imageUrl = entry.substring(imageIndex + 9,
                        entry.indexOf("\"", imageIndex + 9));

                int idIndex = entry.indexOf("\"id\":\"");
                String cardId = entry.substring(idIndex + 6,
                        entry.indexOf("\"", idIndex + 6));

                results.add(new String[]{cardName, imageUrl, cardId});
            } catch (Exception e) {
                System.out.println("Skipping entry: " + e.getMessage());
            }
        }

        return results;
    }
}