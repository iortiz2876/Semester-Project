package ui.views;

import api.TCGDexClient;
import model.SavedCard;
import storage.CardStorage;
import ui.WrapLayout;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CollectionView extends JPanel {

    private JPanel cardGridPanel;
    private JLabel countLabel;

    public CollectionView() {
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(40, 40, 50));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 40));
        header.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel title = new JLabel("⭐ My Collection");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 18));

        countLabel = new JLabel("0 cards");
        countLabel.setForeground(new Color(180, 180, 210));
        countLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        header.add(title, BorderLayout.WEST);
        header.add(countLabel, BorderLayout.EAST);

        // Card grid
        cardGridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        cardGridPanel.setBackground(new Color(40, 40, 50));

        JScrollPane scrollPane = new JScrollPane(cardGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Load immediately on creation
        refresh();
    }

    // There should be a function that tells you when this view becomes visibile
    // when it does, you need to refresh your cards.
    // OR you need to do this refresh on the exterior button pressed.

    public void refresh() {
        cardGridPanel.removeAll();

        List<SavedCard> cards = CardStorage.loadCollection();
        countLabel.setText(cards.size() + " cards");

        if (cards.isEmpty()) {
            JLabel empty = new JLabel("No cards in your collection yet. Search and add some!");
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

    private JPanel buildCardPanel(SavedCard card) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(new Color(55, 55, 70));
        panel.setPreferredSize(new Dimension(180, 300));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Image
        JLabel imageLabel = new JLabel("Loading...", SwingConstants.CENTER);
        imageLabel.setForeground(Color.LIGHT_GRAY);
        imageLabel.setPreferredSize(new Dimension(160, 220));

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                return TCGDexClient.getCardImage(card.imageUrl);
            }
            @Override
            protected void done() {
                try {
                    imageLabel.setIcon(get());
                    imageLabel.setText("");
                } catch (Exception e) {
                    imageLabel.setText("No image");
                }
            }
        }.execute();

        // Name
        JLabel nameLabel = new JLabel(card.name, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));

        // Price
        String price = (card.marketPrice == null || card.marketPrice.equals("—"))
                ? "Price N/A" : card.marketPrice;
        JLabel priceLabel = new JLabel(price, SwingConstants.CENTER);
        priceLabel.setForeground(new Color(100, 220, 100));
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 11));

        // Remove button
        JButton removeBtn = new JButton("Remove");
        removeBtn.setBackground(new Color(140, 50, 50));
        removeBtn.setForeground(Color.WHITE);
        removeBtn.setFocusPainted(false);
        removeBtn.setBorderPainted(false);
        removeBtn.setFont(new Font("Arial", Font.PLAIN, 10));
        removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeBtn.addActionListener(e -> {
            CardStorage.removeFromCollection(card.id);
            refresh();
        });

        // Bottom panel
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(new Color(55, 55, 70));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottom.add(nameLabel);
        bottom.add(priceLabel);
        bottom.add(Box.createRigidArea(new Dimension(0, 4)));
        bottom.add(removeBtn);

        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }
}