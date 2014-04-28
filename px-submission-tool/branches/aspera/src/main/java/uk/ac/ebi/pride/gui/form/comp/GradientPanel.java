package uk.ac.ebi.pride.gui.form.comp;

import javax.swing.*;
import java.awt.*;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class GradientPanel extends JPanel{

    private Color gradientStartColour = new Color(242, 242, 242);
    private Color gradientEndColour = Color.lightGray;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // create graphics g
        Graphics2D g2 = (Graphics2D)g.create();

        // rendering hints
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // paint background
        g2.setPaint(new GradientPaint(0, 0, gradientStartColour, 0, height, gradientEndColour));
        g2.fillRect(0, 0, width, height);

        g2.setPaint(gradientEndColour);
        g2.drawRect(0, 0, width, height);

        g2.dispose();
    }

    public Color getGradientStartColour() {
        return gradientStartColour;
    }

    public void setGradientStartColour(Color gradientStartColour) {
        this.gradientStartColour = gradientStartColour;
    }

    public Color getGradientEndColour() {
        return gradientEndColour;
    }

    public void setGradientEndColour(Color gradientEndColour) {
        this.gradientEndColour = gradientEndColour;
    }
}

