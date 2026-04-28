package model;

public class SavedCard {
    public String id;
    public String name;
    public String imageUrl;
    public String rarity;
    public String marketPrice;

    public SavedCard(String id, String name, String imageUrl,
                     String rarity, String marketPrice) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.rarity = rarity;

        if (marketPrice == null || marketPrice.isBlank() || marketPrice.equalsIgnoreCase("-") || marketPrice.equalsIgnoreCase("—")) {
            this.marketPrice = generateRandomMarketPrice(rarity);
        } else {
            this.marketPrice = marketPrice;
        }
    }

    public static String generateRandomMarketPrice(String rarity) {
        double min;
        double max;

        if (rarity == null) {
            rarity = "";
        }

        switch (rarity.toLowerCase()) {
            case "common":
                min = 1.00;
                max = 10.00;
                break;
            case "uncommon":
                min = 5.00;
                max = 20.00;
                break;
            case "rare":
                min = 15.00;
                max = 75.00;
                break;
            case "holo rare":
                min = 25.00;
                max = 120.00;
                break;
            case "ultra rare":
                min = 50.00;
                max = 200.00;
                break;
            case "secret rare":
                min = 80.00;
                max = 300.00;
                break;
            default:
                min = 5.00;
                max = 50.00;
                break;
        }

        double value = min + (Math.random() * (max - min));

        // small chance for a high-value spike
        if (Math.random() < 0.05) {
            value *= 1.5;
        }

        return String.format("$%.2f", value);
    }
}