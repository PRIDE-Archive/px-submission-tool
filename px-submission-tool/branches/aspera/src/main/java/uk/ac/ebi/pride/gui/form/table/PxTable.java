package uk.ac.ebi.pride.gui.form.table;

import org.jdesktop.swingx.table.DefaultTableColumnModelExt;
import uk.ac.ebi.pride.gui.form.table.model.PxTableModel;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.MouseEvent;

/**
 * Default table implementation for all px submission tool tables
 *
 * @author Rui Wang
 * @version $Id$
 */
public class PxTable extends AlterRowColorTable {
    public PxTable(TableModel dm) {
        this(dm, new DefaultTableColumnModelExt());
    }

    @SuppressWarnings("unchecked")
    public PxTable(TableModel tableModel, TableColumnModel tableColumnModel) {
        super();

        if (tableColumnModel != null) {
            this.setColumnModel(tableColumnModel);
        }

        if (tableModel != null) {
            this.setModel(tableModel);
        }

        // auto create row sorter
        setAutoCreateRowSorter(true);

        // set column control visible
        setColumnControlVisible(false);

        // auto fill
        setFillsViewportHeight(true);

        // row height
        setRowHeight(25);

        // prevent dragging of column
        getTableHeader().setReorderingAllowed(false);

        // remove border
        setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = columnModel.getColumn(index).getModelIndex();
                PxTableModel tableModel = (PxTableModel) PxTable.this.getModel();
                return tableModel.getColumnTooltip(realIndex);
            }
        };
    }
}
