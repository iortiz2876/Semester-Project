package ui.components;

import model.CardResult;

import javax.swing.*;
import java.awt.*;

public class CardPanel extends JPanel {

    public CardPanel(CardResult card, JFrame parentFrame) {
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(55, 55, 70));
        setPreferredSize(new Dimension(180, 270));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        if (card.image != null) {
            imageLabel.setIcon(card.image);
        } else {
            imageLabel.setText("No image");
            imageLabel.setForeground(Color.WHITE);
        }

        JLabel nameLabel = new JLabel(card.name, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));

        add(imageLabel, BorderLayout.CENTER);
        add(nameLabel, BorderLayout.SOUTH);

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                new CardDetailDialog(parentFrame, card).setVisible(true);
            }
            public void mouseEntered(java.awt.event.MouseEvent e) {
                setBackground(new Color(80, 80, 100));
                setBorder(BorderFactory.createLineBorder(new Color(120, 120, 180), 2));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                setBackground(new Color(55, 55, 70));
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
        });
    }
}
