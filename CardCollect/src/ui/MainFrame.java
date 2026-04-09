package ui;

import ui.views.*;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private JPanel contentPanel;
    private CardLayout cardLayout;

    public MainFrame() {
        setTitle("Pokémon TCG Browser");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(30, 30, 40));
        sidebar.setPreferredSize(new Dimension(180, getHeight()));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel titleLabel = new JLabel("TCG Browser");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        sidebar.add(titleLabel);

        sidebar.add(createSidebarButton("🔎  Search", "search"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createSidebarButton("⭐  My Collection", "collection"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createSidebarButton("❤️  WishList", "wishlist"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createSidebarButton("📦  Browse Sets", "sets"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(createSidebarButton("💰  Prices", "prices"));
        sidebar.add(Box.createVerticalGlue());

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(new SearchView(this), "search");
        contentPanel.add(new CollectionView(), "collection");
        contentPanel.add(new WishlistView(), "wishlist");
        contentPanel.add(new SetsView(), "sets");
        contentPanel.add(new PricesView(), "prices");

        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        cardLayout.show(contentPanel, "search");
    }

    private JButton createSidebarButton(String text, String viewName) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(160, 45));
        button.setPreferredSize(new Dimension(160, 45));
        button.setBackground(new Color(50, 50, 65));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        //have the emojis load on all different types of OS
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


        button.addActionListener(e -> cardLayout.show(contentPanel, viewName));
        return button;
    }
}