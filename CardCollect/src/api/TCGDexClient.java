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

    // Get all sets - returns list of [id, name, logoUrl, cardCount]
    public static List<String[]> getAllSets() throws Exception {
        String url = BASE_URL + "/sets";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return parseSetList(response.body());
    }

    // Get all cards in a set with images loaded in parallel
    public static List<CardResult> getSetCards(String setId) throws Exception {
        String url = BASE_URL + "/sets/" + setId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return parseSetCards(response.body());
    }

    // Get market price string for a card ID (e.g. "$5.00" or "—")
    public static String getCardMarketPrice(String cardId) throws Exception {
        String json = getCardDetails(cardId);
        try {
            // Try "normal" pricing first, then "holofoil"
            for (String variant : new String[]{"normal", "holofoil", "reverseHolofoil"}) {
                int variantIdx = json.indexOf("\"" + variant + "\"");
                if (variantIdx == -1) continue;
                int braceStart = json.indexOf("{", variantIdx);
                int braceEnd = json.indexOf("}", braceStart);
                if (braceStart == -1 || braceEnd == -1) continue;
                String nested = json.substring(braceStart + 1, braceEnd);
                int mIdx = nested.indexOf("\"marketPrice\":");
                if (mIdx == -1) continue;
                int start = mIdx + 14;
                int end = nested.indexOf(",", start);
                if (end == -1) end = nested.length();
                String price = nested.substring(start, end).trim();
                if (!price.equals("null") && !price.isEmpty()) {
                    return "$" + price;
                }
            }
        } catch (Exception e) { /* ignore */ }
        return "—";
    }

    private static List<String[]> parseSetList(String json) {
        List<String[]> results = new ArrayList<>();
        String[] parts = json.split("(?=\\{)");
        for (String part : parts) {
            if (!part.contains("\"name\"")) continue;
            try {
                String id    = extractSimpleStringField(part, "id");
                String name  = extractSimpleStringField(part, "name");
                String logo  = extractSimpleStringField(part, "logo");
                String total = extractNestedNumberField(part, "cardCount", "total");
                if (!id.isEmpty() && !name.isEmpty()) {
                    results.add(new String[]{id, name, logo, total});
                }
            } catch (Exception e) { /* skip */ }
        }
        return results;
    }

    private static List<CardResult> parseSetCards(String json) {
        List<String[]> cardData = new ArrayList<>();
        int cardsIdx = json.indexOf("\"cards\"");
        if (cardsIdx == -1) return new ArrayList<>();
        int arrayStart = json.indexOf("[", cardsIdx);
        int arrayEnd   = json.lastIndexOf("]");
        if (arrayStart == -1 || arrayEnd == -1) return new ArrayList<>();
        String cardsJson = json.substring(arrayStart + 1, arrayEnd);

        String[] parts = cardsJson.split("(?=\\{)");
        for (String part : parts) {
            if (!part.contains("\"name\"")) continue;
            try {
                String id    = extractSimpleStringField(part, "id");
                String name  = extractSimpleStringField(part, "name");
                String image = extractSimpleStringField(part, "image");
                if (!id.isEmpty()) cardData.add(new String[]{name, image, id});
            } catch (Exception e) { /* skip */ }
        }

        List<Future<CardResult>> futures = new ArrayList<>();
        for (String[] data : cardData) {
            final String cardName = data[0], imageUrl = data[1], cardId = data[2];
            futures.add(executor.submit(() -> {
                try {
                    ImageIcon icon = getCardImage(imageUrl);
                    return new CardResult(cardName, cardId, icon);
                } catch (Exception e) {
                    return new CardResult(cardName, cardId, null);
                }
            }));
        }

        List<CardResult> results = new ArrayList<>();
        for (Future<CardResult> f : futures) {
            try {
                results.add(f.get(5, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                System.out.println("Image timed out, skipping.");
            } catch (Exception e) {
                System.out.println("Error loading card: " + e.getMessage());
            }
        }
        return results;
    }

    private static String extractSimpleStringField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int idx = json.indexOf(key);
        if (idx == -1) return "";
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    private static String extractNestedNumberField(String json, String parent, String field) {
        try {
            int parentIdx = json.indexOf("\"" + parent + "\":{");
            if (parentIdx == -1) return "0";
            int braceStart = json.indexOf("{", parentIdx);
            int braceEnd   = json.indexOf("}", braceStart);
            String nested  = json.substring(braceStart + 1, braceEnd);
            String key = "\"" + field + "\":";
            int idx = nested.indexOf(key);
            if (idx == -1) return "0";
            int start = idx + key.length();
            int end = nested.indexOf(",", start);
            if (end == -1) end = nested.length();
            return nested.substring(start, end).trim();
        } catch (Exception e) {
            return "0";
        }
    }
}