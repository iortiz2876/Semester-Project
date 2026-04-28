package ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PriceChartPanel extends JPanel {

    private List<String> labels = new ArrayList<>();
    private List<Double> prices = new ArrayList<>();

    public PriceChartPanel() {
        setPreferredSize(new Dimension(500, 320));
        setBackground(new Color(40, 40, 50));
        setBorder(BorderFactory.createLineBorder(new Color(70, 70, 90)));
    }

    public void setPriceHistory(List<String> labels, List<Double> prices) {
        this.labels = (labels != null) ? labels : new ArrayList<>();
        this.prices = (prices != null) ? prices : new ArrayList<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int leftPad = 55;
        int rightPad = 20;
        int topPad = 25;
        int bottomPad = 45;

        if (prices == null || prices.size() < 2) {
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("No price history available", 20, 25);
            g2.dispose();
            return;
        }

        double min = prices.stream().min(Double::compareTo).orElse(0.0);
        double max = prices.stream().max(Double::compareTo).orElse(1.0);

        if (max == min) {
            max = min + 1.0;
        }

        int graphX = leftPad;
        int graphY = topPad;
        int graphW = width - leftPad - rightPad;
        int graphH = height - topPad - bottomPad;

        g2.setColor(new Color(200, 200, 210));
        g2.drawLine(graphX, graphY + graphH, graphX + graphW, graphY + graphH);
        g2.drawLine(graphX, graphY, graphX, graphY + graphH);

        g2.setColor(new Color(90, 90, 110));
        for (int i = 0; i <= 4; i++) {
            int y = graphY + (i * graphH / 4);
            g2.drawLine(graphX, y, graphX + graphW, y);
        }

        g2.setColor(Color.WHITE);
        g2.drawString(String.format("$%.2f", max), 8, graphY + 5);
        g2.drawString(String.format("$%.2f", min), 8, graphY + graphH);

        int n = prices.size();
        int prevX = -1;
        int prevY = -1;

        double firstPrice = prices.get(0);
        double lastPrice = prices.get(prices.size() - 1);

        if (lastPrice > firstPrice) {
            g2.setColor(new Color(0, 200, 0));
        } else if (lastPrice < firstPrice) {
            g2.setColor(new Color(200, 0, 0));
        } else {
            g2.setColor(new Color(180, 180, 180));
        }

        for (int i = 0; i < n; i++) {
            double value = prices.get(i);

            int x = graphX + (i * graphW / (n - 1));
            int y = graphY + graphH - (int) ((value - min) / (max - min) * graphH);

            if (prevX != -1) {
                g2.drawLine(prevX, prevY, x, y);
            }

            g2.fillOval(x - 4, y - 4, 8, 8);

            prevX = x;
            prevY = y;
        }

        g2.setColor(new Color(220, 220, 230));
        for (int i = 0; i < labels.size() && i < prices.size(); i++) {
            int x = graphX + (i * graphW / (n - 1));
            String label = labels.get(i);
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(label);
            g2.drawString(label, x - textW / 2, graphY + graphH + 20);
        }

        g2.dispose();
    }
}