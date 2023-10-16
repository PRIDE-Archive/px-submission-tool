package uk.ac.ebi.pride.gui.form.table.model;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.gui.data.ExtendedCvParam;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Rui Wang
 * @version $Id$
 */
public abstract class AbstractMetaDataTableModel extends PxTableModel implements PropertyChangeListener {

    public enum TableHeader {
        NAME("Name", "Name of the controlled vocabulary"),
        ONTOLOGY("Ontology", "Ontology provides controlled vocabularies"),
        ACCESSION("Accession", "Ontology accession"),
        REMOVAL("Remove", "Remove the controlled vocabulary");

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

    protected AppContext appContext;

    public AbstractMetaDataTableModel() {
        this.appContext = (AppContext) App.getInstance().getDesktopContext();
        this.appContext.addPropertyChangeListener(this);
    }


    @Override
    public void initializeTableModel() {
        TableHeader[] headers = TableHeader.values();
        for (TableHeader header : headers) {
            columnNames.put(header.getHeader(), header.getToolTip());
        }
    }

    public abstract void removeValueAt(int rowIndex);

    protected Object getValueFromCvParams(Collection<CvParam> cvParams, int rowIndex, int columnIndex) {
        if (rowIndex >= 0 && columnIndex >= 0 && cvParams != null && !cvParams.isEmpty()) {
            CvParam s = new ArrayList<>(cvParams).get(rowIndex);
            return getColumnValueAt(s, columnIndex);
        }

        return null;
    }

    private Object getColumnValueAt(CvParam cvParam, int columnIndex) {
        if (TableHeader.NAME.getHeader().equals(getColumnName(columnIndex))) {
            return cvParam instanceof ExtendedCvParam ? ((ExtendedCvParam) cvParam).getHumanReadableName() : cvParam.getName();
        } else if (TableHeader.ONTOLOGY.getHeader().equals(getColumnName(columnIndex))) {
            return cvParam.getCvLabel();
        } else if (TableHeader.ACCESSION.getHeader().equals(getColumnName(columnIndex))) {
            return cvParam.getAccession();
        } else if (TableHeader.REMOVAL.getHeader().equals(getColumnName(columnIndex))) {
            return cvParam;
        } else {
            return null;
        }
    }
}
