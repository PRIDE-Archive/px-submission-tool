package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-07-06 14:32
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class StudyVariable {
    private static final Logger logger = LoggerFactory.getLogger(StudyVariable.class);

    // Bean
    private String description = null;
    // We reference assays and samples via their indexes because, at the time they are set by the parser, we may have
    // not seen their referees, at the validation stage, we'll use the mzTab document as a context for this to obtain
    // the referenced assays and samples
    // Assay references
    private Set<Integer> assayRefsIndexes = null;
    private Set<Integer> sampleRefsIndexes = null;
    // Post-validation
    private Set<Assay> assayRefs = null;
    private Set<Sample> sampleRefs = null;

    public StudyVariable() {
        assayRefsIndexes = new HashSet<>();
        sampleRefsIndexes = new HashSet<>();
        assayRefs = new HashSet<>();
        sampleRefs = new HashSet<>();
    }

    public void addAssayRefIndex(int index) {
        assayRefsIndexes.add(index);
    }

    public void addSampleRefIndex(int index) {
        sampleRefsIndexes.add(index);
    }

    protected void addAssay(Assay assay) {
        assayRefs.add(assay);
    }

    protected void addSample(Sample sample) {
        sampleRefs.add(sample);
    }

    public Set<Integer> getReportedAssayRefIndexes() {
        // To prevent modifications to our internal data
        return new HashSet<>(assayRefsIndexes);
    }

    public Set<Integer> getReportedSampleRefIndexes() {
        // Prevent external manipulation
        return new HashSet<>(sampleRefsIndexes);
    }

    public Set<Assay> getAssays() {
        return new HashSet<>(assayRefs);
    }

    public Set<Sample> getSamples() {
        return new HashSet<>(sampleRefs);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean solveReferences(MzTabDocument context) {
        for (int assayIndex :
                assayRefsIndexes) {
            // cross-reference and retrieve assay via the context
            Assay assay = context.getMetaData().getAssay(assayIndex);
            if (assay == null) {
                logger.error("Assay with index '" + assayIndex + "' WAS NOT FOUND when solving references for study_variable entry");
                return false;
            }
        }
        for (int sampleIndex :
                sampleRefsIndexes) {
            // cross-reference and retrieve sample via the context
            Sample sample = context.getMetaData().getSampleData(sampleIndex);
            if (sample == null) {
                logger.error("Sample with index '" + sampleIndex + "' WAS NOT FOUND when solving references for study_variable entry");
                return false;
            }
        }
        return true;
    }

    public boolean validate(MzTabDocument context) {
        if (!solveReferences(context)) {
            logger.error("VALIDATION error for this study variable, as references COULD NOT be solved");
            return false;
        }
        if (context.getMetaData().getType() == MetaData.MzTabType.QUANTIFICATION) {
            if (getDescription() == null) {
                logger.error("study_variable description IS MANDATORY in mzTab type QUANTIFICATION (both complete and summary modes), but it is missing");
                return false;
            }
            if (context.getMetaData().getMode() == MetaData.MzTabMode.COMPLETE) {
                if (assayRefs.isEmpty()) {
                    logger.error("AT LEAST one assay reference is REQUIRED for study_variable in mzTab mode COMPLETE, mzTab type QUANTIFICATION");
                    return false;
                }
            }
        }
        return true;
    }
}
