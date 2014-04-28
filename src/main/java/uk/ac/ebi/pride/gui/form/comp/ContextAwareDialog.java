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
public class ContextAwareDialog extends JDialog {

    protected App app = (App) App.getInstance();
    protected AppContext appContext = (AppContext) App.getInstance().getDesktopContext();
    protected BalloonTip warningBalloonTip;

    public ContextAwareDialog() {
    }

    public ContextAwareDialog(Frame owner) {
        super(owner);
    }

    public ContextAwareDialog(Frame owner, boolean modal) {
        super(owner, modal);
    }

    public ContextAwareDialog(Frame owner, String title) {
        super(owner, title);
    }

    public ContextAwareDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
    }

    public ContextAwareDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
    }

    public ContextAwareDialog(Dialog owner) {
        super(owner);
    }

    public ContextAwareDialog(Dialog owner, boolean modal) {
        super(owner, modal);
    }

    public ContextAwareDialog(Dialog owner, String title) {
        super(owner, title);
    }

    public ContextAwareDialog(Dialog owner, String title, boolean modal) {
        super(owner, title, modal);
    }

    public ContextAwareDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
    }

    public ContextAwareDialog(Window owner) {
        super(owner);
    }

    public ContextAwareDialog(Window owner, ModalityType modalityType) {
        super(owner, modalityType);
    }

    public ContextAwareDialog(Window owner, String title) {
        super(owner, title);
    }

    public ContextAwareDialog(Window owner, String title, ModalityType modalityType) {
        super(owner, title, modalityType);
    }

    public ContextAwareDialog(Window owner, String title, ModalityType modalityType, GraphicsConfiguration gc) {
        super(owner, title, modalityType, gc);
    }
}
