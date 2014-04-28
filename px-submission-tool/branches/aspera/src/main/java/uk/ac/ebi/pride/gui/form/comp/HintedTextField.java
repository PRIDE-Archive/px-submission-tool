package uk.ac.ebi.pride.gui.form.comp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Text field with grey-out hint
 *
 * @author Rui Wang
 * @version $Id$
 */
public class HintedTextField extends JTextField {
    private String hint;

    public HintedTextField(String hint) {
        this.hint = hint;
        this.setText(this.hint);
        this.addFocusListener(new HintFocusListener());
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
                HintedTextField.this.clearText();
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (getText() == null) {
                HintedTextField.this.setText(hint);
            }
        }
    }
}
