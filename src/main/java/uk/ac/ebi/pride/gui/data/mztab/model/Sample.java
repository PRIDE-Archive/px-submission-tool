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
            if ((getSpecies() != null) && (!getSpecies().validate())) {
                logger.error("INVALID Species information for sample data entry");
                return false;
            }
            if ((getTissue() != null) && (!getTissue().validate())) {
                logger.error("INVALID Tissue information for sample data entry");
                return false;
            }
            if ((getCellType() != null) && (!getCellType().validate())) {
                logger.error("INVALID Cell Type information for sample data entry");
                return false;
            }
            if ((getDisease() != null) && (!getDisease().validate())) {
                logger.error("INVALID Disease information for sample data entry");
                return false;
            }
            if ((getSampleCustomAttribute() != null) && (!getSampleCustomAttribute().validate())) {
                logger.error("INVALID Sample custom attribute for sample data entry");
                return false;
            }
            // It is valid!
            return true;
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
