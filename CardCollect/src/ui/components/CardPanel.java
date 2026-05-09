package ui.components;

import model.CardResult;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CardPanel extends JPanel {

    public CardPanel(CardResult card, JFrame parentFrame) {

        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(55, 55, 70));
        setPreferredSize(new Dimension(180, 310)); // slightly taller for chart
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        //Image
        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);

        if (card.image != null) {
            imageLabel.setIcon(card.image);
        } else {
            imageLabel.setText("No image");
            imageLabel.setForeground(Color.WHITE);
        }

        //name
        JLabel nameLabel = new JLabel(card.name, SwingConstants.CENTER);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));

        // mini price chart
        List<Double> history = generateMiniHistory();
        MiniPriceChartPanel miniChart = new MiniPriceChartPanel(history);

        //bottom layout (name + chart)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        miniChart.setAlignmentX(Component.CENTER_ALIGNMENT);

        bottomPanel.add(nameLabel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        bottomPanel.add(miniChart);

        add(imageLabel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // click + hover
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

    //fake price history generator
    private List<Double> generateMiniHistory() {
        List<Double> history = new ArrayList<>();

        double value = 10 + Math.random() * 90;

        for (int i = 0; i < 7; i++) {
            value += (Math.random() * 6) - 3;

            if (value < 1) {
                value = 1;
            }

            history.add(Math.round(value * 100.0) / 100.0);
        }

        return history;
    }
}
