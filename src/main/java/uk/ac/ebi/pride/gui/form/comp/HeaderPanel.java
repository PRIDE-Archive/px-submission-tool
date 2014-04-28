package uk.ac.ebi.pride.gui.form.comp;

import javax.swing.*;
import java.awt.*;

/**
 * Panel with predefine background
 *
 * @author Rui Wang
 * @version $Id$
 */
public class HeaderPanel extends JPanel {

    public HeaderPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    public HeaderPanel(LayoutManager layout) {
        super(layout);
    }

    public HeaderPanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    public HeaderPanel() {
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // create graphics g
        Graphics2D g2 = (Graphics2D) g.create();

        // rendering hints
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // paint background
        g2.setPaint(new GradientPaint(0, 0, Color.darkGray, 0, getHeight(), Color.black));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.dispose();

    }
}
