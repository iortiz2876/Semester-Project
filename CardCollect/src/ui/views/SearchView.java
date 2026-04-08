package ui.views;

import api.TCGDexClient;
import model.CardResult;
import ui.WrapLayout;
import ui.components.CardPanel;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SearchView extends JPanel {

    private final JTextField searchField;
    private final JPanel cardGridPanel;
    private final JFrame parentFrame;

    public SearchView(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(40, 40, 50));

        searchField = new JTextField();
        JButton searchButton = new JButton("Search");

        cardGridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        cardGridPanel.setBackground(new Color(40, 40, 50));

        JPanel searchBar = new JPanel(new BorderLayout(5, 5));
        searchBar.setBackground(new Color(40, 40, 50));
        searchBar.add(new JLabel("  Card Name: "), BorderLayout.WEST);
        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(searchButton, BorderLayout.EAST);
        searchBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(cardGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);

        add(searchBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e -> doSearch());
        searchField.addActionListener(e -> doSearch());
    }

    private void doSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        cardGridPanel.removeAll();
        JLabel loading = new JLabel("  Searching...");
        loading.setForeground(Color.WHITE);
        cardGridPanel.add(loading);
        cardGridPanel.revalidate();
        cardGridPanel.repaint();

        SwingWorker<Void, CardPanel> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<CardResult> cards = TCGDexClient.searchCardsWithImages(query);
                for (CardResult card : cards) {
                    publish(new CardPanel(card, parentFrame));
                }
                return null;
            }

            @Override
            protected void process(List<CardPanel> panels) {
                if (cardGridPanel.getComponentCount() == 1 &&
                        cardGridPanel.getComponent(0) instanceof JLabel) {
                    cardGridPanel.removeAll();
                }
                for (CardPanel p : panels) cardGridPanel.add(p);
                cardGridPanel.revalidate();
                cardGridPanel.repaint();
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception ex) {
                    cardGridPanel.removeAll();
                    JLabel err = new JLabel("Error: " + ex.getMessage());
                    err.setForeground(Color.RED);
                    cardGridPanel.add(err);
                    cardGridPanel.revalidate();
                    cardGridPanel.repaint();
                }
            }
        };
        worker.execute();
    }
}