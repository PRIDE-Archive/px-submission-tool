package uk.ac.ebi.pride.gui.form.table.renderer;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Cell renderer for drawing an combo box
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ComboBoxCellRenderer extends JComboBox implements TableCellRenderer{
    public ComboBoxCellRenderer(Object items[]) {
        super(items);
        this.setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setSelectedItem(value);
        return this;
    }
}
