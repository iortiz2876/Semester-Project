package ui.views;

import api.TCGDexClient;
import model.CardResult;
import model.SavedCard;
import ui.WrapLayout;
import ui.components.CardDetailDialog;
import ui.components.MiniPriceChartPanel;
import java.util.ArrayList;
import ui.components.MiniPriceChartPanel;
import util.PriceHistoryGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

// Shared base class for any view that displays a list of SavedCards in a scrollable grid
//handles the header, count label, grid,
//scroll pane, refresh loop, and card tile construction. Subclasses just plug in the
//title, empty-state message, and which storage methods to use.
public abstract class SavedCardsView extends JPanel {

    private final JPanel cardGridPanel;
    private final JLabel countLabel;
    private final JLabel titleLabel;

    // Header title shown in the top bar
    protected abstract String getTitle();

    // Message shown when the list is empty
    protected abstract String getEmptyMessage();

    // Load the cards to display
    protected abstract List<SavedCard> loadCards();

    // Remove a card by id
    protected abstract void removeCard(String id);

    // Optional hook for subclasses
    protected void onCardClicked(SavedCard card) {
        // subclasses can override
    }

    // Optional hook so some views can hide the remove button
    protected boolean canRemoveCards() {
        return true;
    }

    protected boolean showCardDetailsOnClick() {
        return true;
    }

    public SavedCardsView() {
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(40, 40, 50));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 40));
        header.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        titleLabel = new JLabel(getTitle());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));

        countLabel = new JLabel("0 cards");
        countLabel.setForeground(new Color(180, 180, 210));
        countLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        header.add(titleLabel, BorderLayout.WEST);
        header.add(countLabel, BorderLayout.EAST);

        cardGridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        cardGridPanel.setBackground(new Color(40, 40, 50));

        JScrollPane scrollPane = new JScrollPane(cardGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        refresh();
    }


    public void refresh() {
        cardGridPanel.removeAll();

        // refresh dynamic title too
        titleLabel.setText(getTitle());

        List<SavedCard> cards = loadCards();
        countLabel.setText(cards.size() + " cards");

        if (cards.isEmpty()) {
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

    private JPanel buildCardPanel(SavedCard card) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(new Color(55, 55, 70));
        panel.setPreferredSize(new Dimension(180, 345));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel imageLabel = new JLabel("Loading...", SwingConstants.CENTER);
        imageLabel.setForeground(Color.LIGHT_GRAY);
        imageLabel.setPreferredSize(new Dimension(160, 220));

        ImageIcon[] loadedImage = {null};

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
                    imageLabel.setText("");
                } catch (Exception e) {
                    imageLabel.setText("No image");
                }
            }
        }.execute();

        panel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                    onCardClicked(card);

                    if (showCardDetailsOnClick()) {
                        Window ancestor = SwingUtilities.getWindowAncestor(panel);
                        JFrame parentFrame = (ancestor instanceof JFrame) ? (JFrame) ancestor : null;
                        CardResult cardResult = new CardResult(card.name, card.id, loadedImage[0]);
                        new CardDetailDialog(parentFrame, cardResult).setVisible(true);
                    }
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

        JLabel nameLabel = new JLabel(card.name, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));

        JLabel priceLabel = new JLabel(
                (card.marketPrice == null || card.marketPrice.isBlank())
                        ? "Price: N/A"
                        : "Price: " + card.marketPrice,
                SwingConstants.CENTER
        );

        java.util.List<Double> history = PriceHistoryGenerator.getHistoryForCard(card);
        MiniPriceChartPanel miniChart = new MiniPriceChartPanel(history);
        miniChart.setAlignmentX(Component.CENTER_ALIGNMENT);

        priceLabel.setForeground(new Color(200, 200, 220));
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 11));

        JButton removeBtn = new JButton("Remove");
        removeBtn.setBackground(new Color(140, 50, 50));
        removeBtn.setForeground(Color.WHITE);
        removeBtn.setFocusPainted(false);
        removeBtn.setBorderPainted(false);
        removeBtn.setFont(new Font("Arial", Font.PLAIN, 10));
        removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        removeBtn.addActionListener(e -> {
            removeCard(card.id);
            refresh();
        });

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);

        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        bottom.add(nameLabel);
        bottom.add(Box.createRigidArea(new Dimension(0, 2)));
        bottom.add(priceLabel);
        bottom.add(Box.createRigidArea(new Dimension(0, 4)));
        bottom.add(miniChart);

        if (canRemoveCards()) {
            bottom.add(Box.createRigidArea(new Dimension(0, 4)));
            bottom.add(removeBtn);
        }

        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }
}