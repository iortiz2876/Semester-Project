package ui.views;

import javax.swing.*;
import java.awt.*;

public class SetsView extends JPanel {
    public SetsView() {
        setLayout(new BorderLayout());
        setBackground(new Color(40, 40, 50));
        JLabel label = new JLabel("📦 Browse sets will appear here.",
                SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        add(label, BorderLayout.CENTER);
    }
}
