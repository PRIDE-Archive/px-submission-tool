package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-08 10:56
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class models a Sample in mzTab metadata section
 */

public class Sample {
    private static final Logger logger = LoggerFactory.getLogger(Sample.class);

    public static class DataEntry {
        private Species species;
        private Tissue tissue;
        private CellType cellType;
        private Disease disease;
        private SampleCustomAttribute sampleCustomAttribute;

        public DataEntry() {
            species = null;
            tissue = null;
            cellType = null;
            disease = null;
            sampleCustomAttribute = null;
        }

        public Species getSpecies() {
            return species;
        }

        public void setSpecies(Species species) {
            this.species = species;
        }

        public Tissue getTissue() {
            return tissue;
        }

        public void setTissue(Tissue tissue) {
            this.tissue = tissue;
        }

        public CellType getCellType() {
            return cellType;
        }

        public void setCellType(CellType cellType) {
            this.cellType = cellType;
        }

        public Disease getDisease() {
            return disease;
        }

        public void setDisease(Disease disease) {
            this.disease = disease;
        }

        public SampleCustomAttribute getSampleCustomAttribute() {
            return sampleCustomAttribute;
        }

        public void setSampleCustomAttribute(SampleCustomAttribute sampleCustomAttribute) {
            this.sampleCustomAttribute = sampleCustomAttribute;
        }

        public boolean validate() {
            // No special requirements for a data entry to be valid, so we'll run a check on those attributes in the
            // data entry that have been set
            return getSpecies().validate()
                    && getTissue().validate()
                    && getCellType().validate()
                    && getDisease().validate()
                    && getSampleCustomAttribute().validate();
        }
    }

    // Bean
    private String description;
    private Map<Integer, DataEntry> dataEntries;

    public Sample() {
        description = "";
        dataEntries = new HashMap<>();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // Data Entries management
    public DataEntry updateDataEntry(DataEntry dataEntry, int index) {
        return dataEntries.put(index, dataEntry);
    }

    public DataEntry getDataEntry(int index) {
        return dataEntries.get(index);
    }

    public Set<Integer> getDataEntryIndexes() {
        return dataEntries.keySet();
    }

    public boolean validate() throws ValidationException {
        // No particular validation requirements for a sample, so we'll just run validation checks on all its data
        // entries
        for (int dataEntryIndex :
                getDataEntryIndexes()) {
            if (!getDataEntry(dataEntryIndex).validate()) {
                logger.error("Sample Data Entry with index '" + dataEntryIndex + "' FAILED VALIDATION CRITERIA");
                return false;
            }
        }
        // Everything validated up to this point
        return true;
    }
}
