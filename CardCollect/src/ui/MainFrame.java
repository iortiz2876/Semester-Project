package ui;

import model.User;
import storage.CardStorage;
import ui.views.*;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private JPanel contentPanel;
    private CardLayout cardLayout;
    private User currentUser;

    // Lazy-loaded views
    private SearchView searchView;
    private CollectionView collectionView;
    private WishlistView wishlistView;
    private MessagesView messagesView;
    private SetsView setsView;
    private PricesView pricesView;

    public MainFrame() {
        setTitle("Card Collect");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        showLogin();
    }

    private void showLogin() {
        LoginView loginView = new LoginView(user -> {
            long totalStart = System.currentTimeMillis();

            currentUser = user;
            CardStorage.setCurrentUser(user.id);

            getContentPane().removeAll();

            long initStart = System.currentTimeMillis();
            initComponents();
            long initEnd = System.currentTimeMillis();
            System.out.println("[TIMING] MainFrame.initComponents(): " + (initEnd - initStart) + " ms");

            revalidate();
            repaint();

            long totalEnd = System.currentTimeMillis();
            System.out.println("[TIMING] Total post-login UI setup: " + (totalEnd - totalStart) + " ms");
        });

        setLayout(new BorderLayout());
        add(loginView, BorderLayout.CENTER);
    }

    private void logout() {
        currentUser = null;
        CardStorage.setCurrentUser(null);

        // Clear lazy-loaded views so a new user gets a fresh app state
        searchView = null;
        collectionView = null;
        wishlistView = null;
        messagesView = null;
        setsView = null;
        pricesView = null;

        getContentPane().removeAll();
        showLogin();
        revalidate();
        repaint();
    }

    private void initComponents() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(30, 30, 40));
        sidebar.setPreferredSize(new Dimension(180, getHeight()));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel titleLabel = new JLabel("Card Collect");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        sidebar.add(titleLabel);

        sidebar.add(createSidebarButton("🔎  Search", () -> showView("search")));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        sidebar.add(createSidebarButton("⭐  My Collection", () -> showView("collection")));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        sidebar.add(createSidebarButton("❤️  WishList", () -> showView("wishlist")));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        sidebar.add(createSidebarButton("💬  Messages", () -> showView("messages")));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        sidebar.add(createSidebarButton("📦  Browse Sets", () -> showView("sets")));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        sidebar.add(createSidebarButton("💰  Prices", () -> showView("prices")));

        sidebar.add(Box.createVerticalGlue());

        JButton logoutButton = new JButton("✖️  Logout");
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setMaximumSize(new Dimension(160, 45));
        logoutButton.setPreferredSize(new Dimension(160, 45));
        logoutButton.setBackground(new Color(140, 50, 50));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorderPainted(false);
        logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String[] emojiFonts = {"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji"};
        Font emojiFont = new Font("Arial", Font.PLAIN, 13);
        for (String name : emojiFonts) {
            Font f = new Font(name, Font.PLAIN, 13);
            if (!f.getFamily().equals("Dialog")) {
                emojiFont = f;
                break;
            }
        }
        logoutButton.setFont(emojiFont);

        logoutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                logoutButton.setBackground(new Color(170, 70, 70));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                logoutButton.setBackground(new Color(140, 50, 50));
            }
        });

        logoutButton.addActionListener(e -> logout());
        sidebar.add(logoutButton);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Only create the fast/default views up front
        long t;

        t = System.currentTimeMillis();
        searchView = new SearchView(this);
        contentPanel.add(searchView, "search");
        System.out.println("[TIMING] SearchView constructor: " + (System.currentTimeMillis() - t) + " ms");

        t = System.currentTimeMillis();
        pricesView = new PricesView();
        contentPanel.add(pricesView, "prices");
        System.out.println("[TIMING] PricesView constructor: " + (System.currentTimeMillis() - t) + " ms");

        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        cardLayout.show(contentPanel, "search");
    }

    private void showView(String viewName) {
        long t = System.currentTimeMillis();

        switch (viewName) {
            case "search" -> {
                if (searchView == null) {
                    long ctor = System.currentTimeMillis();
                    searchView = new SearchView(this);
                    contentPanel.add(searchView, "search");
                    System.out.println("[TIMING] SearchView lazy constructor: " + (System.currentTimeMillis() - ctor) + " ms");
                }
            }

            case "collection" -> {
                if (collectionView == null) {
                    long ctor = System.currentTimeMillis();
                    collectionView = new CollectionView();
                    contentPanel.add(collectionView, "collection");
                    System.out.println("[TIMING] CollectionView constructor: " + (System.currentTimeMillis() - ctor) + " ms");
                }

                long refresh = System.currentTimeMillis();
                collectionView.refresh();
                System.out.println("[TIMING] CollectionView.refresh(): " + (System.currentTimeMillis() - refresh) + " ms");
            }

            case "wishlist" -> {
                if (wishlistView == null) {
                    long ctor = System.currentTimeMillis();
                    wishlistView = new WishlistView();
                    contentPanel.add(wishlistView, "wishlist");
                    System.out.println("[TIMING] WishlistView constructor: " + (System.currentTimeMillis() - ctor) + " ms");
                }

                long refresh = System.currentTimeMillis();
                wishlistView.refresh();
                System.out.println("[TIMING] WishlistView.refresh(): " + (System.currentTimeMillis() - refresh) + " ms");
            }

            case "messages" -> {
                if (messagesView == null) {
                    long ctor = System.currentTimeMillis();
                    messagesView = new MessagesView(currentUser);
                    contentPanel.add(messagesView, "messages");
                    System.out.println("[TIMING] MessagesView constructor: " + (System.currentTimeMillis() - ctor) + " ms");
                }

                long refresh = System.currentTimeMillis();
                messagesView.refreshUsers();
                System.out.println("[TIMING] MessagesView.refreshUsers(): " + (System.currentTimeMillis() - refresh) + " ms");
            }

            case "sets" -> {
                if (setsView == null) {
                    long ctor = System.currentTimeMillis();
                    setsView = new SetsView(this);
                    contentPanel.add(setsView, "sets");
                    System.out.println("[TIMING] SetsView constructor: " + (System.currentTimeMillis() - ctor) + " ms");
                }
            }

            case "prices" -> {
                if (pricesView == null) {
                    long ctor = System.currentTimeMillis();
                    pricesView = new PricesView();
                    contentPanel.add(pricesView, "prices");
                    System.out.println("[TIMING] PricesView lazy constructor: " + (System.currentTimeMillis() - ctor) + " ms");
                }
            }

            default -> {
                System.out.println("[TIMING] Unknown view requested: " + viewName);
                return;
            }
        }

        cardLayout.show(contentPanel, viewName);
        revalidate();
        repaint();

        System.out.println("[TIMING] showView(\"" + viewName + "\"): " + (System.currentTimeMillis() - t) + " ms");
    }

    private JButton createSidebarButton(String text, Runnable onSwitch) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(160, 45));
        button.setPreferredSize(new Dimension(160, 45));
        button.setBackground(new Color(50, 50, 65));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String[] emojiFonts = {"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji"};
        Font emojiFont = new Font("Arial", Font.PLAIN, 13);
        for (String name : emojiFonts) {
            Font f = new Font(name, Font.PLAIN, 13);
            if (!f.getFamily().equals("Dialog")) {
                emojiFont = f;
                break;
            }
        }
        button.setFont(emojiFont);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(80, 80, 100));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(50, 50, 65));
            }
        });

        button.addActionListener(e -> {
            if (onSwitch != null) {
                onSwitch.run();
            }
        });

        return button;
    }
}