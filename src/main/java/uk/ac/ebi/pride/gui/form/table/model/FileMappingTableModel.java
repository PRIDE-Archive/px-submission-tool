package uk.ac.ebi.pride.gui.form.table.model;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.util.Constant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Table model for file mapping
 *
 * @author Rui Wang
 * @version $Id$
 */
public class FileMappingTableModel extends PxTableModel {
    public enum TableHeader {
        SELECTION("+", "Select a file as a related file"),
        FILE_NAME("File Name", "File name"),
        PATH("PATH / URL", "File path or URL"),
        TYPE("Type", "File type");

        private final String header;
        private final String toolTip;

        private TableHeader(String header, String tooltip) {
            this.header = header;
            this.toolTip = tooltip;
        }

        public String getHeader() {
            return header;
        }

        public String getToolTip() {
            return toolTip;
        }
    }

    private final Map<DataFile, Boolean> dataFiles;

    public FileMappingTableModel() {
        this.dataFiles = new LinkedHashMap<>();
    }

    @Override
    protected void initializeTableModel() {
        TableHeader[] headers = TableHeader.values();
        for (TableHeader header : headers) {
            columnNames.put(header.getHeader(), header.getToolTip());
        }
    }

    /**
     * Add a data file
     *
     * @param newData  new data file
     * @param selected whether the file is already selected
     */
    public void addData(DataFile newData, boolean selected) {
        if (!dataFiles.keySet().contains(newData)) {
            // row count
            int rowCnt = this.getRowCount();

            // add a new row
            dataFiles.put(newData, selected);
            fireTableRowsInserted(rowCnt, rowCnt);
        }
    }

    /**
     * Get all the data files which are currently in the table
     *
     * @return Map<DataFile, Boolean>  a list of data files
     */
    public Map<DataFile, Boolean> getData() {
        return new LinkedHashMap<>(dataFiles);
    }

    @Override
    public int getRowCount() {
        return dataFiles.size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return TableHeader.SELECTION.getHeader().equals(getColumnName(columnIndex)) || super.isCellEditable(rowIndex, columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (!dataFiles.isEmpty() && rowIndex >= 0 && columnIndex >= 0) {
            List<Map.Entry<DataFile, Boolean>> entries = new ArrayList<>(dataFiles.entrySet());

            Map.Entry<DataFile, Boolean> entry = entries.get(rowIndex);
            DataFile dataFile = entry.getKey();
            Boolean selected = entry.getValue();

            if (TableHeader.SELECTION.getHeader().equals(getColumnName(columnIndex))) {
                return selected;
            } else if (TableHeader.FILE_NAME.getHeader().equals(getColumnName(columnIndex))) {
                if (dataFile.isFile()) {
                    return dataFile.getFile().getName();
                } else {
                    return Constant.URL;
                }
            } else if (TableHeader.PATH.getHeader().equals(getColumnName(columnIndex))) {
                if (dataFile.isFile()) {
                    return dataFile.getFile().getAbsolutePath();
                } else {
                    return dataFile.getUrl().toString();
                }
            } else if (TableHeader.TYPE.getHeader().equals(getColumnName(columnIndex))) {
                return dataFile.getFileType();
            }
        }

        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        List<Map.Entry<DataFile, Boolean>> entries = new ArrayList<>(dataFiles.entrySet());

        Map.Entry<DataFile, Boolean> entry = entries.get(rowIndex);
        DataFile dataFile = entry.getKey();

        if (TableHeader.SELECTION.getHeader().equals(getColumnName(columnIndex))) {
            dataFiles.put(dataFile, (Boolean) aValue);
        } else {
            super.setValueAt(aValue, rowIndex, columnIndex);
        }
    }
}
