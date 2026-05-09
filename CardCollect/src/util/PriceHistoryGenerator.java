package util;

import model.SavedCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PriceHistoryGenerator {

    private static final Map<String, List<Double>> CACHE = new HashMap<>();

    //gets saved history or makes a new one
    public static List<Double> getHistoryForCard(SavedCard card) {
        if (card == null) {
            return new ArrayList<>();
        }

        return CACHE.computeIfAbsent(card.id, key ->
                generateHistoryFromCurrentPrice(parsePrice(card.marketPrice)));
    }

    //makes labels for the chart days
    public static List<String> defaultDayLabels(int size) {
        List<String> labels = new ArrayList<>();

        for (int i = size - 1; i >= 0; i--) {
            if (i == 0) {
                labels.add("Today");
            } else {
                labels.add("-" + i + "d");
            }
        }

        return labels;
    }

    //makes fake price history from the current price
    private static List<Double> generateHistoryFromCurrentPrice(double currentPrice) {
        List<Double> history = new ArrayList<>();

        if (currentPrice <= 0) {
            currentPrice = 10.0;
        }

        double value = currentPrice * (0.82 + (Math.random() * 0.10));

        for (int i = 0; i < 6; i++) {
            value += (Math.random() * 4.0) - 2.0;

            if (value < 1.0) {
                value = 1.0;
            }

            history.add(round2(value));
        }

        history.add(round2(currentPrice));
        return history;
    }

    //turns price text into a number
    private static double parsePrice(String marketPrice) {
        if (marketPrice == null || marketPrice.isBlank()) {
            return 10.0;
        }

        try {
            return Double.parseDouble(
                    marketPrice.replace("$", "")
                            .replace(",", "")
                            .replace("—", "")
                            .trim()
            );
        } catch (Exception e) {
            return 10.0;
        }
    }

    //rounds to 2 decimals
    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}