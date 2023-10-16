package uk.ac.ebi.pride.gui.form.table.editor;

//import uk.ac.ebi.pride.gui.form.dialog.FileMappingDialog;

import javax.swing.*;
import java.awt.*;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ButtonCellEditor extends DefaultCellEditor {

    protected JButton button;
    protected boolean isPushed;


    public ButtonCellEditor(String text, Icon icon) {
        super(new JCheckBox());
        this.button = new JButton(text, icon);
        button.setOpaque(true);
        button.addActionListener(e -> fireEditingStopped());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        isPushed = true;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        isPushed = false;
        return super.getCellEditorValue();
    }

    @Override
    public boolean stopCellEditing() {
        isPushed = false;
        return super.stopCellEditing();
    }
}
