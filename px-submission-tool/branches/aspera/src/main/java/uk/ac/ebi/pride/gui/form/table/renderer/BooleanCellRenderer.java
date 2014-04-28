package uk.ac.ebi.pride.gui.form.table.renderer;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class BooleanCellRenderer extends JLabel implements TableCellRenderer {

    private static final Color CORRECT_FILE_MAPPING_COLOUR = new Color(40, 175, 99, 100);
    private static final Color INCORRECT_FILE_MAPPING_COLOUR = new Color(215, 39, 41, 100);


    private boolean valid;

    public BooleanCellRenderer() {
        this.setOpaque(true);
        this.setHorizontalTextPosition(SwingConstants.CENTER);
        this.setFont(this.getFont().deriveFont(Font.BOLD));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        valid = (Boolean) value;
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        int width = getWidth();
        int height = getHeight();

        // rendering hints
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // paint a background
        Color background;

        if (valid) {
            background = CORRECT_FILE_MAPPING_COLOUR;
        } else {
            background = INCORRECT_FILE_MAPPING_COLOUR;
        }

        g2.setColor(background);
        g2.fillRect(0, 0, width, height);

        // paint text
        g2.setColor(Color.black);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD));
        FontMetrics fontMetrics = g2.getFontMetrics();

        String text = (valid ? "Yes" : "No") + "";
        int textWidth = fontMetrics.stringWidth(text);
        int xPos = (width - textWidth) / 2;
        int yPos = height / 2 + fontMetrics.getDescent() + 2;
        g2.drawString(text, xPos, yPos);

        g2.dispose();
    }
}
