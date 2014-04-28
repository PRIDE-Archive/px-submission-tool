package uk.ac.ebi.pride.gui.form.table.model;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.Tuple;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.SampleMetaData;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class SampleMetaDataTableModel extends PxTableModel implements PropertyChangeListener {

    public enum TableHeader {
        METADATA_TYPE("Type", "Annotation type"),
        METADATA_VALUE("Value", "Annotation value"),
        ONTOLOGY("Ontology", "Ontology name"),
        ONTOLOGY_ACCESSION("Accession", "Ontology accession"),
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

    public SampleMetaDataTableModel() {
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
        return dataFile == null ? 0 : getTableEntries().size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        if (dataFile != null) {
            List<Tuple<SampleMetaData.Type, CvParam>> tableEntries = getTableEntries();

            if (!tableEntries.isEmpty() && rowIndex >= 0 && columnIndex >= 0) {
                Tuple<SampleMetaData.Type, CvParam> tableEntry = tableEntries.get(rowIndex);
                SampleMetaData.Type type = tableEntry.getKey();
                CvParam param = tableEntry.getValue();

                if (TableHeader.METADATA_TYPE.getHeader().equals(getColumnName(columnIndex))) {
                    return tableEntry.getKey().getName();
                } else if (TableHeader.METADATA_VALUE.getHeader().equals(getColumnName(columnIndex))) {
                    return type.equals(SampleMetaData.Type.EXPERIMENTAL_FACTOR) ? param.getValue() : param.getName();
                } else if (TableHeader.ONTOLOGY.getHeader().equals(getColumnName(columnIndex))) {
                    return param.getCvLabel();
                } else if (TableHeader.ONTOLOGY_ACCESSION.getHeader().equals(getColumnName(columnIndex))) {
                    return param.getAccession();
                } else if (TableHeader.REMOVAL.getHeader().equals(getColumnName(columnIndex))) {
                    return tableEntry;
                }
            }
        }
        return null;
    }

    /**
     * Format data for the table consumption
     */
    private List<Tuple<SampleMetaData.Type, CvParam>> getTableEntries() {
        List<Tuple<SampleMetaData.Type, CvParam>> tableEntries = new ArrayList<Tuple<SampleMetaData.Type, CvParam>>();

        SampleMetaData sampleMetaData = dataFile.getSampleMetaData();
        Map<SampleMetaData.Type, Set<CvParam>> metaDataEntries = sampleMetaData.getMetaData();

        for (Map.Entry<SampleMetaData.Type, Set<CvParam>> typeSetEntry : metaDataEntries.entrySet()) {
            SampleMetaData.Type type = typeSetEntry.getKey();
            if (type.equals(SampleMetaData.Type.EXPERIMENTAL_FACTOR)) {
                // this is a special case for experimental factor. In the submission tool, we are making
                // the experimental factor non-mandatory, so when it is the default value, we should not
                // display this in the table
                for (CvParam param : typeSetEntry.getValue()) {
                    if (!param.getValue().equalsIgnoreCase(App.getInstance().getDesktopContext().getProperty("experimental.factor.default.value"))) {
                        tableEntries.add(new Tuple<SampleMetaData.Type, CvParam>(type, param));
                    }
                }

            } else {
                for (CvParam param : typeSetEntry.getValue()) {
                    tableEntries.add(new Tuple<SampleMetaData.Type, CvParam>(type, param));
                }
            }
        }

        return tableEntries;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();

        if (AppContext.REMOVE_DATA_FILE.equals(propName)) {
            if (evt.getOldValue().equals(dataFile)) {
                setDataFile(null);
            }
        } else if (AppContext.ADD_NEW_SAMPLE_METADATA_ENTRY.equals(propName) || AppContext.REMOVE_SAMPLE_METADATA_ENTRY.equals(propName)) {
            fireTableDataChanged();
        }
    }
}
