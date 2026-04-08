package model;

public class SavedCard {
    public String id;
    public String name;
    public String imageUrl;
    public String rarity;
    public String marketPrice;

    public SavedCard(String id, String name, String imageUrl,
                     String rarity, String marketPrice) {
        this.id          = id;
        this.name        = name;
        this.imageUrl    = imageUrl;
        this.rarity      = rarity;
        this.marketPrice = marketPrice;
    }
}