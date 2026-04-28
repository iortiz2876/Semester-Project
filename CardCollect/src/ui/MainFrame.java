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

    public MainFrame() {
        setTitle("Card Collect");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        showLogin();
    }

    private void showLogin() {
        LoginView loginView = new LoginView(user -> {
            currentUser = user;
            CardStorage.setCurrentUser(user.id);
            getContentPane().removeAll();
            initComponents();
            revalidate();
            repaint();
        });

        setLayout(new BorderLayout());
        add(loginView, BorderLayout.CENTER);
    }

    private void logout() {
        currentUser = null;
        CardStorage.setCurrentUser(null);
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

        CollectionView collectionView = new CollectionView();
        WishlistView wishlistView = new WishlistView();
        MessagesView messagesView = new MessagesView(currentUser);
        PricesView pricesView = new PricesView();
        UserCollectionsView userCollectionsView = new UserCollectionsView(currentUser);

        sidebar.add(createSidebarButton("🔎  Search", "search", null));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createSidebarButton("⭐  My Collection", "collection", collectionView::refresh));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createSidebarButton("❤️  WishList", "wishlist", wishlistView::refresh));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createSidebarButton("💬  Messages", "messages", messagesView::refreshUsers));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createSidebarButton("👥  Users", "users", userCollectionsView::refreshUsers));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createSidebarButton("📦  Browse Sets", "sets", null));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createSidebarButton("💰  Prices", "prices", pricesView::refreshCards));

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
        contentPanel.add(new SearchView(this), "search");
        contentPanel.add(collectionView, "collection");
        contentPanel.add(wishlistView, "wishlist");
        contentPanel.add(messagesView, "messages");
        contentPanel.add(userCollectionsView, "users");
        contentPanel.add(new SetsView(this), "sets");
        contentPanel.add(pricesView, "prices");

        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JButton createSidebarButton(String text, String viewName, Runnable onSwitch) {
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
            cardLayout.show(contentPanel, viewName);
            if (onSwitch != null) onSwitch.run();
        });

        return button;
    }
}