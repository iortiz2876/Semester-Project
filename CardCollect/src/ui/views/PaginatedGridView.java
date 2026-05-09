package ui.views;

import ui.WrapLayout;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

//base class for pages that show items in a grid
//used so other views do not repeat the same page code
public abstract class PaginatedGridView<T> extends JPanel {

    //how many items show on one page
    protected static final int PAGE_SIZE = 30;

    //panel that holds the item cards
    protected final JPanel cardGridPanel;

    //bottom part with prev and next buttons
    protected final JPanel paginationBar;

    //keeps all loaded items for paging
    protected List<T> allItems = new ArrayList<>();
    protected int currentPage = 0;

    //subclasses make their own tile for each item
    protected abstract JPanel buildTile(T item);

    public PaginatedGridView() {
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(40, 40, 50));

        //makes the grid where cards go
        cardGridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        cardGridPanel.setBackground(new Color(40, 40, 50));

        //makes the page buttons area
        paginationBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        paginationBar.setBackground(new Color(30, 30, 40));
        paginationBar.setVisible(false);

        //lets the grid scroll
        JScrollPane scrollPane = new JScrollPane(cardGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);
        add(paginationBar, BorderLayout.SOUTH);
    }

    //sets new items and starts on first page
    protected void setItems(List<T> items) {
        this.allItems = items;
        showPage(0);
    }

    //shows one page of the items
    protected void showPage(int page) {
        currentPage = page;

        int totalPages = (int) Math.ceil((double) allItems.size() / PAGE_SIZE);
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, allItems.size());

        //clears old cards and adds new page cards
        cardGridPanel.removeAll();
        for (int i = from; i < to; i++) {
            cardGridPanel.add(buildTile(allItems.get(i)));
        }

        cardGridPanel.revalidate();
        cardGridPanel.repaint();

        rebuildPagination(totalPages);
    }

    //updates the bottom page buttons
    protected void rebuildPagination(int totalPages) {
        paginationBar.removeAll();

        //hides buttons if there is only one page
        if (totalPages <= 1) {
            paginationBar.setVisible(false);
            return;
        }

        JButton prev = makePagButton("← Prev");
        prev.setEnabled(currentPage > 0);
        prev.addActionListener(e -> showPage(currentPage - 1));

        //shows current page number
        JLabel pageLabel = new JLabel("Page " + (currentPage + 1) + " of " + totalPages
                + "  (" + allItems.size() + " results)");
        pageLabel.setForeground(Color.WHITE);
        pageLabel.setFont(new Font("Arial", Font.PLAIN, 12));

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

    //makes the buttons for changing pages
    protected JButton makePagButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(60, 60, 80));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Arial", Font.PLAIN, 12));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            // changes color when mouse is on it
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