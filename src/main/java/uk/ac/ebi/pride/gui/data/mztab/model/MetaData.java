package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-08 09:28
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class MetaData implements MzTabSection {
    private static final Logger logger = LoggerFactory.getLogger(MetaData.class);

    // mzTab modes
    public enum MzTabType {
        QUANTIFICATION("Quantification"),
        IDENTIFICATION("Identification");

        private String type;

        MzTabType(String type) {
            this.type = type;
        }

        public String getValue() {
            return toString();
        }

        @Override
        public String toString() {
            return type;
        }
    }

    // mzTab types
    public enum MzTabMode {
        COMPLETE("Complete"),
        SUMMARY("Summary");

        private String mode;

        MzTabMode(String mode) {
            this.mode = mode;
        }

        public String getValue() {
            return toString();
        }

        @Override
        public String toString() {
            return mode;
        }
    }

    // Bean fields
    // mzTab-version
    private String version;
    // mzTab-mode
    private MzTabMode mode;
    // mzTab-type
    private MzTabType type;
    // mzTab-ID
    private String fileId;
    // title
    private String title;
    // description
    private String description;
    // ms-run entries
    private Map<Integer, MsRun> msRuns;
    // samples
    private Map<Integer, Sample> samples;
    // TODO - Capture the following extra attributes
    // TODO indexed protein_search_engine_score
    // TODO indexed peptide_search_engine_score
    // TODO indexed psm_search_engine_score
    // TODO indexed smallmolecule_search_engine_score
    // NOTE - it says on the documentation that they must be reported for every search engine score reported in the
    // corresponding section
    // TODO - --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    public MetaData() {
        version = null;
        mode = null;
        type = null;
        fileId = null;
        title = null;
        description = null;
        msRuns = new HashMap<>();
        samples = new HashMap<>();
    }

    // Getters/Setters
    public String getVersion() {
        return version;
    }

    public MzTabMode getMode() {
        return mode;
    }

    public MzTabType getType() {
        return type;
    }

    public String getFileId() {
        return fileId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setVersion(String version) {

        this.version = version;
    }

    public void setMode(MzTabMode mode) {
        this.mode = mode;
    }

    public void setType(MzTabType type) {
        this.type = type;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // MsRun Entries management
    public MsRun updateMsRun(MsRun msRun, int index) {
        logger.debug("Adding ms-run with index " + index);
        return msRuns.put(index, msRun);
    }

    public MsRun getMsRunEntry(int index) {
        return msRuns.get(index);
    }

    public Set<Integer> getAvailableMsRunIndexes() {
        return msRuns.keySet();
    }

    // Sample Entries management
    public void updateSampleData(Sample sample, int index) {
        logger.debug("Adding sample data entry, index " + index);
        samples.put(index, sample);
    }

    public Sample getSampleData(int index) {
        return samples.get(index);
    }

    public Set<Integer> getAvailableSampleIndexes() {
        return samples.keySet();
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, MzTabSectionValidator validator) throws InvalidMzTabSectionException {
        try {
            return validator.validate(mzTabDocument, this);
        } catch (MzTabSectionValidatorException e) {
            throw new InvalidMzTabSectionException("An ERROR occurred while validating MetaData mzTab section: " + e.getMessage());
        }
    }
}
