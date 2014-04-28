package uk.ac.ebi.pride.gui.form.table.model;

import uk.ac.ebi.pride.data.model.CvParam;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class MetaDataTableModel extends AbstractMetaDataTableModel {
    private final Set<CvParam> metaDataValues;

    public MetaDataTableModel() {
        this.metaDataValues = new LinkedHashSet<CvParam>();
    }

    @Override
    public int getRowCount() {
        return metaDataValues.size();
    }

    public Set<CvParam> getValues() {
        return new LinkedHashSet<CvParam>(metaDataValues);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return getValueFromCvParams(metaDataValues, rowIndex, columnIndex);
    }

    public void addValues(Collection<CvParam> values) {
        metaDataValues.addAll(values);
        fireTableDataChanged();
    }

    public void addValue(CvParam value) {
        metaDataValues.add(value);
        fireTableDataChanged();
    }

    public void clearValues() {
        metaDataValues.clear();
        fireTableDataChanged();
    }

    @Override
    public void removeValueAt(int rowIndex) {
        CvParam value = (CvParam)getValueAt(rowIndex, getColumnIndex(TableHeader.REMOVAL.getHeader()));
        metaDataValues.remove(value);
        fireTableDataChanged();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // do nothing
    }
}
