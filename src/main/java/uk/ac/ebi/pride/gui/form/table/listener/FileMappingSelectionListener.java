package uk.ac.ebi.pride.gui.form.table.listener;

import uk.ac.ebi.pride.gui.form.table.model.FileMappingTableModel;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class FileMappingSelectionListener extends MouseAdapter {
    private JTable table;

    public FileMappingSelectionListener(JTable table) {
        this.table = table;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        int col = table.columnAtPoint(e.getPoint());

        if (col != 0) {
            Boolean selected = (Boolean) table.getValueAt(row, 0);
            FileMappingTableModel tableModel = (FileMappingTableModel) table.getModel();
            tableModel.setValueAt(!selected, row, 0);
            table.getSelectionModel().clearSelection();
        }
    }
}

