package ui.views;

import api.TCGDexClient;
import model.CardResult;
import ui.WrapLayout;
import ui.components.CardDetailDialog;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// View that lets the user browse all available card sets, then drill into a set
// to see its cards (with live market prices) in a paginated grid.
//
// Internally this uses a CardLayout with two "screens":
//   - "setList"   → the grid of all sets
//   - "setDetail" → the paginated card grid for a selected set
public class SetsView extends JPanel {

    // How many cards to show per page in the set detail view.
    // Cards fetch their prices individually, so paginating keeps the network
    // load (and the price executor queue) manageable.
    private static final int PAGE_SIZE = 25;

    private final JFrame parentFrame; // needed so card detail dialogs can be modal to the main window

    // CardLayout container that swaps between the set list and the set detail screens
    private final JPanel contentPanel;
    private final CardLayout contentLayout;

    // Shared thread pool for fetching market prices in the background.
    // Static so all SetsView instances share one pool — capped at 5 threads
    // so we don't hammer the API with one request per visible card at once.
    private static final ExecutorService priceExecutor = Executors.newFixedThreadPool(5);

    // ─── Set detail pagination state ──────────────────────────────────
    // Held as fields (rather than passed around) so the prev/next buttons
    // can rebuild the current page without re-fetching the cards
    private List<CardResult> currentSetCards = new java.util.ArrayList<>();
    private int setCurrentPage = 0;
    private String currentSetName = "";

    public SetsView(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(40, 40, 50));

        // CardLayout lets us swap between the set list and set detail views in place
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setBackground(new Color(40, 40, 50));

        // Show a loading placeholder immediately while the sets are fetched
        contentPanel.add(buildLoadingPanel("Loading sets..."), "setList");
        add(contentPanel, BorderLayout.CENTER);

        loadSets();
    }

    // -------------------------
    // Set list view
    // -------------------------

    // Kicks off an async fetch of all sets and swaps in the result (or an error panel) when done
    private void loadSets() {
        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() throws Exception {
                // Off the EDT — this hits the network
                return TCGDexClient.getAllSets();
            }

            @Override
            protected void done() {
                // Back on the EDT — safe to touch Swing
                try {
                    displaySets(get());
                } catch (Exception e) {
                    replaceSetList(buildErrorPanel("Failed to load sets: " + e.getMessage()));
                }
            }
        };
        worker.execute();
    }

    // Builds the set list screen from the fetched data and swaps it into the CardLayout
    private void displaySets(List<String[]> sets) {
        JPanel view = new JPanel(new BorderLayout());
        view.setBackground(new Color(40, 40, 50));

        // Title bar with the total set count
        JLabel title = new JLabel("📦 Browse Sets  (" + sets.size() + " sets)");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));
        view.add(title, BorderLayout.NORTH);

        // Wrapping grid of set tiles — WrapLayout flows tiles and wraps to new rows as needed
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        grid.setBackground(new Color(40, 40, 50));
        grid.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Each set is a String[] of {id, name, logoUrl, cardCount} — flat array because that's
        // what TCGDexClient returns. Could be cleaner as a small Set record/class.
        for (String[] set : sets) {
            grid.add(buildSetPanel(set[0], set[1], set[2], set[3]));
        }

        // Scrollable so long lists of sets don't overflow the window
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.setBackground(new Color(40, 40, 50));
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getViewport().setBackground(new Color(40, 40, 50));
        view.add(scroll, BorderLayout.CENTER);

        replaceSetList(view);
    }

    // Builds a single tile in the set list grid: logo on top, name + card count below
    private JPanel buildSetPanel(String setId, String setName, String logoUrl, String cardCount) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(55, 55, 70));
        panel.setPreferredSize(new Dimension(190, 135));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // tile is clickable

        // Reserved space for the logo — sized up front so the layout doesn't jump
        // when the logo finishes loading asynchronously
        JLabel logoLabel = new JLabel("", SwingConstants.CENTER);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoLabel.setPreferredSize(new Dimension(160, 60));
        logoLabel.setMaximumSize(new Dimension(160, 60));
        panel.add(logoLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        // Set name
        JLabel nameLabel = new JLabel(setName, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(nameLabel);

        // Card count subtitle
        JLabel countLabel = new JLabel(cardCount + " cards", SwingConstants.CENTER);
        countLabel.setForeground(new Color(160, 160, 200));
        countLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(countLabel);

        // Load the set logo asynchronously so the UI doesn't block on each image fetch.
        // TCGDex returns a base URL without an extension, so we append ".png" ourselves.
        if (!logoUrl.isEmpty()) {
            SwingWorker<ImageIcon, Void> logoWorker = new SwingWorker<>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    URL url = new URL(logoUrl + ".png");
                    ImageIcon icon = new ImageIcon(url);
                    // Pre-scale to the reserved label size so we're not scaling on every paint
                    Image scaled = icon.getImage().getScaledInstance(160, 55, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }

                @Override
                protected void done() {
                    try {
                        logoLabel.setIcon(get());
                        // Tile size is fixed, but revalidate/repaint is cheap insurance in case
                        // the icon ends up changing the label's preferred size
                        panel.revalidate();
                        panel.repaint();
                    } catch (Exception e) {
                        // Logo failed to load — silently leave the label blank, not worth surfacing
                    }
                }
            };
            logoWorker.execute();
        }

        // Click the tile to drill into the set; hover effects show it's interactive
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showSetCards(setId, setName);
            }
            public void mouseEntered(java.awt.event.MouseEvent e) {
                panel.setBackground(new Color(80, 80, 100));
                panel.setBorder(BorderFactory.createLineBorder(new Color(120, 120, 180), 2));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                panel.setBackground(new Color(55, 55, 70));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
            }
        });

        return panel;
    }

    // -------------------------
    // Set detail view (cards + prices)
    // -------------------------

    // Switches to the set detail screen and starts fetching the cards for the chosen set
    private void showSetCards(String setId, String setName) {
        // Show a loading placeholder immediately so the user gets feedback while the fetch runs
        JPanel loading = buildLoadingPanel("Loading " + setName + "...");
        contentPanel.add(loading, "setDetail");
        contentLayout.show(contentPanel, "setDetail");

        SwingWorker<List<CardResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<CardResult> doInBackground() throws Exception {
                return TCGDexClient.getSetCards(setId);
            }

            @Override
            protected void done() {
                try {
                    displaySetCards(setName, get());
                } catch (Exception e) {
                    contentPanel.add(buildErrorPanel("Failed to load set: " + e.getMessage()), "setDetail");
                    contentLayout.show(contentPanel, "setDetail");
                }
            }
        };
        worker.execute();
    }

    // Stores the fetched cards in the pagination state fields and renders page 0
    private void displaySetCards(String setName, List<CardResult> cards) {
        currentSetCards = cards;
        currentSetName  = setName;
        setCurrentPage  = 0;
        buildSetDetailView(0);
    }

    // Builds and displays a single page of the set detail view.
    // Called both for the initial render and on every prev/next click.
    private void buildSetDetailView(int page) {
        setCurrentPage = page;

        // Calculate pagination bounds for this page
        int totalPages = (int) Math.ceil((double) currentSetCards.size() / PAGE_SIZE);
        int from = page * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, currentSetCards.size()); // clamp the last page

        JPanel view = new JPanel(new BorderLayout());
        view.setBackground(new Color(40, 40, 50));

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
        // Manual hover effect since we disabled the L&F border/focus
        backBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { backBtn.setBackground(new Color(90, 90, 120)); }
            public void mouseExited(java.awt.event.MouseEvent e)  { backBtn.setBackground(new Color(60, 60, 80));  }
        });
        // Back button just swaps the CardLayout back to the cached set list — no re-fetch
        backBtn.addActionListener(e -> contentLayout.show(contentPanel, "setList"));

        JLabel title = new JLabel(currentSetName + "  —  " + currentSetCards.size() + " cards");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 16));

        header.add(backBtn);
        header.add(title);
        view.add(header, BorderLayout.NORTH);

        // ─── Card grid (current page only) ───────────────────────────
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        grid.setBackground(new Color(40, 40, 50));
        grid.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        // Only build tiles for the cards on this page — keeps memory and price requests bounded
        for (int i = from; i < to; i++) {
            grid.add(buildCardWithPrice(currentSetCards.get(i)));
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getViewport().setBackground(new Color(40, 40, 50));
        view.add(scroll, BorderLayout.CENTER);

        // ─── Pagination bar (only shown when there's more than one page) ───
        if (totalPages > 1) {
            JPanel pagBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
            pagBar.setBackground(new Color(30, 30, 40));

            // Prev button — disabled on the first page
            JButton prev = makePagButton("← Prev");
            prev.setEnabled(page > 0);
            prev.addActionListener(e -> buildSetDetailView(setCurrentPage - 1));

            JLabel pageLabel = new JLabel("Page " + (page + 1) + " of " + totalPages);
            pageLabel.setForeground(Color.WHITE);
            pageLabel.setFont(new Font("Arial", Font.PLAIN, 12));

            // Next button — disabled on the last page
            JButton next = makePagButton("Next →");
            next.setEnabled(page < totalPages - 1);
            next.addActionListener(e -> buildSetDetailView(setCurrentPage + 1));

            pagBar.add(prev);
            pagBar.add(pageLabel);
            pagBar.add(next);
            view.add(pagBar, BorderLayout.SOUTH);
        }

        // Swap the freshly built page into the CardLayout.
        // Note: this keeps adding new "setDetail" panels on every page change without
        // removing the old ones. CardLayout shows the most recently added one with that
        // name, so it works visually, but the old panels stick around in memory until
        // the SetsView is disposed. Worth cleaning up if memory ever becomes an issue.
        contentPanel.add(view, "setDetail");
        contentLayout.show(contentPanel, "setDetail");
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // Factory for the prev/next pagination buttons — kept separate to avoid duplicating
    // the styling and hover-effect boilerplate
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

    // Builds a card tile for the set detail grid: image on top, name and live market price below.
    // Similar to ui.components.CardPanel but adds an async price fetch.
    private JPanel buildCardWithPrice(CardResult card) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setBackground(new Color(55, 55, 70));
        panel.setPreferredSize(new Dimension(180, 295));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Card image (or fallback text)
        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        if (card.image != null) {
            imageLabel.setIcon(card.image);
        } else {
            imageLabel.setText("No image");
            imageLabel.setForeground(Color.WHITE);
        }

        // Bottom area: name on top, price below it
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.setBackground(new Color(55, 55, 70));

        JLabel nameLabel = new JLabel(card.name, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Starts as a placeholder; the background fetch below will update it
        JLabel priceLabel = new JLabel("Loading price...", SwingConstants.CENTER);
        priceLabel.setForeground(new Color(100, 210, 100)); // green to read as money
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        southPanel.add(nameLabel);
        southPanel.add(priceLabel);

        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);

        // Click to open full card details; hover to highlight.
        // southPanel needs its background updated separately from panel because it's an
        // opaque child — otherwise the bottom strip stays the old color on hover.
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

        // Fetch market price in the background using the shared price executor.
        // Using the executor (rather than a SwingWorker per card) caps concurrent
        // requests at the pool size, so 25 cards on a page won't fire 25 simultaneous calls.
        // SwingUtilities.invokeLater hops back onto the EDT before touching the label.
        priceExecutor.submit(() -> {
            try {
                String price = TCGDexClient.getCardMarketPrice(card.id);
                SwingUtilities.invokeLater(() ->
                        priceLabel.setText(price.equals("—") ? "Price: N/A" : "Market: " + price)
                );
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> priceLabel.setText("Price: N/A"));
            }
        });

        return panel;
    }

    // -------------------------
    // Helpers
    // -------------------------

    // Generic centered "Loading..." panel used as a placeholder during async fetches
    private JPanel buildLoadingPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 40, 50));
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    // Generic centered error panel — same shape as the loading panel but with a red tint
    private JPanel buildErrorPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 40, 50));
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(new Color(220, 80, 80));
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    // Swaps a new panel into the "setList" slot of the CardLayout and shows it.
    // Used by both the initial fetch result and the error fallback.
    private void replaceSetList(JPanel newPanel) {
        contentPanel.add(newPanel, "setList");
        contentLayout.show(contentPanel, "setList");
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}
