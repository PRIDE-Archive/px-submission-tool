package uk.ac.ebi.pride.gui.form.table.model;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.validation.SubmissionValidator;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * {@code ResultFileTableModel} is the table model containing result files for annotating sample details
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ResultFileTableModel extends PxTableModel implements PropertyChangeListener {

    public enum TableHeader {
        FILE_NAME("File Name", "File name"),
        PATH("PATH / URL", "File path or URL"),
        TYPE("Type", "File type"),
        ANNOTATION_STATUS("Complete", "Whether annotation is complete"),
        ANNOTATION("Add annotation", "Add sample details");

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

    private final AppContext appContext;

    public ResultFileTableModel() {
        this.appContext = (AppContext) App.getInstance().getDesktopContext();
        this.appContext.addPropertyChangeListener(this);
    }

    @Override
    protected void initializeTableModel() {
        TableHeader[] headers = TableHeader.values();
        for (TableHeader header : headers) {
            columnNames.put(header.getHeader(), header.getToolTip());
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return TableHeader.ANNOTATION.getHeader().equals(getColumnName(columnIndex)) || super.isCellEditable(rowIndex, columnIndex);
    }

    @Override
    public int getRowCount() {
        return appContext.getSubmissionFilesByType(ProjectFileType.RESULT).size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        List<DataFile> dataFiles = appContext.getSubmissionFilesByType(ProjectFileType.RESULT);

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
            } else if (TableHeader.ANNOTATION_STATUS.getHeader().equals(getColumnName(columnIndex))) {
                return !SubmissionValidator.validateSampleMetaDataEntry(dataFile, true).hasError();
            } else if (TableHeader.ANNOTATION.getHeader().equals(getColumnName(columnIndex))) {
                return dataFile;
            }
        }

        return null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        if (AppContext.ADD_NEW_DATA_FILE.equals(propName) || AppContext.REMOVE_DATA_FILE.equals(propName) || AppContext.CHANGE_DATA_FILE_TYPE.equals(propName)
                || AppContext.ADD_NEW_SAMPLE_METADATA_ENTRY.equals(propName) || AppContext.REMOVE_SAMPLE_METADATA_ENTRY.equals(propName) || AppContext.NEW_SUBMISSION_FILE.equals(propName)
                || AppContext.SUBMISSION_TYPE_CHANGED.equals(propName)) {
            fireTableDataChanged();
        }
    }
}
