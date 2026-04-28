package ui.views;

import model.SavedCard;
import storage.CardStorage;
import ui.components.PriceChartPanel;
import util.PriceHistoryGenerator;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PricesView extends JPanel {

    private final DefaultListModel<SavedCard> cardListModel = new DefaultListModel<>();
    private final JList<SavedCard> cardList = new JList<>(cardListModel);

    private final JLabel titleLabel = new JLabel("Select a card to view price history");
    private final JLabel currentPriceLabel = new JLabel("Current Price: N/A");
    private final JLabel trendLabel = new JLabel("Trend: N/A");

    private final PriceChartPanel chartPanel = new PriceChartPanel();

    public PricesView() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(40, 40, 50));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 40));
        header.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel headerTitle = new JLabel("Prices");
        headerTitle.setForeground(Color.WHITE);
        headerTitle.setFont(new Font("Arial", Font.BOLD, 18));
        header.add(headerTitle, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        setupCardList();

        JScrollPane leftScroll = new JScrollPane(cardList);
        leftScroll.setPreferredSize(new Dimension(260, 0));
        leftScroll.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 90)));

        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(new Color(40, 40, 50));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(30, 30, 40));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        currentPriceLabel.setForeground(new Color(210, 210, 220));
        currentPriceLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        trendLabel.setForeground(new Color(210, 210, 220));
        trendLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        infoPanel.add(titleLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        infoPanel.add(currentPriceLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        infoPanel.add(trendLabel);

        rightPanel.add(infoPanel, BorderLayout.NORTH);
        rightPanel.add(chartPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightPanel);
        splitPane.setResizeWeight(0.28);

        add(splitPane, BorderLayout.CENTER);

        refreshCards();
    }

    public void refreshCards() {
        cardListModel.clear();

        List<SavedCard> cards = CardStorage.loadCollection();
        for (SavedCard card : cards) {
            cardListModel.addElement(card);
        }

        if (!cardListModel.isEmpty()) {
            cardList.setSelectedIndex(0);
        } else {
            titleLabel.setText("No cards in your collection");
            currentPriceLabel.setText("Current Price: N/A");
            trendLabel.setText("Trend: N/A");
            chartPanel.setPriceHistory(java.util.Collections.emptyList(), java.util.Collections.emptyList());
        }
    }

    private void setupCardList() {
        cardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cardList.setBackground(new Color(50, 50, 65));
        cardList.setForeground(Color.WHITE);

        cardList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (value instanceof SavedCard card) {
                    label.setText(card.name + "  (" + safe(card.marketPrice) + ")");
                }

                label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                return label;
            }
        });

        cardList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SavedCard selected = cardList.getSelectedValue();
                updateSelectedCard(selected);
            }
        });
    }

    private void updateSelectedCard(SavedCard card) {
        if (card == null) {
            titleLabel.setText("Select a card to view price history");
            currentPriceLabel.setText("Current Price: N/A");
            trendLabel.setText("Trend: N/A");
            chartPanel.setPriceHistory(java.util.Collections.emptyList(), java.util.Collections.emptyList());
            return;
        }

        List<Double> history = PriceHistoryGenerator.getHistoryForCard(card);
        List<String> labels = PriceHistoryGenerator.defaultDayLabels(history.size());

        titleLabel.setText(card.name);
        currentPriceLabel.setText("Current Price: " + safe(card.marketPrice));
        trendLabel.setText("Trend: " + buildTrendText(history));

        chartPanel.setPriceHistory(labels, history);
    }

    private String buildTrendText(List<Double> history) {
        if (history == null || history.size() < 2) {
            return "N/A";
        }

        double first = history.get(0);
        double last = history.get(history.size() - 1);
        double diff = Math.round((last - first) * 100.0) / 100.0;

        if (diff > 0) {
            return "Up $" + String.format("%.2f", diff);
        } else if (diff < 0) {
            return "Down $" + String.format("%.2f", Math.abs(diff));
        } else {
            return "No change";
        }
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }
}