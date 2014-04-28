package uk.ac.ebi.pride.gui.form.comp;

import javax.swing.*;
import java.awt.*;

/**
 * Wrap a panel on a component
 *
 * @author Rui Wang
 * @version $Id$
 */
public class Wrapper extends JPanel {

    public Wrapper() {
        setLayout(new BorderLayout());
        setOpaque(false);
    }

    public Wrapper(JComponent wrapped) {
        setLayout(new BorderLayout());
        add(wrapped, BorderLayout.CENTER);
        setOpaque(false);
    }

    public Wrapper(LayoutManager layout, JComponent wrapped) {
        super(layout);
        add(wrapped);
        setOpaque(false);
    }

    public Wrapper(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        setOpaque(false);
    }

    public Wrapper(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public Wrapper(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
        setOpaque(false);
    }

    public void setContent(JComponent wrapped) {
        if (wrapped == getTargetComponent()) {
            return;
        }

        removeAll();
        setLayout(new BorderLayout());
        if (wrapped != null) {
            add(wrapped, BorderLayout.CENTER);
        }
        validate();
    }

    public void requestFocus() {
        if (getTargetComponent() == this) {
            super.requestFocus();
            return;
        }
        getTargetComponent().requestFocus();
    }

    public boolean requestFocusInWindow() {
        if (getTargetComponent() == this) {
            return super.requestFocusInWindow();
        }
        return getTargetComponent().requestFocusInWindow();
    }

    public void requestFocusInternal() {
        super.requestFocus();
    }

    public final boolean requestFocus(boolean temporary) {
        if (getTargetComponent() == this) {
            return super.requestFocus(temporary);
        }
        return getTargetComponent().requestFocus(temporary);
    }

    public JComponent getTargetComponent() {
        if (getComponentCount() == 1) {
            return (JComponent) getComponent(0);
        } else {
            return this;
        }
    }
}
