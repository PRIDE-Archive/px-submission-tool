package uk.ac.ebi.pride.gui.form.comp;

import net.java.balloontip.BalloonTip;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;

import java.awt.*;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ContextAwareHeaderPanel extends HeaderPanel {

    protected App app = (App) App.getInstance();
    protected AppContext appContext = (AppContext) App.getInstance().getDesktopContext();
    protected BalloonTip warningBalloonTip;

    public ContextAwareHeaderPanel() {
    }

    public ContextAwareHeaderPanel(LayoutManager layout) {
        super(layout);
    }

    public ContextAwareHeaderPanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    public ContextAwareHeaderPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

}
