package ui.views;

import api.TCGDexClient;
import model.SavedCard;
import storage.CardStorage;
import ui.WrapLayout;
import javax.swing.*;
import java.awt.*;
import java.util.List;

// View that displays the user's saved card collection as a scrollable grid of tiles.
// Each tile shows the card image, name, market price, and a remove button.
public class CollectionView extends JPanel {

    // Grid that holds one tile per saved card; rebuilt from scratch on every refresh()
    private JPanel cardGridPanel;

    // "X cards" counter shown in the header, updated on every refresh()
    private JLabel countLabel;

    public CollectionView() {
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(40, 40, 50)); // dark theme background

        // ─── Header bar ──────────────────────────────────────────────
        // Slightly darker than the body so it reads as a distinct strip at the top
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 40));
        header.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        // Title on the left
        JLabel title = new JLabel("⭐ My Collection");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 18));

        // Card count on the right; starts at 0 and gets updated by refresh()
        countLabel = new JLabel("0 cards");
        countLabel.setForeground(new Color(180, 180, 210));
        countLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        header.add(title, BorderLayout.WEST);
        header.add(countLabel, BorderLayout.EAST);

        // ─── Card grid ───────────────────────────────────────────────
        // WrapLayout is like FlowLayout but actually wraps properly inside a JScrollPane,
        // so tiles flow left-to-right and wrap to the next row when they run out of space
        cardGridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        cardGridPanel.setBackground(new Color(40, 40, 50));

        // Wrap the grid in a scroll pane so long collections can scroll vertically
        JScrollPane scrollPane = new JScrollPane(cardGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // smoother scroll than the default ~1px
        scrollPane.setBorder(null); // remove default border so it blends with the panel

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Populate the grid right away so the view isn't empty when first shown
        refresh();
    }

    // TODO: There should be a function that tells you when this view becomes visible —
    // when it does, you need to refresh your cards. OR you need to do this refresh
    // on the exterior button press that switches to this view. Right now refresh()
    // only runs at construction time and after the user removes a card, so changes
    // made elsewhere (like adding a card from the search view) won't show up until
    // something forces a refresh.

    // Reloads the collection from storage and rebuilds the grid from scratch.
    // Call this whenever the underlying collection might have changed.
    public void refresh() {
        cardGridPanel.removeAll(); // wipe the existing tiles before rebuilding

        // Pull the latest saved cards from disk and update the header counter
        List<SavedCard> cards = CardStorage.loadCollection();
        countLabel.setText(cards.size() + " cards");

        if (cards.isEmpty()) {
            // Empty state: show a friendly placeholder instead of a blank panel
            JLabel empty = new JLabel("No cards in your collection yet. Search and add some!");
            empty.setForeground(Color.LIGHT_GRAY);
            empty.setFont(new Font("Arial", Font.PLAIN, 14));
            cardGridPanel.add(empty);
        } else {
            // Build one tile per saved card
            for (SavedCard card : cards) {
                cardGridPanel.add(buildCardPanel(card));
            }
        }

        // Tell Swing to recompute the layout and repaint, since we changed the children
        cardGridPanel.revalidate();
        cardGridPanel.repaint();
    }

    // Builds one card tile for the grid: image on top, name/price/remove button on the bottom
    private JPanel buildCardPanel(SavedCard card) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(new Color(55, 55, 70));
        panel.setPreferredSize(new Dimension(180, 300)); // fixed tile size keeps the grid uniform
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // ─── Image (loaded asynchronously) ───────────────────────────
        // Start with a "Loading..." placeholder so the UI doesn't block while images download
        JLabel imageLabel = new JLabel("Loading...", SwingConstants.CENTER);
        imageLabel.setForeground(Color.LIGHT_GRAY);
        imageLabel.setPreferredSize(new Dimension(160, 220));

        // Fetch the card image off the EDT so the UI stays responsive while downloading.
        // SwingWorker handles the thread switching: doInBackground runs on a worker thread,
        // done() runs back on the EDT so it's safe to touch Swing components there.
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                return TCGDexClient.getCardImage(card.imageUrl);
            }
            @Override
            protected void done() {
                try {
                    imageLabel.setIcon(get());
                    imageLabel.setText(""); // clear the "Loading..." placeholder
                } catch (Exception e) {
                    // Network error, bad URL, etc. — fall back to a text placeholder
                    imageLabel.setText("No image");
                }
            }
        }.execute();

        // ─── Name label ──────────────────────────────────────────────
        JLabel nameLabel = new JLabel(card.name, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));

        // ─── Price label ─────────────────────────────────────────────
        // Show "Price N/A" if the price is missing or was stored as the em-dash placeholder
        String price = (card.marketPrice == null || card.marketPrice.equals("—"))
                ? "Price N/A" : card.marketPrice;
        JLabel priceLabel = new JLabel(price, SwingConstants.CENTER);
        priceLabel.setForeground(new Color(100, 220, 100)); // green to read as "money"
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 11));

        // ─── Remove button ───────────────────────────────────────────
        // Red tint to signal a destructive action
        JButton removeBtn = new JButton("Remove");
        removeBtn.setBackground(new Color(140, 50, 50));
        removeBtn.setForeground(Color.WHITE);
        removeBtn.setFocusPainted(false);
        removeBtn.setBorderPainted(false);
        removeBtn.setFont(new Font("Arial", Font.PLAIN, 10));
        removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Remove from storage and rebuild the whole grid so this tile disappears.
        // refresh() is heavy but the collection is small enough that it doesn't matter.
        removeBtn.addActionListener(e -> {
            CardStorage.removeFromCollection(card.id);
            refresh();
        });

        // ─── Bottom panel: stacks name, price, and remove button vertically ───
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(new Color(55, 55, 70));

        // BoxLayout needs each child to declare its alignment, otherwise they
        // default to left-aligned and look off-center under the image
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        bottom.add(nameLabel);
        bottom.add(priceLabel);
        bottom.add(Box.createRigidArea(new Dimension(0, 4))); // small gap before the button
        bottom.add(removeBtn);

        // Image fills the center of the tile, name/price/button row sits at the bottom
        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }
}