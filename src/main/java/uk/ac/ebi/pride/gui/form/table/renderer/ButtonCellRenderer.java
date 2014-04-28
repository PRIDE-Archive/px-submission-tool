package uk.ac.ebi.pride.gui.form.table.renderer;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Table cell renderer to draw buttons
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ButtonCellRenderer extends JButton implements TableCellRenderer {

    public ButtonCellRenderer(String text, Icon icon) {
        super(text, icon);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }
}
