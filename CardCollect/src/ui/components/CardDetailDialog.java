package ui.components;

import api.TCGDexClient;
import model.CardResult;
import javax.swing.*;
import java.awt.*;
import model.SavedCard;
import storage.CardStorage;

public class CardDetailDialog extends JDialog {

    public CardDetailDialog(JFrame parent, CardResult card) {
        super(parent, card.name, true);
        setSize(650, 700);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(new Color(40, 40, 50));

        // Show loading screen first
        JPanel loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setBackground(new Color(40, 40, 50));
        JLabel loadingLabel = new JLabel("Loading card details...", SwingConstants.CENTER);
        loadingLabel.setForeground(Color.WHITE);
        loadingLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        loadingPanel.add(loadingLabel, BorderLayout.CENTER);
        add(loadingPanel);

        // Fetch full card details in background
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

    // -------------------------
    // Main layout: image on left, info on right
    // -------------------------
    private JPanel buildDetailPanel(CardResult card, String json) {
        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBackground(new Color(40, 40, 50));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        root.add(buildImagePanel(card), BorderLayout.WEST);
        root.add(buildInfoPanel(card, json), BorderLayout.CENTER); // ← pass card here

        return root;
    }

    // -------------------------
    // Left side: large card image
    // -------------------------
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



    // -------------------------
    // Right side: all card info
    // -------------------------
    private JPanel buildInfoPanel(CardResult card, String json) { // ← card parameter here
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
            panel.add(makeInfoRow("Weakness",   weakness));
            panel.add(makeInfoRow("Resistance", extractField(json, "resistances")));
            panel.add(makeInfoRow("Retreat",    extractField(json, "retreat")));
        }

        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(makeSectionHeader("💰 Pricing (USD)"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(makeInfoRow("Market Price", extractNestedField(json, "normal", "marketPrice")));
        panel.add(makeInfoRow("Low",          extractNestedField(json, "normal", "lowPrice")));
        panel.add(makeInfoRow("Mid",          extractNestedField(json, "normal", "midPrice")));
        panel.add(makeInfoRow("High",         extractNestedField(json, "normal", "highPrice")));

        panel.add(Box.createVerticalGlue());
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(makeActionButtons(json, card)); // ← card is now available here
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(makeCloseButton());

        return panel;
    }

    // -------------------------
    // UI component builders
    // -------------------------
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

        JLabel valueLabel = new JLabel(value.isEmpty() ? "—" : value);
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

    // -------------------------
    // JSON parsing helpers
    // -------------------------

    // Extract a simple string, number, or array field
    private String extractField(String json, String field) {
        try {
            String key = "\"" + field + "\":";
            int idx = json.indexOf(key);
            if (idx == -1) return "";
            int start = idx + key.length();
            char first = json.charAt(start);

            if (first == '"') {
                // String value
                return json.substring(start + 1, json.indexOf("\"", start + 1));
            } else if (first == '[') {
                // Array — flatten to comma separated
                int end = json.indexOf("]", start);
                return json.substring(start, end + 1)
                        .replaceAll("[\\[\\]\"]", "")
                        .trim();
            } else {
                // Number
                int end = json.indexOf(",", start);
                if (end == -1) end = json.indexOf("}", start);
                return json.substring(start, end).trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    // Extract a field nested inside a parent object e.g. "normal": { "marketPrice": 5.00 }
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

    // Extract and format all attacks into a readable string
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

        // Build a SavedCard from the JSON
        String imageUrl    = extractField(json, "image");
        String rarity      = extractField(json, "rarity");
        String marketPrice = extractNestedField(json, "normal", "marketPrice");
        SavedCard savedCard = new SavedCard(card.id, card.name, imageUrl, rarity, marketPrice);

        // Collection button
        boolean inCollection = CardStorage.isInCollection(card.id);
        JButton collectionBtn = new JButton(inCollection ? "✅ In Collection" : "＋ Collection");
        styleActionButton(collectionBtn, inCollection ? new Color(60, 100, 60) : new Color(60, 80, 120));
        collectionBtn.addActionListener(e -> {
            if (CardStorage.isInCollection(card.id)) {
                CardStorage.removeFromCollection(card.id);
                collectionBtn.setText("＋ Collection");
                styleActionButton(collectionBtn, new Color(60, 80, 120));
            } else {
                CardStorage.saveToCollection(savedCard);
                collectionBtn.setText("✅ In Collection");
                styleActionButton(collectionBtn, new Color(60, 100, 60));
            }
        });

        // Wishlist button
        boolean inWishlist = CardStorage.isInWishlist(card.id);
        JButton wishlistBtn = new JButton(inWishlist ? "⭐ In Wishlist" : "☆ Wishlist");
        styleActionButton(wishlistBtn, inWishlist ? new Color(120, 100, 30) : new Color(60, 80, 120));
        wishlistBtn.addActionListener(e -> {
            if (CardStorage.isInWishlist(card.id)) {
                CardStorage.removeFromWishlist(card.id);
                wishlistBtn.setText("☆ Wishlist");
                styleActionButton(wishlistBtn, new Color(60, 80, 120));
            } else {
                CardStorage.saveToWishlist(savedCard);
                wishlistBtn.setText("⭐ In Wishlist");
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
}
