package ui.components;

import model.CardResult;

import javax.swing.*;
import java.awt.*;

// A clickable thumbnail tile for a single card. Used in the search results grid —
// shows the card's image and name, and opens a CardDetailDialog when clicked.
public class CardPanel extends JPanel {

    public CardPanel(CardResult card, JFrame parentFrame) {
        // BorderLayout with 5px gaps so the image and name label have a little breathing room
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(55, 55, 70)); // dark slate, matches the rest of the app's theme
        setPreferredSize(new Dimension(180, 270)); // fixed tile size so the grid stays uniform
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // inner padding around the contents
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // pointer cursor signals the tile is clickable

        // Card image (or fallback text if the image failed to load), centered in the tile
        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        if (card.image != null) {
            imageLabel.setIcon(card.image);
        } else {
            imageLabel.setText("No image");
            imageLabel.setForeground(Color.WHITE);
        }

        // Card name shown beneath the image
        JLabel nameLabel = new JLabel(card.name, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));

        // Image fills the center, name sits at the bottom
        add(imageLabel, BorderLayout.CENTER);
        add(nameLabel, BorderLayout.SOUTH);

        // Mouse handling: click to open details, hover to highlight
        addMouseListener(new java.awt.event.MouseAdapter() {
            // Click anywhere on the tile opens the full card detail dialog
            public void mouseClicked(java.awt.event.MouseEvent e) {
                new CardDetailDialog(parentFrame, card).setVisible(true);
            }

            // Hover effect: lighten the background and add a colored border to show focus
            public void mouseEntered(java.awt.event.MouseEvent e) {
                setBackground(new Color(80, 80, 100));
                setBorder(BorderFactory.createLineBorder(new Color(120, 120, 180), 2));
            }

            // Revert to the normal background and empty border on mouse exit
            public void mouseExited(java.awt.event.MouseEvent e) {
                setBackground(new Color(55, 55, 70));
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
        });
    }
}
