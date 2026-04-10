package ui.views;

import api.TCGDexClient;
import model.CardResult;
import ui.WrapLayout;
import ui.components.CardPanel;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// View that lets the user search for cards by name and browse the results in a paginated grid.
// Layout: search bar on top, card grid in the middle (scrollable), pagination bar on the bottom.
public class SearchView extends JPanel {

    // How many cards to render per page. Pagination keeps the grid fast even when
    // a search returns hundreds of results.
    private static final int PAGE_SIZE = 25;

    private final JTextField searchField;
    private final JPanel cardGridPanel; // holds the CardPanel tiles for the current page
    private final JPanel paginationBar; // prev/next/page-label strip at the bottom
    private final JFrame parentFrame;   // passed to CardPanel so its detail dialogs can be modal to the main window

    // All search results from the most recent query, kept in memory so paging
    // doesn't require re-hitting the API
    private List<CardResult> allResults = new ArrayList<>();
    private int currentPage = 0;

    public SearchView(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(40, 40, 50));

        // ─── Search bar (top) ────────────────────────────────────────
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");

        // Card grid uses WrapLayout so tiles flow left-to-right and wrap to new rows
        cardGridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        cardGridPanel.setBackground(new Color(40, 40, 50));

        JPanel searchBar = new JPanel(new BorderLayout(5, 5));
        searchBar.setBackground(new Color(40, 40, 50));
        JLabel cardNameLabel = new JLabel("  Card Name:  ");
        cardNameLabel.setForeground(Color.WHITE);
        searchBar.add(cardNameLabel, BorderLayout.WEST);
        searchBar.add(searchField, BorderLayout.CENTER); // text field stretches to fill the row
        searchBar.add(searchButton, BorderLayout.EAST);
        searchBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ─── Pagination bar (bottom) ─────────────────────────────────
        // Hidden until a search returns more than one page of results
        paginationBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        paginationBar.setBackground(new Color(30, 30, 40));
        paginationBar.setVisible(false);

        // Wrap the grid in a scroll pane so long result lists can scroll vertically
        JScrollPane scrollPane = new JScrollPane(cardGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // smoother than the default 1px scroll
        scrollPane.setBorder(null);

        add(searchBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(paginationBar, BorderLayout.SOUTH);

        // Trigger search both from the button click and from pressing Enter in the field
        searchButton.addActionListener(e -> doSearch());
        searchField.addActionListener(e -> doSearch());
    }

    // Kicks off a search for whatever's in the text field. Runs the API call off the EDT
    // and updates the grid with the results (or an error message) when it finishes.
    private void doSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return; // ignore blank submits

        // Reset state from any previous search
        allResults.clear();
        currentPage = 0;
        paginationBar.setVisible(false);

        // Show a "Searching..." placeholder while the request is in flight
        cardGridPanel.removeAll();
        JLabel loading = new JLabel("  Searching...");
        loading.setForeground(Color.WHITE);
        cardGridPanel.add(loading);
        cardGridPanel.revalidate();
        cardGridPanel.repaint();

        // SwingWorker handles the EDT-off / EDT-on thread switching for us
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
                    allResults = get();
                    showPage(0);
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

    // Renders a single page of results from the cached allResults list.
    // Called both for the initial render after a search and on every prev/next click.
    private void showPage(int page) {
        currentPage = page;

        // Calculate pagination bounds for this page
        int totalPages = (int) Math.ceil((double) allResults.size() / PAGE_SIZE);
        int from = page * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, allResults.size()); // clamp the last page

        // Wipe the previous page's tiles and rebuild from the slice for this page
        cardGridPanel.removeAll();
        for (int i = from; i < to; i++) {
            cardGridPanel.add(new CardPanel(allResults.get(i), parentFrame));
        }
        cardGridPanel.revalidate();
        cardGridPanel.repaint();

        rebuildPagination(totalPages);
    }

    // Rebuilds the prev/next/page-label strip. Hidden entirely if there's only one page.
    private void rebuildPagination(int totalPages) {
        paginationBar.removeAll();

        // Don't show the bar at all for single-page results — it would just be clutter
        if (totalPages <= 1) {
            paginationBar.setVisible(false);
            return;
        }

        // Prev button — disabled on the first page
        JButton prev = makePagButton("← Prev");
        prev.setEnabled(currentPage > 0);
        prev.addActionListener(e -> showPage(currentPage - 1));

        // Page indicator with the total result count, e.g. "Page 2 of 5  (108 results)"
        JLabel pageLabel = new JLabel("Page " + (currentPage + 1) + " of " + totalPages
                + "  (" + allResults.size() + " results)");
        pageLabel.setForeground(Color.WHITE);
        pageLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        // Next button — disabled on the last page
        JButton next = makePagButton("Next →");
        next.setEnabled(currentPage < totalPages - 1);
        next.addActionListener(e -> showPage(currentPage + 1));

        paginationBar.add(prev);
        paginationBar.add(pageLabel);
        paginationBar.add(next);
        paginationBar.setVisible(true);
        paginationBar.revalidate();
        paginationBar.repaint();
    }

    // Factory for the prev/next pagination buttons.
    // Duplicated from SetsView — could be pulled out into a shared UI helper if you want
    // to deduplicate, since both views style their pagination buttons identically.
    private JButton makePagButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(60, 60, 80));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            // Only highlight on hover if the button is actually enabled
            public void mouseEntered(java.awt.event.MouseEvent e) { if (btn.isEnabled()) btn.setBackground(new Color(90, 90, 120)); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(new Color(60, 60, 80)); }
        });
        return btn;
    }
}
