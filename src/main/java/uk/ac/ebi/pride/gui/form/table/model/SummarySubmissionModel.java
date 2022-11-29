package uk.ac.ebi.pride.gui.form.table.model;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.ResubmissionFileChangeState;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Table model for showing all the result files and enable adding file mappings
 *
 * @author Suresh Hewapathirana
 * @version $Id$
 */
public class SummarySubmissionModel extends PxTableModel implements PropertyChangeListener {

    public enum TableHeader {
        FILE_ID("File ID", "File ID"),
        FILE_NAME("File Name", "File name"),
        TYPE("Type", "File type"),
        SIZE("File Size(bytes)", "File size");

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

    public SummarySubmissionModel() {
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
    public int getRowCount() {
            return appContext.getSubmissionRecord().getSubmission().getDataFiles().size();
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        List<DataFile> dataFiles = appContext.getSubmissionRecord().getSubmission().getDataFiles();

        if (!dataFiles.isEmpty() && rowIndex >= 0 && columnIndex >= 0 && rowIndex < dataFiles.size() ) {
            DataFile dataFile = dataFiles.get(rowIndex);

            if (SummarySubmissionModel.TableHeader.FILE_ID.getHeader().equals(getColumnName(columnIndex))) {
                return dataFile.getFileId();
            } else if (TableHeader.FILE_NAME.getHeader().equals(getColumnName(columnIndex))) {
                return dataFile.getFile().getName();
            } else if (TableHeader.TYPE.getHeader().equals(getColumnName(columnIndex))) {
                return dataFile.getFileType();
            } else if (SummarySubmissionModel.TableHeader.SIZE.getHeader().equals(getColumnName(columnIndex))) {
                return dataFile.getFileSize();
            }
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        List<DataFile> dataFiles = appContext.getSubmissionRecord().getSubmission().getDataFiles();

//        if (SummarySubmissionModel.TableHeader.ACTION.getHeader().equals(getColumnName(columnIndex))) {
//            DataFile dataFile = dataFiles.get(rowIndex);
//            appContext.setResubmissionAction(dataFile, (ResubmissionFileChangeState) aValue);
//        } else {
            super.setValueAt(aValue, rowIndex, columnIndex);
//        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        if (AppContext.ADD_NEW_DATA_FILE.equals(propName)) {
            fireTableDataChanged();
        }
    }
}
