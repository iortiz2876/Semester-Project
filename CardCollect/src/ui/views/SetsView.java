package ui.views;

import api.TCGDexClient;
import ui.WrapLayout;
import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.List;

// View that lets the user browse all available card sets, then drill into a set
// to see its cards. Uses a CardLayout with two screens:
//   - "setList"   → the grid of all sets (owned by this class)
//   - "setDetail" → a SetDetailView that handles the paginated card grid
//
// The set detail screen used to live here too, but it was extracted into SetDetailView
// so it could reuse PaginatedGridView for free.
public class SetsView extends JPanel {

    private final JFrame parentFrame; // needed so detail dialogs can be modal to the main window

    // CardLayout container that swaps between the set list and set detail screens
    private final JPanel contentPanel;
    private final CardLayout contentLayout;

    // Reusable set detail screen — created once and reused for every set the user clicks.
    // loadSet() swaps its contents in place instead of building a new view each time.
    private final SetDetailView setDetailView;

    public SetsView(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(40, 40, 50));

        // CardLayout lets us swap between the set list and set detail views in place
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setBackground(new Color(40, 40, 50));

        // Build the set detail view once. Its "back" button just flips the CardLayout
        // back to the set list — no re-fetch needed since we cached it.
        setDetailView = new SetDetailView(parentFrame,
                () -> contentLayout.show(contentPanel, "setList"));
        contentPanel.add(setDetailView, "setDetail");

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
                return TCGDexClient.getAllSets();
            }

            @Override
            protected void done() {
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

        // Wrapping grid of set tiles
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        grid.setBackground(new Color(40, 40, 50));
        grid.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Each set is a String[] of {id, name, logoUrl, cardCount} — flat array because
        // that's what TCGDexClient returns. Could be cleaner as a small Set record.
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
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Reserved space for the logo — sized up front so the layout doesn't jump
        // when the logo finishes loading asynchronously
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

        // Load the set logo asynchronously so the UI doesn't block on each image fetch.
        // TCGDex returns a base URL without an extension, so we append ".png" ourselves.
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
                        // Logo failed to load — silently leave the label blank
                    }
                }
            };
            logoWorker.execute();
        } else { //print the pokemon logo
            try {
                URL url = getClass().getResource("/resources/images/pngimg.com - pokemon_logo_PNG3.png");
                ImageIcon icon = new ImageIcon(url);
                Image scaled = icon.getImage().getScaledInstance(160, 55, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaled));
            } catch (Exception e) {
                // Fallback image missing — leave the label blank
            }
        }



        // Click the tile to drill into the set; hover effects show it's interactive
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Tell the shared detail view to load this set and swap it into view
                setDetailView.loadSet(setId, setName);
                contentLayout.show(contentPanel, "setDetail");
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
