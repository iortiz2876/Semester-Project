package ui.views;

import api.TCGDexClient;
import ui.WrapLayout;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// View that lets the user browse all available card sets, then drill into a set
// to see its cards. Uses a CardLayout with two screens:
//   - "setList"   -> paginated grid of all sets
//   - "setDetail" -> a SetDetailView that handles the paginated card grid
public class SetsView extends JPanel {

    private static final int PAGE_SIZE = 15;

    private final JFrame parentFrame;

    private final JPanel contentPanel;
    private final CardLayout contentLayout;
    private final SetDetailView setDetailView;

    // Current panel used for the "setList" card so we can replace it cleanly
    private JPanel currentSetListPanel;

    // Cached set data for pagination
    private List<String[]> allSets = new ArrayList<>();
    private int currentPage = 0;

    public SetsView(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(40, 40, 50));

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setBackground(new Color(40, 40, 50));

        // Set list placeholder first so startup always lands here
        currentSetListPanel = buildLoadingPanel("Loading sets...");
        contentPanel.add(currentSetListPanel, "setList");

        // Reusable detail view
        setDetailView = new SetDetailView(parentFrame, () -> {
            long t = System.currentTimeMillis();
            contentLayout.show(contentPanel, "setList");
            contentPanel.revalidate();
            contentPanel.repaint();
            System.out.println("[TIMING] Back to setList show(): " + (System.currentTimeMillis() - t) + " ms");
        });
        contentPanel.add(setDetailView, "setDetail");

        add(contentPanel, BorderLayout.CENTER);

        // Explicitly show the set list on startup
        contentLayout.show(contentPanel, "setList");

        loadSets();
    }

    // -------------------------
    // Data loading
    // -------------------------

    private void loadSets() {
        long loadStart = System.currentTimeMillis();

        SwingWorker<List<String[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String[]> doInBackground() throws Exception {
                long apiStart = System.currentTimeMillis();
                List<String[]> result = TCGDexClient.getAllSets();
                long apiEnd = System.currentTimeMillis();
                System.out.println("[TIMING] TCGDexClient.getAllSets(): " + (apiEnd - apiStart) + " ms");
                return result;
            }

            @Override
            protected void done() {
                try {
                    allSets = get();
                    currentPage = 0;

                    long displayStart = System.currentTimeMillis();
                    showSetPage(0);
                    long displayEnd = System.currentTimeMillis();

                    System.out.println("[TIMING] first showSetPage(): " + (displayEnd - displayStart) + " ms");
                    System.out.println("[TIMING] Total loadSets() end-to-end: " + (System.currentTimeMillis() - loadStart) + " ms");
                } catch (Exception e) {
                    replaceSetList(buildErrorPanel("Failed to load sets: " + e.getMessage()));
                }
            }
        };
        worker.execute();
    }

    // -------------------------
    // Pagination / rendering
    // -------------------------

    private void showSetPage(int page) {
        if (allSets == null || allSets.isEmpty()) {
            replaceSetList(buildErrorPanel("No sets found."));
            return;
        }

        currentPage = page;

        int totalPages = (int) Math.ceil((double) allSets.size() / PAGE_SIZE);
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, allSets.size());

        long buildStart = System.currentTimeMillis();

        JPanel view = new JPanel(new BorderLayout());
        view.setBackground(new Color(40, 40, 50));

        JLabel title = new JLabel("Browse Sets  (" + allSets.size() + " sets)");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));
        view.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        grid.setBackground(new Color(40, 40, 50));
        grid.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        for (int i = from; i < to; i++) {
            String[] set = allSets.get(i);
            grid.add(buildSetPanel(set[0], set[1], set[2], set[3]));
        }

        System.out.println("[TIMING] build visible set tiles (" + (to - from) + "): " + (System.currentTimeMillis() - buildStart) + " ms");

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.setBackground(new Color(40, 40, 50));
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getViewport().setBackground(new Color(40, 40, 50));
        view.add(scroll, BorderLayout.CENTER);

        view.add(buildPaginationBar(totalPages), BorderLayout.SOUTH);

        replaceSetList(view);
    }

    private JPanel buildPaginationBar(int totalPages) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        bar.setBackground(new Color(30, 30, 40));
        bar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton prev = makePagButton("← Prev");
        prev.setEnabled(currentPage > 0);
        prev.addActionListener(e -> showSetPage(currentPage - 1));

        JLabel pageLabel = new JLabel(
                "Page " + (currentPage + 1) + " of " + totalPages +
                        "  (" + allSets.size() + " sets)"
        );
        pageLabel.setForeground(Color.WHITE);
        pageLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton next = makePagButton("Next →");
        next.setEnabled(currentPage < totalPages - 1);
        next.addActionListener(e -> showSetPage(currentPage + 1));

        bar.add(prev);
        bar.add(pageLabel);
        bar.add(next);

        return bar;
    }

    private JButton makePagButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(60, 60, 80));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Arial", Font.PLAIN, 12));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(new Color(90, 90, 120));
                }
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(60, 60, 80));
            }
        });

        return btn;
    }

    // -------------------------
    // Set tile
    // -------------------------

    private JPanel buildSetPanel(String setId, String setName, String logoUrl, String cardCount) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(55, 55, 70));
        panel.setPreferredSize(new Dimension(190, 145));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel logoLabel = new JLabel("", SwingConstants.CENTER);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoLabel.setPreferredSize(new Dimension(160, 60));
        logoLabel.setMaximumSize(new Dimension(160, 60));
        panel.add(logoLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel nameLabel = new JLabel(setName, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(nameLabel);

        // Only visible tiles load logos now, so pagination dramatically reduces startup cost
        if (!logoUrl.isEmpty()) {
            SwingWorker<ImageIcon, Void> logoWorker = new SwingWorker<>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    URL url = new URL(logoUrl + ".png");
                    ImageIcon icon = new ImageIcon(url);
                    Image scaled = icon.getImage().getScaledInstance(160, 55, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }

                @Override
                protected void done() {
                    try {
                        logoLabel.setIcon(get());
                        panel.revalidate();
                        panel.repaint();
                    } catch (Exception e) {
                        // Leave blank if logo load fails
                    }
                }
            };
            logoWorker.execute();
        } else {
            try {
                URL url = getClass().getResource("/resources/images/pngimg.com - pokemon_logo_PNG3.png");
                ImageIcon icon = new ImageIcon(url);
                Image scaled = icon.getImage().getScaledInstance(160, 55, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaled));
            } catch (Exception e) {
                // Ignore fallback image failure
            }
        }

        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                long t = System.currentTimeMillis();
                setDetailView.loadSet(setId, setName);
                contentLayout.show(contentPanel, "setDetail");
                System.out.println("[TIMING] Open set detail click handler: " + (System.currentTimeMillis() - t) + " ms");
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
    // Helpers
    // -------------------------

    private JPanel buildLoadingPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 40, 50));

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(Color.LIGHT_GRAY);
        label.setFont(new Font("Arial", Font.PLAIN, 14));

        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildErrorPanel(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 40, 50));

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(new Color(220, 80, 80));
        label.setFont(new Font("Arial", Font.PLAIN, 14));

        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private void replaceSetList(JPanel newPanel) {
        long t = System.currentTimeMillis();

        contentPanel.remove(currentSetListPanel);
        currentSetListPanel = newPanel;
        contentPanel.add(currentSetListPanel, "setList");

        contentLayout.show(contentPanel, "setList");
        contentPanel.revalidate();
        contentPanel.repaint();

        System.out.println("[TIMING] replaceSetList(): " + (System.currentTimeMillis() - t) + " ms");
    }
}