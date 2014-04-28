package uk.ac.ebi.pride.gui.form.table.editor;

import javax.swing.*;

/**
 * Cell editor to allow select values using combo box
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ComboBoxCellEditor extends DefaultCellEditor{

    public ComboBoxCellEditor(Object[] items) {
        super(new JComboBox(items));
    }
}
