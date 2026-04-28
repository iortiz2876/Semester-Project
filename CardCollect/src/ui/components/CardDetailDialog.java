package ui.components;

import api.TCGDexClient;
import model.CardResult;
import model.SavedCard;
import storage.CardStorage;
import ui.components.PriceChartPanel;
import util.PriceHistoryGenerator;

import javax.swing.*;
import java.awt.*;
import java.util.List;

//Gather up all the deatil about a card in your collection
public class CardDetailDialog extends JDialog {

    public CardDetailDialog(JFrame parent, CardResult card) {
        super(parent, card.name, true);
        setSize(650, 700);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(new Color(40, 40, 50));

        JPanel loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setBackground(new Color(40, 40, 50));
        JLabel loadingLabel = new JLabel("Loading card details...", SwingConstants.CENTER);
        loadingLabel.setForeground(Color.WHITE);
        loadingLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        loadingPanel.add(loadingLabel, BorderLayout.CENTER);
        add(loadingPanel);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return TCGDexClient.getCardDetails(card.id);
            }

            @Override
            protected void done() {
                try {
                    String json = get();
                    getContentPane().removeAll();
                    add(buildDetailPanel(card, json));
                    revalidate();
                    repaint();
                } catch (Exception ex) {
                    loadingLabel.setText("Error loading card details.");
                }
            }
        };
        worker.execute();
    }

    private JPanel buildDetailPanel(CardResult card, String json) {
        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBackground(new Color(40, 40, 50));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        root.add(buildImagePanel(card), BorderLayout.WEST);
        root.add(buildInfoPanel(card, json), BorderLayout.CENTER);

        return root;
    }

    private JPanel buildImagePanel(CardResult card) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 40, 50));
        panel.setPreferredSize(new Dimension(250, 0));

        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);

        if (card.image != null) {
            imageLabel.setIcon(TCGDexClient.scaleImage(card.image, 230, 320));
        } else {
            imageLabel.setText("No image available");
            imageLabel.setForeground(Color.LIGHT_GRAY);
        }

        panel.add(imageLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildInfoPanel(CardResult card, String json) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(40, 40, 50));

        panel.add(makeSectionHeader("Card Info"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        panel.add(makeInfoRow("Name",        extractField(json, "name")));
        panel.add(makeInfoRow("ID",          extractField(json, "id")));
        panel.add(makeInfoRow("Rarity",      extractField(json, "rarity")));
        panel.add(makeInfoRow("HP",          extractField(json, "hp")));
        panel.add(makeInfoRow("Stage",       extractField(json, "stage")));
        panel.add(makeInfoRow("Types",       extractField(json, "types")));
        panel.add(makeInfoRow("Illustrator", extractField(json, "illustrator")));

        String desc = extractField(json, "description");
        if (!desc.isEmpty()) {
            panel.add(Box.createRigidArea(new Dimension(0, 12)));
            panel.add(makeSectionHeader("Description"));
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
            panel.add(makeDescriptionBox(desc));
        }

        String attacks = extractAttacks(json);
        if (!attacks.isEmpty()) {
            panel.add(Box.createRigidArea(new Dimension(0, 12)));
            panel.add(makeSectionHeader("Attacks"));
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
            panel.add(makeDescriptionBox(attacks));
        }

        String weakness = extractField(json, "weaknesses");
        if (!weakness.isEmpty()) {
            panel.add(Box.createRigidArea(new Dimension(0, 12)));
            panel.add(makeSectionHeader("Weakness / Resistance"));
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
            panel.add(makeInfoRow("Weakness",   extractNestedField(json, "weaknesses", "type")));
            panel.add(makeInfoRow("Weakness",   extractNestedField(json, "weaknesses", "value")));
            panel.add(makeInfoRow("Resistance", extractField(json, "resistances")));
            panel.add(makeInfoRow("Retreat",    extractField(json, "retreat")));
        }

        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(makeSectionHeader("Pricing (USD)"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        String avgPrice = extractNestedField(json, "cardmarket", "avg");
        String lowPrice = extractNestedField(json, "cardmarket", "low");
        String trendPrice = extractNestedField(json, "cardmarket", "trend");

        panel.add(makeInfoRow("avg", avgPrice));
        panel.add(makeInfoRow("low", lowPrice));
        panel.add(makeInfoRow("trend", trendPrice));

        String basePrice = normalizePriceForGraph(avgPrice, trendPrice, lowPrice);

        SavedCard graphCard = new SavedCard(
                card.id,
                card.name,
                extractField(json, "image"),
                extractField(json, "rarity"),
                basePrice
        );

        List<Double> history = PriceHistoryGenerator.getHistoryForCard(graphCard);
        List<String> labels = PriceHistoryGenerator.defaultDayLabels(history.size());

        PriceChartPanel chartPanel = new PriceChartPanel();
        chartPanel.setPriceHistory(labels, history);
        chartPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        chartPanel.setPreferredSize(new Dimension(320, 220));

        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(chartPanel);

        panel.add(Box.createVerticalGlue());
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(makeActionButtons(json, card));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(makeCloseButton());

        return panel;
    }

    private JLabel makeSectionHeader(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(160, 160, 220));
        label.setFont(new Font("Arial", Font.BOLD, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel makeInfoRow(String field, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        row.setBackground(new Color(40, 40, 50));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel fieldLabel = new JLabel(field + ":  ");
        fieldLabel.setForeground(new Color(180, 180, 210));
        fieldLabel.setFont(new Font("Arial", Font.BOLD, 12));

        JLabel valueLabel = new JLabel(value.isEmpty() ? "-" : value);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        row.add(fieldLabel);
        row.add(valueLabel);
        return row;
    }

    private JTextArea makeDescriptionBox(String text) {
        JTextArea area = new JTextArea(text);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setEditable(false);
        area.setBackground(new Color(55, 55, 70));
        area.setForeground(Color.LIGHT_GRAY);
        area.setFont(new Font("Arial", Font.ITALIC, 12));
        area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        return area;
    }

    private JButton makeCloseButton() {
        JButton button = new JButton("Close");
        button.setBackground(new Color(80, 80, 100));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(110, 110, 140));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(80, 80, 100));
            }
        });

        button.addActionListener(e -> dispose());
        return button;
    }

    private String extractField(String json, String field) {
        try {
            String key = "\"" + field + "\":";
            int idx = json.indexOf(key);
            if (idx == -1) return "";

            int start = idx + key.length();
            char first = json.charAt(start);

            if (first == '"') {
                return json.substring(start + 1, json.indexOf("\"", start + 1));
            } else if (first == '[') {
                int end = json.indexOf("]", start);
                return json.substring(start, end + 1)
                        .replaceAll("[\\[\\]\"]", "")
                        .trim();
            } else {
                int end = json.indexOf(",", start);
                if (end == -1) end = json.indexOf("}", start);
                return json.substring(start, end).trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    private String extractNestedField(String json, String parent, String field) {
        try {
            int parentIdx = json.indexOf("\"" + parent + "\"");
            if (parentIdx == -1) return "—";

            int braceStart = json.indexOf("{", parentIdx);
            int braceEnd   = json.indexOf("}", braceStart);
            String nested  = json.substring(braceStart, braceEnd);
            String value   = extractField(nested, field);

            return value.isEmpty() ? "—" : "$" + value;
        } catch (Exception e) {
            return "—";
        }
    }

    private String extractAttacks(String json) {
        try {
            StringBuilder sb = new StringBuilder();

            int idx = json.indexOf("\"attacks\"");
            if (idx == -1) return "";

            int arrayStart = json.indexOf("[", idx);
            int arrayEnd   = json.indexOf("]", arrayStart);
            String attacksJson = json.substring(arrayStart, arrayEnd);

            String[] attacks = attacksJson.split("\\{");
            for (String attack : attacks) {
                if (!attack.contains("\"name\"")) continue;

                String name   = extractField("{" + attack, "name");
                String effect = extractField("{" + attack, "effect");
                String damage = extractField("{" + attack, "damage");

                if (!name.isEmpty()) {
                    sb.append("• ").append(name);
                    if (!damage.isEmpty()) sb.append("  [").append(damage).append("]");
                    if (!effect.isEmpty()) sb.append("\n  ").append(effect);
                    sb.append("\n");
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private JPanel makeActionButtons(String json, CardResult card) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(new Color(40, 40, 50));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        String imageUrl    = extractField(json, "image");
        String rarity      = extractField(json, "rarity");
        String marketPrice = extractNestedField(json, "normal", "marketPrice");
        SavedCard savedCard = new SavedCard(card.id, card.name, imageUrl, rarity, marketPrice);

        boolean inCollection = CardStorage.isInCollection(card.id);
        JButton collectionBtn = new JButton(inCollection ? "- In Collection" : "+ Collection");
        styleActionButton(collectionBtn, inCollection ? new Color(60, 100, 60) : new Color(60, 80, 120));

        collectionBtn.addActionListener(e -> {
            if (CardStorage.isInCollection(card.id)) {
                CardStorage.removeFromCollection(card.id);
                collectionBtn.setText("+ Collection");
                styleActionButton(collectionBtn, new Color(60, 80, 120));
            } else {
                CardStorage.saveToCollection(savedCard);
                collectionBtn.setText("- In Collection");
                styleActionButton(collectionBtn, new Color(60, 100, 60));
            }
        });

        boolean inWishlist = CardStorage.isInWishlist(card.id);
        JButton wishlistBtn = new JButton(inWishlist ? "- In Wishlist" : "+ Wishlist");
        styleActionButton(wishlistBtn, inWishlist ? new Color(120, 100, 30) : new Color(60, 80, 120));

        wishlistBtn.addActionListener(e -> {
            if (CardStorage.isInWishlist(card.id)) {
                CardStorage.removeFromWishlist(card.id);
                wishlistBtn.setText("+ Wishlist");
                styleActionButton(wishlistBtn, new Color(60, 80, 120));
            } else {
                CardStorage.saveToWishlist(savedCard);
                wishlistBtn.setText("- In Wishlist");
                styleActionButton(wishlistBtn, new Color(120, 100, 30));
            }
        });

        panel.add(collectionBtn);
        panel.add(Box.createRigidArea(new Dimension(8, 0)));
        panel.add(wishlistBtn);
        return panel;
    }

    private void styleActionButton(JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("Arial", Font.PLAIN, 12));
    }

    private String normalizePriceForGraph(String avgPrice, String trendPrice, String lowPrice) {
        if (isUsablePrice(avgPrice)) return avgPrice;
        if (isUsablePrice(trendPrice)) return trendPrice;
        if (isUsablePrice(lowPrice)) return lowPrice;
        return "$10.00";
    }

    private boolean isUsablePrice(String price) {
        if (price == null) return false;

        String cleaned = price.replace("$", "").replace(",", "").replace("—", "").trim();
        if (cleaned.isEmpty()) return false;

        try {
            Double.parseDouble(cleaned);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}