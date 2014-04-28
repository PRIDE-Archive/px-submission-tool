package uk.ac.ebi.pride.gui.form.comp;

import net.java.balloontip.BalloonTip;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.gui.navigation.NavigationPanelDescriptor;

import java.awt.*;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ContextAwareNavigationPanelDescriptor extends NavigationPanelDescriptor {
    protected final App app = (App) App.getInstance();
    protected final AppContext appContext = (AppContext) App.getInstance().getDesktopContext();
    protected BalloonTip warningBalloonTip;

    public ContextAwareNavigationPanelDescriptor(Object navigationId, String title, String description, Component navigationPanel) {
        super(navigationId, title, description, navigationPanel);
    }
}
