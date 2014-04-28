package uk.ac.ebi.pride.gui.form.comp;

import javax.swing.*;
import java.awt.*;

/**
 * Rounded panel
 *
 * @author Rui Wang
 * @version $Id$
 */
public class RoundedPanel extends JPanel {

    /**
     * Colour of the border
     */
    private Color borderColour = Color.black;

    /**
     * Colour for drop shadow
     */
    private Color shadowColour = Color.black;

    /**
     * Transparency of drop shadow
     */
    private int shadowAlpha = 150;

    /**
     * Distance between shadow border and opaque panel border
     */
    private int shadowGap = 5;
    /**
     * offset of drop shadow
     */
    private int shadowOffset = 4;
    /**
     * True if want to show drop shadow
     */
    private boolean shady = true;
    /**
     * stroke size
     */
    private int strokeSize = 1;
    /**
     * radius of corner arcs
     */
    private Dimension arcs = new Dimension(20, 20);


    public RoundedPanel() {
        super();
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2d = (Graphics2D) graphics.create();
        // antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int sGap = getShadowGap();
        Color sColour = new Color(shadowColour.getRed(), shadowColour.getBlue(), shadowColour.getBlue(), shadowAlpha);

        if (shady) {
            g2d.setColor(sColour);
            g2d.fillRoundRect(shadowOffset, shadowOffset,
                    width - strokeSize - shadowOffset, height - strokeSize - shadowOffset,
                    arcs.width, arcs.height);
        } else {
            sGap = 1;
        }

        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, width - sGap, height - sGap, arcs.width, arcs.height);

        g2d.setColor(getBorderColour());
        g2d.setStroke(new BasicStroke(strokeSize));
        g2d.drawRoundRect(0, 0, width - sGap, height - sGap, arcs.width, arcs.height);

        g2d.dispose();
    }

    public Color getBorderColour() {
        return borderColour;
    }

    public void setBorderColour(Color borderColour) {
        this.borderColour = borderColour;
    }

    public Color getShadowColour() {
        return shadowColour;
    }

    public void setShadowColour(Color shadowColour) {
        this.shadowColour = shadowColour;
    }

    public int getShadowAlpha() {
        return shadowAlpha;
    }

    public void setShadowAlpha(int shadowAlpha) {
        this.shadowAlpha = shadowAlpha;
    }

    public int getShadowGap() {
        return shadowGap;
    }

    public void setShadowGap(int shadowGap) {
        this.shadowGap = shadowGap;
    }

    public int getShadowOffset() {
        return shadowOffset;
    }

    public void setShadowOffset(int shadowOffset) {
        this.shadowOffset = shadowOffset;
    }

    public boolean isShady() {
        return shady;
    }

    public void setShady(boolean shady) {
        this.shady = shady;
    }

    public int getStrokeSize() {
        return strokeSize;
    }

    public void setStrokeSize(int strokeSize) {
        this.strokeSize = strokeSize;
    }

    public Dimension getArcs() {
        return arcs;
    }

    public void setArcs(Dimension arcs) {
        this.arcs = arcs;
    }
}
