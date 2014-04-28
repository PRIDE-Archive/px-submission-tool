package uk.ac.ebi.pride.gui.form.comp;

import net.java.balloontip.BalloonTip;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;

import javax.swing.*;
import java.awt.*;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ContextAwarePanel extends JPanel {

    protected App app = (App) App.getInstance();
    protected AppContext appContext = (AppContext) App.getInstance().getDesktopContext();
    protected BalloonTip warningBalloonTip;

    public ContextAwarePanel() {
    }

    public ContextAwarePanel(LayoutManager layout) {
        super(layout);
    }

    public ContextAwarePanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    public ContextAwarePanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

}
