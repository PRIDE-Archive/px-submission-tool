package uk.ac.ebi.pride.gui.form.table.model;

import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.ProjectMetaData;

import java.beans.PropertyChangeEvent;
import java.util.Set;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ModificationMetaDataTableModel extends AbstractMetaDataTableModel {

    @Override
    public int getRowCount() {
        return appContext.getNumberOfModifications();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ProjectMetaData projectMetaData = appContext.getSubmissionRecord().getSubmission().getProjectMetaData();

        Set<CvParam> mods = projectMetaData.getModifications();

        return getValueFromCvParams(mods, rowIndex, columnIndex);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();

        if (AppContext.ADD_NEW_MODIFICATION.equals(propName) || AppContext.REMOVE_MODIFICATION.equals(propName) || AppContext.NEW_SUBMISSION_FILE.equals(propName)) {
            fireTableDataChanged();
        }
    }

    @Override
    public void removeValueAt(int rowIndex) {
        Object value = getValueAt(rowIndex, getColumnIndex(TableHeader.REMOVAL.getHeader()));
        appContext.removeModification((CvParam) value);
    }
}
