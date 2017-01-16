package uk.ac.ebi.pride.gui.form.table.listener;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.utilities.util.Tuple;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.SampleMetaData;
import uk.ac.ebi.pride.gui.form.table.model.SampleMetaDataTableModel;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class RemoveSampleMetaDataListener extends MouseAdapter {
    private JTable table;
    private String colHeader;

    public RemoveSampleMetaDataListener(JTable table, String colHeader) {
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
                SampleMetaDataTableModel tableModel = (SampleMetaDataTableModel) table.getModel();
                Tuple<SampleMetaData.Type, CvParam> tableEntry = (Tuple<SampleMetaData.Type, CvParam>) tableModel.getValueAt(row, col);
                ((AppContext) App.getInstance().getDesktopContext()).removeSampleMetadataEntry(tableModel.getDataFile(), tableEntry.getKey(), tableEntry.getValue());
            }
        }
    }
}
