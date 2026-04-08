package ui.views;

import javax.swing.*;
import java.awt.*;

public class PricesView extends JPanel {
    public PricesView() {
        setLayout(new BorderLayout());
        setBackground(new Color(40, 40, 50));
        JLabel label = new JLabel("💰 Price tracker will appear here.",
                SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        add(label, BorderLayout.CENTER);
    }
}