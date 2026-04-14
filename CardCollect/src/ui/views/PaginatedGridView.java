package ui.views;

import ui.WrapLayout;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Generic base class for any view that shows a list of items as a scrollable, paginated grid.
// Handles the grid panel, scroll pane, pagination bar (prev/next/page label), and the
// showPage/rebuildPagination logic. Subclasses plug in two things:
//   1. The generic type T (whatever kind of item they're displaying)
//   2. A buildTile(T item) method that turns one item into a JPanel tile
//
// Used by SearchView (T = CardResult) and SetDetailView (T = CardResult with live prices).
public abstract class PaginatedGridView<T> extends JPanel {

    // How many items to show per page. Tuned so pages render quickly and
    // the user gets feedback before the whole result set is processed.
    protected static final int PAGE_SIZE = 30;

    // Grid that holds the tiles for the current page; rebuilt on every showPage() call
    protected final JPanel cardGridPanel;

    // Prev/next/page-label strip at the bottom; hidden when there's only one page
    protected final JPanel paginationBar;

    // All items from the most recent load, kept in memory so paging doesn't re-fetch
    protected List<T> allItems = new ArrayList<>();
    protected int currentPage = 0;

    // ─── Hook for subclasses ───────────────────────────────────────
    // Build the UI tile for a single item. Called once per visible item per page.
    protected abstract JPanel buildTile(T item);

    public PaginatedGridView() {
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(40, 40, 50));

        // ─── Card grid ───────────────────────────────────────────────
        // WrapLayout flows tiles left-to-right and wraps to new rows inside the scroll pane
        cardGridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        cardGridPanel.setBackground(new Color(40, 40, 50));

        // ─── Pagination bar (bottom) ─────────────────────────────────
        // Hidden until a load produces more than one page of results
        paginationBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        paginationBar.setBackground(new Color(30, 30, 40));
        paginationBar.setVisible(false);

        // Wrap the grid in a scroll pane so long result lists can scroll vertically
        JScrollPane scrollPane = new JScrollPane(cardGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // smoother than the default 1px
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);
        add(paginationBar, BorderLayout.SOUTH);
        // Note: subclasses are expected to add their own header (search bar, back button, etc.)
        // to BorderLayout.NORTH in their constructors.
    }

    // Replaces the current items with a new list and renders page 0.
    // Call this from subclasses whenever fresh data comes in (e.g. after a search finishes).
    protected void setItems(List<T> items) {
        this.allItems = items;
        showPage(0);
    }

    // Renders a single page of items from the cached allItems list.
    // Called both for the initial render and on every prev/next click.
    protected void showPage(int page) {
        currentPage = page;

        // Calculate pagination bounds for this page
        int totalPages = (int) Math.ceil((double) allItems.size() / PAGE_SIZE);
        int from = page * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, allItems.size()); // clamp the last page

        // Wipe the previous page's tiles and rebuild from the slice for this page
        cardGridPanel.removeAll();
        for (int i = from; i < to; i++) {
            cardGridPanel.add(buildTile(allItems.get(i)));
        }
        cardGridPanel.revalidate();
        cardGridPanel.repaint();

        rebuildPagination(totalPages);
    }

    // Rebuilds the prev/next/page-label strip. Hidden entirely if there's only one page.
    protected void rebuildPagination(int totalPages) {
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

        // Page indicator with the total result count
        JLabel pageLabel = new JLabel("Page " + (currentPage + 1) + " of " + totalPages
                + "  (" + allItems.size() + " results)");
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

    // Factory for the prev/next pagination buttons. Shared styling + hover effect so
    // every view that extends this class gets the same look for free.
    protected JButton makePagButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(60, 60, 80));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            // Only highlight on hover if the button is actually enabled
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(new Color(90, 90, 120));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(60, 60, 80));
            }
        });
        return btn;
    }
}