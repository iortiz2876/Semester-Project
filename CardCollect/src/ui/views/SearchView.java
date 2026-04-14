package ui.views;

import api.TCGDexClient;
import model.CardResult;
import ui.components.CardPanel;
import javax.swing.*;
import java.awt.*;
import java.util.List;

// Search view: user types a name, we hit the API, results show up in the paginated grid.
// All the grid, scroll pane, and pagination boilerplate now lives in PaginatedGridView —
// this class just owns the search bar on top and the doSearch() logic.
public class SearchView extends PaginatedGridView<CardResult> {

    private final JTextField searchField;
    private final JFrame parentFrame; // passed to CardPanel so detail dialogs can be modal to the main window

    public SearchView(JFrame parentFrame) {
        super(); // sets up the grid, scroll pane, and pagination bar
        this.parentFrame = parentFrame;

        // ─── Search bar (top) ────────────────────────────────────────
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");

        JPanel searchBar = new JPanel(new BorderLayout(5, 5));
        searchBar.setBackground(new Color(40, 40, 50));
        JLabel cardNameLabel = new JLabel("  Card Name:  ");
        cardNameLabel.setForeground(Color.WHITE);
        searchBar.add(cardNameLabel, BorderLayout.WEST);
        searchBar.add(searchField, BorderLayout.CENTER); // text field stretches to fill the row
        searchBar.add(searchButton, BorderLayout.EAST);
        searchBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Base class already put the grid in CENTER and the pagination bar in SOUTH —
        // we just need to add our search bar on top
        add(searchBar, BorderLayout.NORTH);

        // Trigger search both from the button click and from pressing Enter in the field
        searchButton.addActionListener(e -> doSearch());
        searchField.addActionListener(e -> doSearch());
    }

    // Each search result becomes a clickable CardPanel tile
    @Override
    protected JPanel buildTile(CardResult item) {
        return new CardPanel(item, parentFrame);
    }

    // Kicks off a search for whatever's in the text field. Runs the API call off the EDT
    // and hands the results to the base class when they arrive.
    private void doSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return; // ignore blank submits

        // Show a "Searching..." placeholder while the request is in flight
        cardGridPanel.removeAll();
        paginationBar.setVisible(false);
        JLabel loading = new JLabel("  Searching...");
        loading.setForeground(Color.WHITE);
        cardGridPanel.add(loading);
        cardGridPanel.revalidate();
        cardGridPanel.repaint();

        // SwingWorker handles the EDT-off / EDT-on thread switching
        SwingWorker<List<CardResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<CardResult> doInBackground() throws Exception {
                // Off the EDT — hits the network and decodes images
                return TCGDexClient.searchCardsWithImages(query);
            }

            @Override
            protected void done() {
                // Back on the EDT — safe to touch Swing
                try {
                    // Hand the results to the base class; it takes care of paging
                    setItems(get());
                } catch (Exception ex) {
                    // Swap the loading placeholder for a red error message
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
