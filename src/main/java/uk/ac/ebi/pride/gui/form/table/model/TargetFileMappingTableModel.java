package uk.ac.ebi.pride.gui.form.table.model;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.util.Constant;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Table model for showing all the file mappings of a particular result file
 *
 * @author Rui Wang
 * @version $Id$
 */
public class TargetFileMappingTableModel extends PxTableModel implements PropertyChangeListener {
    public enum TableHeader {
        FILE_NAME("File Name", "File name"),
        PATH("PATH / URL", "File path or URL"),
        TYPE("Type", "File type"),
        REMOVAL("Remove", "Delete file or URL");

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

    private DataFile dataFile;

    public TargetFileMappingTableModel() {
        this.dataFile = null;
        App.getInstance().getDesktopContext().addPropertyChangeListener(this);
    }

    @Override
    protected void initializeTableModel() {
        TableHeader[] headers = TableHeader.values();
        for (TableHeader header : headers) {
            columnNames.put(header.getHeader(), header.getToolTip());
        }
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return dataFile == null ? 0 : dataFile.getFileMappings().size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        if (dataFile != null) {
            List<DataFile> dataFiles = dataFile.getFileMappings();

            if (!dataFiles.isEmpty() && rowIndex >= 0 && columnIndex >= 0) {
                DataFile dataFile = dataFiles.get(rowIndex);

                if (TableHeader.FILE_NAME.getHeader().equals(getColumnName(columnIndex))) {
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
                } else if (TableHeader.REMOVAL.getHeader().equals(getColumnName(columnIndex))) {
                    return dataFile;
                }
            }
        }

        return null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();

        if (AppContext.REMOVE_DATA_FILE.equals(propName)) {
            if (evt.getOldValue().equals(dataFile)) {
                setDataFile(null);
            }
        } else if (AppContext.ADD_NEW_DATA_FILE_MAPPING.equals(propName) || AppContext.REMOVE_DATA_FILE_MAPPING.equals(propName)) {
            fireTableDataChanged();
        }
    }
}
