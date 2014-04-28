package uk.ac.ebi.pride.gui.form.comp;

import java.awt.*;

/**
 * Panel with rounded edge and an gray gradient rectangle inside
 *
 * @author Rui Wang
 * @version $Id$
 */
public class GradientRoundedPanel extends RoundedPanel{

    private int gradientGap = 8;
    private Color gradientStartColour = new Color(242, 242, 242);
    private Color gradientEndColour = Color.lightGray;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // create graphics g
        Graphics2D g2 = (Graphics2D)g.create();

        // rendering hints
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int xPos = gradientGap;
        int yPos = gradientGap;
        int width = getWidth() - gradientGap*2 - getShadowGap();
        int height = getHeight() - gradientGap*2 - getShadowGap();

        // paint background
        g2.setPaint(new GradientPaint(0, 0, gradientStartColour, 0, height, gradientEndColour));
        g2.fillRect(xPos, yPos, width, height);

        g2.setPaint(gradientEndColour);
        g2.drawRect(xPos, yPos, width, height);

        g2.dispose();
    }

    public int getGradientGap() {
        return gradientGap;
    }

    public void setGradientGap(int gradientGap) {
        this.gradientGap = gradientGap;
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
