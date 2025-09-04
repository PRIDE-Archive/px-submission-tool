package uk.ac.ebi.pride.gui.form.table.listener;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.DataFile;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Listener to trigger a removal of a data file
 *
 * @author Rui Wang
 * @version $Id$
 */
public class RemoveDataFileListener extends MouseAdapter {
    private JTable table;
    private String colHeader;

    public RemoveDataFileListener(JTable table, String colHeader) {
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
                Object value = table.getValueAt(row, col);
                DataFile dataFile = (DataFile) value;
                
                // Don't allow removal of checksum.txt files
                if (dataFile.getFileName().equals("checksum.txt")) {
                    JOptionPane.showMessageDialog(table, 
                        "Cannot remove checksum.txt files. These files are required for submission integrity.",
                        "File Removal Not Allowed", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                ((AppContext) App.getInstance().getDesktopContext()).removeDatafile(dataFile);
            }
        }
    }
}
