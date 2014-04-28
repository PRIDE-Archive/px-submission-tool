package uk.ac.ebi.pride.gui.navigation;

import org.jdesktop.swingx.border.DropShadowBorder;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.GUIUtilities;

import javax.swing.*;
import java.awt.*;

/**
 * NavigationTitlePanel
 *
 * @author Rui Wang
 * @version $Id$
 */
public class NavigationTitlePanel extends JPanel{
    private ImageIcon pxLogo;

    public NavigationTitlePanel() {
        pxLogo = (ImageIcon)GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("px.logo"));
        setPreferredSize(new Dimension(pxLogo.getIconWidth(), pxLogo.getIconHeight() + 20));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // create graphics g
        Graphics2D g2 = (Graphics2D)g.create();

        // rendering hints
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // paint background
        g2.setPaint(new GradientPaint(0, 0, Color.darkGray, 0, getHeight(), Color.black));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // draw px logo

        int xPos = getWidth() - pxLogo.getIconWidth() - 10;
        int yPos = 10;

        g2.drawImage(pxLogo.getImage(), xPos, yPos, null);
        g2.dispose();
    }
}
