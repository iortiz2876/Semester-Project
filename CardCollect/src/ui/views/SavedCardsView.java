package ui.views;

import api.TCGDexClient;
import model.CardResult;
import model.SavedCard;
import ui.WrapLayout;
import ui.components.CardDetailDialog;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

// Shared base class for any view that displays a list of SavedCards in a scrollable grid
// (currently CollectionView and WishlistView). Handles the header, count label, grid,
// scroll pane, refresh loop, and card tile construction. Subclasses just plug in the
// title, empty-state message, and which storage methods to use.
public abstract class SavedCardsView extends JPanel {

    private final JPanel cardGridPanel;
    private final JLabel countLabel;

    // ─── Hooks for subclasses ──────────────────────────────────────
    // Each subclass fills these in to point at its own title, message, and storage backend.

    // Header title shown in the top bar (e.g. "⭐ My Collection")
    protected abstract String getTitle();

    // Message shown when the list is empty
    protected abstract String getEmptyMessage();

    // Load the cards to display (e.g. CardStorage.loadCollection())
    protected abstract List<SavedCard> loadCards();

    // Remove a card by id (e.g. CardStorage.removeFromCollection(id))
    protected abstract void removeCard(String id);

    public SavedCardsView() {
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(40, 40, 50));

        // ─── Header bar ──────────────────────────────────────────────
        // Slightly darker strip at the top with the title on the left and card count on the right
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 40));
        header.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel title = new JLabel(getTitle());
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 18));

        countLabel = new JLabel("0 cards");
        countLabel.setForeground(new Color(180, 180, 210));
        countLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        header.add(title, BorderLayout.WEST);
        header.add(countLabel, BorderLayout.EAST);

        // ─── Card grid ───────────────────────────────────────────────
        // WrapLayout flows tiles left-to-right and wraps to new rows inside the scroll pane
        cardGridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        cardGridPanel.setBackground(new Color(40, 40, 50));

        JScrollPane scrollPane = new JScrollPane(cardGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // smoother scroll than the default
        scrollPane.setBorder(null);

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Populate immediately so the view isn't blank when first shown
        refresh();
    }

    // Reloads cards from the subclass's storage and rebuilds the grid from scratch.
    // Call this whenever the underlying list might have changed.
    public void refresh() {
        cardGridPanel.removeAll();

        List<SavedCard> cards = loadCards();
        countLabel.setText(cards.size() + " cards");

        if (cards.isEmpty()) {
            // Friendly placeholder instead of a blank panel
            JLabel empty = new JLabel(getEmptyMessage());
            empty.setForeground(Color.LIGHT_GRAY);
            empty.setFont(new Font("Arial", Font.PLAIN, 14));
            cardGridPanel.add(empty);
        } else {
            for (SavedCard card : cards) {
                cardGridPanel.add(buildCardPanel(card));
            }
        }

        cardGridPanel.revalidate();
        cardGridPanel.repaint();
    }

    // Builds one card tile: image on top, name/price/remove button stacked at the bottom.
    private JPanel buildCardPanel(SavedCard card) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(new Color(55, 55, 70));
        panel.setPreferredSize(new Dimension(180, 300));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ─── Image (loaded asynchronously) ───────────────────────────
        // Start with a "Loading..." placeholder so the UI doesn't block on image downloads
        JLabel imageLabel = new JLabel("Loading...", SwingConstants.CENTER);
        imageLabel.setForeground(Color.LIGHT_GRAY);
        imageLabel.setPreferredSize(new Dimension(160, 220));

        // Holds the image once loaded so the click handler can pass it to the detail dialog
        ImageIcon[] loadedImage = {null};

        // SwingWorker does the download off the EDT and updates the label back on the EDT
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                return TCGDexClient.getCardImage(card.imageUrl);
            }
            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    loadedImage[0] = icon;
                    imageLabel.setIcon(icon);
                    imageLabel.setText(""); // clear the "Loading..." text
                } catch (Exception e) {
                    imageLabel.setText("No image");
                }
            }
        }.execute();

        // ─── Click to open card detail dialog ────────────────────────
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Window ancestor = SwingUtilities.getWindowAncestor(panel);
                JFrame parentFrame = (ancestor instanceof JFrame) ? (JFrame) ancestor : null;
                CardResult cardResult = new CardResult(card.name, card.id, loadedImage[0]);
                new CardDetailDialog(parentFrame, cardResult).setVisible(true);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(new Color(75, 75, 95));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(new Color(55, 55, 70));
            }
        });

        // ─── Name label ──────────────────────────────────────────────
        JLabel nameLabel = new JLabel(card.name, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));

        // ─── Price label ─────────────────────────────────────────────
        // Show "Price N/A" if the price is missing or was stored as the em-dash placeholder







        // ─── Remove button ───────────────────────────────────────────
        JButton removeBtn = new JButton("Remove");
        removeBtn.setBackground(new Color(140, 50, 50)); // red tint for destructive action
        removeBtn.setForeground(Color.WHITE);
        removeBtn.setFocusPainted(false);
        removeBtn.setBorderPainted(false);
        removeBtn.setFont(new Font("Arial", Font.PLAIN, 10));
        removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Delegate the actual removal to the subclass, then rebuild the whole grid
        removeBtn.addActionListener(e -> {
            removeCard(card.id);
            refresh();
        });

        // ─── Bottom panel: name, price, remove button stacked vertically ───
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false); // transparent so hover colour from parent panel shows through

        // BoxLayout needs explicit alignment or children default to left-aligned
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
       // priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        bottom.add(nameLabel);
        //bottom.add(priceLabel);
        bottom.add(Box.createRigidArea(new Dimension(0, 4))); // small gap before the button
        bottom.add(removeBtn);

        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }
}