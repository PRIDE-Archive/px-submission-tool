package uk.ac.ebi.pride.gui.data.mztab;

import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

    public class DataEntry {
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
    }

    // Bean
    private String description;
    private List<DataEntry> dataEntries;

    public Sample() {
        description = "";
        dataEntries = new ArrayList<>();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {

        return description;
    }

    public List<DataEntry> getDataEntries() {
        return dataEntries;
    }
}
