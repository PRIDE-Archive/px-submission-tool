package uk.ac.ebi.pride.gui.form.comp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Text area with grey-out hint area
 *
 * @author Rui Wang
 * @version $Id$
 */
public class HintedTextArea extends JTextArea {
    private String hint;

    public HintedTextArea(String hint) {
        this.hint = hint;
        this.setText(this.hint);
        this.addFocusListener(new HintFocusListener());
        this.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        this.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
    }

    @Override
    public String getText() {
        String currentText = super.getText().trim();
        return (currentText.equals(hint) || "".equals(currentText)) ? null : currentText;
    }

    @Override
    public void setText(String text) {
        if (text != null && !"".equals(text.trim())) {
            this.setForeground(hint.equals(text) ? Color.gray : Color.black);
            super.setText(text);
        }
    }

    private void clearText() {
        this.setForeground(Color.black);
        super.setText("");
    }

    private class HintFocusListener extends FocusAdapter {
        @Override
        public void focusGained(FocusEvent e) {
            if (getText() == null) {
                HintedTextArea.this.clearText();
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (getText() == null) {
                HintedTextArea.this.setText(hint);
            }
        }
    }
}
