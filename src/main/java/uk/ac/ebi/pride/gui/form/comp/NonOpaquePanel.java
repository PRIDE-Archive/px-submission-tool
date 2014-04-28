package uk.ac.ebi.pride.gui.form.comp;

import uk.ac.ebi.pride.gui.util.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * A none opaque panel
 *
 * @author Rui Wang
 * @version $Id$
 */
public class NonOpaquePanel extends Wrapper {
    public NonOpaquePanel() {
        setOpaque(false);
    }

    public NonOpaquePanel(JComponent wrapped) {
        super(wrapped);
        setOpaque(false);
    }

    public NonOpaquePanel(LayoutManager layout, JComponent wrapped) {
        super(layout, wrapped);
        setOpaque(false);
    }

    public NonOpaquePanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        setOpaque(false);
    }

    public NonOpaquePanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public NonOpaquePanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
        setOpaque(false);
    }

    @Override
    public void setOpaque(boolean isOpaque) {
        super.setOpaque(isOpaque);

        boolean isNimBus = UIUtil.isUnderNimbusLookAndFeel();

        if (!isOpaque && isNimBus) {
            if (isNimBus) {
                setBackground(UIUtil.TRANSPARENT_COLOR);
            }
        }
    }
}
