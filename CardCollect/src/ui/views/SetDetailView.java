package ui.views;

import api.TCGDexClient;
import model.CardResult;
import ui.components.CardDetailDialog;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// The "cards in a chosen set" screen. Extracted out of SetsView so it can plug into
// PaginatedGridView for free and so SetsView stays focused on just the set list.
// Displays each card with a live market price fetched in the background.
public class SetDetailView extends PaginatedGridView<CardResult> {

    private final JFrame parentFrame; // for modal card detail dialogs
    private final Runnable onBack;    // called when the user clicks "Back to Sets"

    // Header components kept as fields so the title can be updated when a new set loads
    private final JLabel titleLabel;

    // Shared thread pool for fetching market prices in the background.
    // Capped at 5 threads so we don't fire one request per visible card simultaneously.
    private static final ExecutorService priceExecutor = Executors.newFixedThreadPool(5);

    public SetDetailView(JFrame parentFrame, Runnable onBack) {
        super();
        this.parentFrame = parentFrame;
        this.onBack = onBack;

        // ─── Header: back button + set title ─────────────────────────
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        header.setBackground(new Color(30, 30, 40));

        JButton backBtn = new JButton("← Back to Sets");
        backBtn.setBackground(new Color(60, 60, 80));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.setBorderPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        // Manual hover effect since we disabled the default L&F border/focus
        backBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { backBtn.setBackground(new Color(90, 90, 120)); }
            public void mouseExited(java.awt.event.MouseEvent e)  { backBtn.setBackground(new Color(60, 60, 80));  }
        });
        // Delegate back navigation to whoever embeds this view (SetsView)
        backBtn.addActionListener(e -> onBack.run());

        titleLabel = new JLabel(""); // filled in by loadSet()
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        header.add(backBtn);
        header.add(titleLabel);
        add(header, BorderLayout.NORTH);
    }

    // Kicks off an async fetch of the given set's cards and hands them to the base class
    // when they arrive. Called by SetsView when the user clicks a set tile.
    public void loadSet(String setId, String setName) {
        // Update the header immediately so the user sees which set is loading
        titleLabel.setText(setName + "  —  loading...");

        // Show a loading placeholder in the grid
        cardGridPanel.removeAll();
        paginationBar.setVisible(false);
        JLabel loading = new JLabel("  Loading " + setName + "...");
        loading.setForeground(Color.LIGHT_GRAY);
        cardGridPanel.add(loading);
        cardGridPanel.revalidate();
        cardGridPanel.repaint();

        SwingWorker<java.util.List<CardResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected java.util.List<CardResult> doInBackground() throws Exception {
                return TCGDexClient.getSetCards(setId);
            }

            @Override
            protected void done() {
                try {
                    java.util.List<CardResult> cards = get();
                    // Update the header with the final card count and hand off to the base class
                    titleLabel.setText(setName + "  —  " + cards.size() + " cards");
                    setItems(cards);
                } catch (Exception e) {
                    cardGridPanel.removeAll();
                    JLabel err = new JLabel("Failed to load set: " + e.getMessage());
                    err.setForeground(new Color(220, 80, 80));
                    cardGridPanel.add(err);
                    cardGridPanel.revalidate();
                    cardGridPanel.repaint();
                }
            }
        };
        worker.execute();
    }

    // Build one card tile with image, name, and an asynchronously-loaded market price.
    @Override
    protected JPanel buildTile(CardResult card) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setBackground(new Color(55, 55, 70));
        panel.setPreferredSize(new Dimension(180, 295));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Card image (or fallback text if it didn't load)
        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        if (card.image != null) {
            imageLabel.setIcon(card.image);
        } else {
            imageLabel.setText("No image");
            imageLabel.setForeground(Color.WHITE);
        }

        // Bottom area: name on top, price below
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.setBackground(new Color(55, 55, 70));

        JLabel nameLabel = new JLabel(card.name, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        southPanel.add(nameLabel);

        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);

        // Click to open full card details; hover to highlight.
        // southPanel needs its background updated separately because it's an opaque child.
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                new CardDetailDialog(parentFrame, card).setVisible(true);
            }
            public void mouseEntered(java.awt.event.MouseEvent e) {
                panel.setBackground(new Color(80, 80, 100));
                southPanel.setBackground(new Color(80, 80, 100));
                panel.setBorder(BorderFactory.createLineBorder(new Color(120, 120, 180), 2));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                panel.setBackground(new Color(55, 55, 70));
                southPanel.setBackground(new Color(55, 55, 70));
                panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
        });


        return panel;
    }
}
