package com.library.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A small bar chart drawn with plain Java2D (no external libraries). */
public class BarChartPanel extends JPanel {

    private static final Color BAR_COLOR = new Color(66, 133, 244);

    private final String title;
    private Map<String, Integer> data = new LinkedHashMap<>();

    public BarChartPanel(String title) {
        this.title = title;
        setPreferredSize(new Dimension(380, 230));
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
    }

    public void setData(Map<String, Integer> newData) {
        this.data = new LinkedHashMap<>(newData);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        Color text = getForeground();

        g2.setColor(text);
        g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
        g2.drawString(title, 10, 20);

        FontMetrics fm = g2.getFontMetrics(getFont().deriveFont(11f));
        g2.setFont(getFont().deriveFont(11f));

        if (data.isEmpty()) {
            String msg = "No data yet";
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            g2.dispose();
            return;
        }

        int left = 34, right = 10, top = 34, bottom = 36;
        int chartW = w - left - right;
        int chartH = h - top - bottom;
        int max = Math.max(1, Collections.max(data.values()));

        // axes
        g2.setColor(text);
        g2.drawLine(left, top, left, top + chartH);
        g2.drawLine(left, top + chartH, left + chartW, top + chartH);
        g2.drawString("0", left - 4 - fm.stringWidth("0"), top + chartH + 4);
        String maxText = String.valueOf(max);
        g2.drawString(maxText, left - 4 - fm.stringWidth(maxText), top + 12);

        int n = data.size();
        int slot = chartW / n;
        int barW = Math.max(8, slot * 2 / 3);
        int maxChars = Math.max(3, slot / 7);
        int usable = chartH - 16; // room above the tallest bar for its value

        int i = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            int value = entry.getValue();
            int barH = (int) ((long) value * usable / max);
            int x = left + i * slot + (slot - barW) / 2;
            int y = top + chartH - barH;

            g2.setColor(BAR_COLOR);
            g2.fillRect(x, y, barW, barH);

            g2.setColor(text);
            String v = String.valueOf(value);
            g2.drawString(v, x + (barW - fm.stringWidth(v)) / 2, y - 3);

            String label = entry.getKey();
            if (label.length() > maxChars) {
                label = label.substring(0, maxChars - 1) + "…";
            }
            g2.drawString(label, x + (barW - fm.stringWidth(label)) / 2, top + chartH + 14);
            i++;
        }
        g2.dispose();
    }
}
