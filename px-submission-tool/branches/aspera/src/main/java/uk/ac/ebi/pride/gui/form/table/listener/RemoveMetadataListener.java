package uk.ac.ebi.pride.gui.form.table.listener;

import uk.ac.ebi.pride.gui.form.table.model.AbstractMetaDataTableModel;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Listener for removing modification
 *
 * @author Rui Wang
 * @version $Id$
 */
public class RemoveMetadataListener extends MouseAdapter {
    private JTable table;
    private String colHeader;

    public RemoveMetadataListener(JTable table, String colHeader) {
        this.table = table;
        this.colHeader = colHeader;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int col = table.columnAtPoint(e.getPoint());
        String header = table.getColumnName(col);
        if (header.equals(colHeader)) {
            int row = table.rowAtPoint(e.getPoint());
            if (row >= 0 && row < table.getRowCount()) {
                AbstractMetaDataTableModel tableModel = (AbstractMetaDataTableModel) table.getModel();
                tableModel.removeValueAt(row);
            }
        }
    }
}

