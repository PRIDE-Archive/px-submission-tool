package uk.ac.ebi.pride.gui.form.table.model;

import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * Default table model for all px submission tool tables
 *
 * @author Rui Wang
 * @version $Id$
 */
public abstract class PxTableModel extends AbstractTableModel {
    protected final Map<String, String> columnNames;

    public PxTableModel() {
        columnNames = new LinkedHashMap<>();
        initializeTableModel();
    }

    /**
     * This method should be implemented to columns
     * and basic rows of the table
     */
    protected abstract void initializeTableModel();

    /**
     * Add an extra column to table model
     *
     * @param columnName column string name
     * @param toolTip    column tool tips
     */
    public void addColumn(String columnName, String toolTip) {
        columnNames.put(columnName, toolTip);
        fireTableStructureChanged();
    }

    /**
     * Remove all the columns
     */
    public void removeAllColumns() {
        columnNames.clear();
    }

    /**
     * Get the number of columns
     *
     * @return int number of columns
     */
    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    /**
     * Get the index of the column
     *
     * @param header column title
     * @return int  index of the column in int
     */
    public int getColumnIndex(String header) {
        int index = -1;

        List<Map.Entry<String, String>> entries = new LinkedList<>(columnNames.entrySet());

        for (Map.Entry<String, String> entry : entries) {
            if (entry.getKey().equals(header)) {
                index = entries.indexOf(entry);
            }
        }

        return index;
    }

    /**
     * Get column name
     *
     * @param index column index
     * @return String  column name
     */
    public String getColumnName(int index) {
        String columnName = null;

        List<Map.Entry<String, String>> entries = new LinkedList<>(columnNames.entrySet());
        Map.Entry<String, String> entry = entries.get(index);
        if (entry != null) {
            columnName = entry.getKey();
        }

        return columnName;
    }

    /**
     * Get column tooltip
     *
     * @param index column index
     * @return String  column tooltip
     */
    public String getColumnTooltip(int index) {
        String tooltip = null;

        List<Map.Entry<String, String>> entries = new LinkedList<>(columnNames.entrySet());
        Map.Entry<String, String> entry = entries.get(index);
        if (entry != null) {
            tooltip = entry.getValue();
        }

        return tooltip;
    }

}
