package uk.ac.ebi.pride.gui.navigation;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.gui.GUIUtilities;

import javax.swing.*;
import java.awt.*;

/**
 * NavigationTitlePanel
 *
 * @author Rui Wang
 * @version $Id$
 */
public class NavigationTitlePanel extends JPanel {
    private ImageIcon pxLogo;

    // Factory method
    private static ImageIcon getLogo() {
        AppContext appContext = (AppContext) App.getInstance().getDesktopContext();
        ImageIcon logo = (ImageIcon)GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("px.logo"));
        if (appContext.isTrainingModeFlag()) {
            logo = (ImageIcon)GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("px.logo.trainingMode"));
        }
        return logo;
    }

    public NavigationTitlePanel() {
        pxLogo = getLogo();
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

    // Training mode visual handling, this could be done in a different way, more OOP friendly and elegant but,
    // it's just the minimum core needed to cover the requirement for training mode visual aids, it can be refactored
    // in the future
    public void toggleTrainingMode() {
        pxLogo = getLogo();
        setPreferredSize(new Dimension(pxLogo.getIconWidth(), pxLogo.getIconHeight() + 20));
        this.validate();
        this.repaint();
    }

}
