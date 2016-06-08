package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-08 09:28
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class MetaData {
    private static final Logger logger = LoggerFactory.getLogger(MetaData.class);

    // mzTab modes
    public enum MzTabMode {
        QUANTIFICATION("Quantification"),
        IDENTIFICATION("Identification");

        private String mode;

        MzTabMode(String mode) {
            this.mode = mode;
        }


        @Override
        public String toString() {
            return mode;
        }
    }

    // mzTab types
    public enum MzTabType {
        COMPLETE("Complete"),
        SUMMARY("Summary");

        private String type;

        MzTabType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
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
    private List<MsRun> msRuns;
    // samples
    private List<Sample> samples;

    public MetaData() {
        version = null;
        mode = null;
        type = null;
        fileId = null;
        title = null;
        description = null;
        msRuns = new ArrayList<>();
        samples = new ArrayList<>();
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

    // TODO - add ms-run entry
}
