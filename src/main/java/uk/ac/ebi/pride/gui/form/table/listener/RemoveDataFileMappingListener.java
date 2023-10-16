package uk.ac.ebi.pride.gui.form.table.listener;

import uk.ac.ebi.pride.data.model.DataFile;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Mouse click listener for remove data file mapping
 *
 * @author Rui Wang
 * @version $Id$
 */
public class RemoveDataFileMappingListener extends MouseAdapter {
    private JTable table;
    private String colHeader;

    public RemoveDataFileMappingListener(JTable table, String colHeader) {
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
                DataFile mapping = (DataFile) table.getValueAt(row, col);
//                TargetFileMappingTableModel tableModel = (TargetFileMappingTableModel) table.getModel();
//                DataFile dataFile = tableModel.getDataFile();
//                ((AppContext) App.getInstance().getDesktopContext()).removeFileMapping(dataFile, mapping);
            }
        }
    }
}
