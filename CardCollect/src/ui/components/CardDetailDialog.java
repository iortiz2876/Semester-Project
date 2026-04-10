package ui.components;

import api.TCGDexClient;
import model.CardResult;
import javax.swing.*;
import java.awt.*;
import model.SavedCard;
import storage.CardStorage;


//Gather up all the deatil about a card in your collection
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

   //build the panel for the card iteself
    private JPanel buildImagePanel(CardResult card) {
        // Create the container panel using BorderLayout so the image can be centered
        JPanel panel = new JPanel(new BorderLayout());

        // Dark slate background to make the card image stand out
        panel.setBackground(new Color(40, 40, 50));

        // Fix the panel width at 250px; height (0) lets the parent layout stretch it vertically
        panel.setPreferredSize(new Dimension(250, 0));

        // Label that will hold the card image (or fallback text), centered horizontally
        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);

        if (card.image != null) {
            // Scale the card's image to 230x320 (leaving a small margin inside the 250px panel)
            // and set it as the label's icon
            imageLabel.setIcon(TCGDexClient.scaleImage(card.image, 230, 320));
        } else {
            // Fallback when the card has no image: show a placeholder message in light gray
            imageLabel.setText("No image available");
            imageLabel.setForeground(Color.LIGHT_GRAY);
        }

        // Place the label in the center region of the BorderLayout
        panel.add(imageLabel, BorderLayout.CENTER);

        return panel;
    }

    //gathers the data for the card when clicked on
    private JPanel buildInfoPanel(CardResult card, String json) { // card parameter passed through for the action buttons
        // Container panel using vertical BoxLayout so each section stacks top-to-bottom
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(40, 40, 50)); // matches the dark theme of the image panel

        // ─── Card Info section ───────────────────────────────────────────
        panel.add(makeSectionHeader("Card Info"));
        panel.add(Box.createRigidArea(new Dimension(0, 6))); // small spacer below the header

        // Pull each field out of the raw JSON and render it as a label/value row
        panel.add(makeInfoRow("Name",        extractField(json, "name")));
        panel.add(makeInfoRow("ID",          extractField(json, "id")));
        panel.add(makeInfoRow("Rarity",      extractField(json, "rarity")));
        panel.add(makeInfoRow("HP",          extractField(json, "hp")));
        panel.add(makeInfoRow("Stage",       extractField(json, "stage")));
        panel.add(makeInfoRow("Types",       extractField(json, "types")));
        panel.add(makeInfoRow("Illustrator", extractField(json, "illustrator")));

        // ─── Description section (only shown if the card actually has one)
        String desc = extractField(json, "description");
        if (!desc.isEmpty()) {
            panel.add(Box.createRigidArea(new Dimension(0, 12))); // larger spacer between sections
            panel.add(makeSectionHeader("Description"));
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
            panel.add(makeDescriptionBox(desc)); // wrapped text box for longer prose
        }

        // Attacks section (only shown if the card has attacks)
        String attacks = extractAttacks(json); // custom extractor since attacks are a nested array
        if (!attacks.isEmpty()) {
            panel.add(Box.createRigidArea(new Dimension(0, 12)));
            panel.add(makeSectionHeader("Attacks"));
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
            panel.add(makeDescriptionBox(attacks));
        }

        // ─── Weakness / Resistance / Retreat section ─────────────────────
        // Only rendered if the card has a weakness (used as a proxy for "has battle stats")
        String weakness = extractField(json, "weaknesses");
        if (!weakness.isEmpty()) {
            panel.add(Box.createRigidArea(new Dimension(0, 12)));
            panel.add(makeSectionHeader("Weakness / Resistance"));
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
            panel.add(makeInfoRow("Weakness",   weakness));
            panel.add(makeInfoRow("Resistance", extractField(json, "resistances")));
            panel.add(makeInfoRow("Retreat",    extractField(json, "retreat")));
        }

        // ─── Pricing section (always shown) ──────────────────────────────
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(makeSectionHeader("💰 Pricing (USD)"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));


        //***** MONEY *******
        // Prices live under the "normal" object in the JSON, so use the nested-field extractor
        panel.add(makeInfoRow("Market Price", extractNestedField(json, "normal", "marketPrice")));
        panel.add(makeInfoRow("Low",          extractNestedField(json, "normal", "lowPrice")));
        panel.add(makeInfoRow("Mid",          extractNestedField(json, "normal", "midPrice")));
        panel.add(makeInfoRow("High",         extractNestedField(json, "normal", "highPrice")));

        // ─── Bottom controls ─────────────────────────────────────────────
        // Vertical glue pushes the buttons to the bottom of the panel regardless of content above
        panel.add(Box.createVerticalGlue());
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(makeActionButtons(json, card)); // action buttons need both the parsed json and the original card
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(makeCloseButton());

        return panel;
    }

    // -------------------------
    // UI component builders
    // -------------------------
    private JLabel makeSectionHeader(String text) {
        // Section header label used to title each group of fields (e.g. "Card Info", "Pricing")
        JLabel label = new JLabel(text);
        label.setForeground(new Color(160, 160, 220)); // soft purple-blue to stand out from white field values
        label.setFont(new Font("Arial", Font.BOLD, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT); // align left within the parent BoxLayout
        return label;
    }

    private JPanel makeInfoRow(String field, String value) {
        // A single "Label: Value" row used throughout the info panel
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        row.setBackground(new Color(40, 40, 50)); // matches parent background so the row blends in

        // Cap the row height at 28px so BoxLayout doesn't stretch it vertically to fill space
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // The field name (left side), bold and slightly dimmed
        JLabel fieldLabel = new JLabel(field + ":  ");
        fieldLabel.setForeground(new Color(180, 180, 210));
        fieldLabel.setFont(new Font("Arial", Font.BOLD, 12));

        // The value (right side); show an em-dash placeholder if there's no data
        JLabel valueLabel = new JLabel(value.isEmpty() ? "—" : value);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        row.add(fieldLabel);
        row.add(valueLabel);
        return row;
    }

    private JTextArea makeDescriptionBox(String text) {
        // A multi-line, word-wrapped, read-only text box used for descriptions and attack lists
        JTextArea area = new JTextArea(text);
        area.setWrapStyleWord(true); // wrap on word boundaries instead of mid-word
        area.setLineWrap(true);
        area.setEditable(false); // display only — user shouldn't be able to type into it
        area.setBackground(new Color(55, 55, 70)); // slightly lighter than the panel to look like a "card"
        area.setForeground(Color.LIGHT_GRAY);
        area.setFont(new Font("Arial", Font.ITALIC, 12)); // italic gives it a flavor-text feel
        area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8)); // inner padding so text doesn't touch edges
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100)); // cap height so it doesn't dominate the layout
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        return area;
    }

    private JButton makeCloseButton() {
        // Close button for dismissing the card detail window
        JButton button = new JButton("Close");
        button.setBackground(new Color(80, 80, 100));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);  // remove the focus rectangle for a cleaner look
        button.setBorderPainted(false); // remove the default button border
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // pointer cursor on hover
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Manual hover effect since we disabled the default L&F styling
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(110, 110, 140)); // brighten on hover
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(80, 80, 100)); // back to normal on exit
            }
        });

        // Closes the parent window (this method lives inside a JFrame/JDialog subclass)
        button.addActionListener(e -> dispose());
        return button;
    }

// -------------------------
// JSON parsing helpers
// -------------------------
// Note: these are hand-rolled string parsers rather than a real JSON library.
// They work for the TCGDex response shape but are fragile — they assume well-formed
// JSON without escaped quotes, nested arrays in unexpected places, etc.

    // Extract a simple string, number, or array field from the top level of the JSON
    private String extractField(String json, String field) {
        try {
            // Build the search key, e.g. "name": — the quotes prevent matching field names that
            // appear as substrings of other field names
            String key = "\"" + field + "\":";
            int idx = json.indexOf(key);
            if (idx == -1) return ""; // field not present

            int start = idx + key.length();
            char first = json.charAt(start); // peek at the first character to figure out the value's type

            if (first == '"') {
                // String value: grab everything between the opening and next closing quote
                return json.substring(start + 1, json.indexOf("\"", start + 1));
            } else if (first == '[') {
                // Array value: grab through the closing bracket, then strip brackets/quotes
                // so ["Fire","Water"] becomes "Fire,Water"
                int end = json.indexOf("]", start);
                return json.substring(start, end + 1)
                        .replaceAll("[\\[\\]\"]", "")
                        .trim();
            } else {
                // Number (or boolean/null): read until the next comma or closing brace
                int end = json.indexOf(",", start);
                if (end == -1) end = json.indexOf("}", start);
                return json.substring(start, end).trim();
            }
        } catch (Exception e) {
            // Any parsing failure (out-of-bounds, missing delimiter, etc.) just returns empty
            return "";
        }
    }

    // Extract a field nested inside a parent object, e.g. "normal": { "marketPrice": 5.00 }
    private String extractNestedField(String json, String parent, String field) {
        try {
            // Find the parent key
            int parentIdx = json.indexOf("\"" + parent + "\"");
            if (parentIdx == -1) return "—"; // parent object missing — return placeholder

            // Slice out just the parent's object body, then reuse extractField on that substring
            int braceStart = json.indexOf("{", parentIdx);
            int braceEnd   = json.indexOf("}", braceStart);
            String nested  = json.substring(braceStart, braceEnd);
            String value   = extractField(nested, field);

            // Prefix with $ since this helper is only used for prices; em-dash if missing
            return value.isEmpty() ? "—" : "$" + value;
        } catch (Exception e) {
            return "—";
        }
    }

    // Extract and format all attacks from the JSON into a single readable multi-line string
    private String extractAttacks(String json) {
        try {
            StringBuilder sb = new StringBuilder();

            // Locate the "attacks" array
            int idx = json.indexOf("\"attacks\"");
            if (idx == -1) return ""; // no attacks on this card

            int arrayStart = json.indexOf("[", idx);
            int arrayEnd   = json.indexOf("]", arrayStart);
            String attacksJson = json.substring(arrayStart, arrayEnd);

            // Split on "{" to get each attack object as its own chunk.
            // The leading "{" is stripped by split(), so we re-add it below before parsing.
            String[] attacks = attacksJson.split("\\{");
            for (String attack : attacks) {
                // Skip empty fragments (the part before the first "{") and anything without a name field
                if (!attack.contains("\"name\"")) continue;

                // Re-add the leading brace so extractField sees a proper object substring
                String name   = extractField("{" + attack, "name");
                String effect = extractField("{" + attack, "effect");
                String damage = extractField("{" + attack, "damage");

                // Format as: • AttackName  [Damage]
                //              effect text
                if (!name.isEmpty()) {
                    sb.append("• ").append(name);
                    if (!damage.isEmpty()) sb.append("  [").append(damage).append("]");
                    if (!effect.isEmpty()) sb.append("\n  ").append(effect);
                    sb.append("\n");
                }
            }
            return sb.toString().trim(); // trim trailing newline
        } catch (Exception e) {
            return "";
        }
    }

    private JPanel makeActionButtons(String json, CardResult card) {
        // Builds the row of "Collection" and "Wishlist" toggle buttons at the bottom of the info panel
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(new Color(40, 40, 50));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // cap height so BoxLayout doesn't stretch it

        // Build a SavedCard snapshot from the JSON — this is what gets persisted to storage
        // if the user adds the card to their collection or wishlist
        String imageUrl    = extractField(json, "image");
        String rarity      = extractField(json, "rarity");
        String marketPrice = extractNestedField(json, "normal", "marketPrice");
        SavedCard savedCard = new SavedCard(card.id, card.name, imageUrl, rarity, marketPrice);

        // ─── Collection toggle button ──────────────────────────────
        // Initial state reflects whether the card is already saved
        boolean inCollection = CardStorage.isInCollection(card.id);
        JButton collectionBtn = new JButton(inCollection ? "✅ In Collection" : "＋ Collection");
        // Green tint when already saved, blue tint otherwise
        styleActionButton(collectionBtn, inCollection ? new Color(60, 100, 60) : new Color(60, 80, 120));

        collectionBtn.addActionListener(e -> {
            // Toggle: if it's in the collection, remove it; otherwise add it.
            // Re-check storage on each click rather than trusting a cached boolean,
            // so the state stays correct even if something else modified storage.
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

        // ─── Wishlist toggle button ────────────────────────────────
        // Same toggle pattern as the collection button, but uses a gold tint for the "saved" state
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
        panel.add(Box.createRigidArea(new Dimension(8, 0))); // 8px horizontal gap between buttons
        panel.add(wishlistBtn);
        return panel;
    }

    // Shared styling for the action buttons so the collection and wishlist buttons stay visually consistent.
// Takes a background color since each button (and each state) uses a different tint.
    private void styleActionButton(JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);   // hide focus rectangle
        button.setBorderPainted(false);  // hide default border for a flat look
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("Arial", Font.PLAIN, 12));
    }
}
