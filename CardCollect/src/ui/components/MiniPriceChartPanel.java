package ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MiniPriceChartPanel extends JPanel {
    private final List<Double> prices;

    public MiniPriceChartPanel(List<Double> prices) {
        this.prices = prices;
        setPreferredSize(new Dimension(150, 36));
        setMaximumSize(new Dimension(150, 36));
        setMinimumSize(new Dimension(150, 36));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (prices == null || prices.size() < 2) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int pad = 4;

        double min = prices.stream().min(Double::compareTo).orElse(0.0);
        double max = prices.stream().max(Double::compareTo).orElse(1.0);

        if (max == min) {
            max = min + 1.0;
        }

        double firstPrice = prices.get(0);
        double lastPrice = prices.get(prices.size() - 1);

        if (lastPrice > firstPrice) {
            g2.setColor(new Color(0, 200, 0));
        } else if (lastPrice < firstPrice) {
            g2.setColor(new Color(200, 0, 0));
        } else {
            g2.setColor(new Color(180, 180, 180));
        }

        int prevX = -1;
        int prevY = -1;

        for (int i = 0; i < prices.size(); i++) {
            double value = prices.get(i);

            int x = pad + i * (width - 2 * pad) / (prices.size() - 1);
            int y = height - pad - (int) ((value - min) / (max - min) * (height - 2 * pad));

            if (prevX != -1) {
                g2.drawLine(prevX, prevY, x, y);
            }

            g2.fillOval(x - 2, y - 2, 4, 4);

            prevX = x;
            prevY = y;
        }

        g2.dispose();
    }
}