package uk.ac.ebi.pride.gui.navigation;

import javax.swing.*;
import java.awt.*;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class NavigationControlPanel extends JPanel {

    public NavigationControlPanel() {
        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // create new graphics
        Graphics2D g2 = (Graphics2D) g.create();

        // paint background
        g2.setPaint(new GradientPaint(0, 0, Color.darkGray, 0, getHeight(), Color.black));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // dispose
        g2.dispose();
    }
}
