package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidCvParameterException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-07-06 14:57
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class Assay {
    private static final Logger logger = LoggerFactory.getLogger(Assay.class);

    // WARNING - Only part of the assay[1-n] attributes in the metadata section are modeled here
    // TODO - Complete this bean with all properies of assay
    public class QuantificationReagent extends CvParameter {
        public QuantificationReagent(String label, String accession, String name, String value) throws InvalidCvParameterException {
            super(label, accession, name, value);
        }

        public QuantificationReagent(CvParameter cv) {
            super(cv);
        }
    }

    private int DEFAULT_INDEX_VALUE = -1;
    // For parsing time
    private int msRunRefIndex = DEFAULT_INDEX_VALUE;
    private int sampleRefIndex = DEFAULT_INDEX_VALUE;
    // Fixed at validation time
    private MsRun msRunRef = null;
    private Sample sampleRef = null;
    // CV parameter
    private QuantificationReagent quantificationReagent = null;

    public int getMsRunRefIndex() {
        return msRunRefIndex;
    }

    public void setMsRunRefIndex(int msRunRefIndex) {
        this.msRunRefIndex = msRunRefIndex;
    }

    public int getSampleRefIndex() {
        return sampleRefIndex;
    }

    public void setSampleRefIndex(int sampleRefIndex) {
        this.sampleRefIndex = sampleRefIndex;
    }

    public MsRun getMsRunRef() {
        return msRunRef;
    }

    public void setMsRunRef(MsRun msRunRef) {
        this.msRunRef = msRunRef;
    }

    public Sample getSampleRef() {
        return sampleRef;
    }

    public void setSampleRef(Sample sampleRef) {
        this.sampleRef = sampleRef;
    }

    public QuantificationReagent getQuantificationReagent() {
        return quantificationReagent;
    }

    public void setQuantificationReagent(CvParameter quantificationReagent) {
        this.quantificationReagent = new QuantificationReagent(quantificationReagent);
    }

    public boolean hasMsRunRefBeenSet() {
        return (getMsRunRefIndex() != DEFAULT_INDEX_VALUE);
    }

    public boolean hasSampleRefBeenSet() {
        return (getSampleRefIndex() != DEFAULT_INDEX_VALUE);
    }

    // Solve references
    public boolean solveReferences(MzTabDocument context) {
        if (!(hasMsRunRefBeenSet() || hasSampleRefBeenSet())) {
            logger.error("No references has been set for samples or ms-runs for this assay entry");
            return false;
        }
        if (getMsRunRefIndex() != DEFAULT_INDEX_VALUE) {
            MsRun msRun = context.getMetaData().getMsRunEntry(getMsRunRefIndex());
            if (msRun == null) {
                logger.error("Invalid ms-run reference index '" + getMsRunRefIndex() + "' when processing assay information");
                return false;
            }
            setMsRunRef(msRun);
        }
        if (getSampleRefIndex() != DEFAULT_INDEX_VALUE) {
            Sample sample = context.getMetaData().getSampleData(getSampleRefIndex());
            if (sample == null) {
                logger.error("Invalid sample reference index '" + getMsRunRefIndex() + "' when processing assay information");
                return false;
            }
        }
        return true;
    }

    public boolean validate(MzTabDocument context) throws ValidationException {
        // Solve references
        if (!solveReferences(context)) {
            return false;
        }
        // Quantification reagent is required for Complete mode
        if ((context.getMetaData().getMode() == MetaData.MzTabMode.COMPLETE)
                && (context.getMetaData().getType() == MetaData.MzTabType.QUANTIFICATION)) {
            if (getQuantificationReagent() == null) {
                logger.error("MISSING REQUIRED quantification_reagent information for this assay, in mzTab mode COMPLETE");
                return false;
            }
            if (getMsRunRef() == null) {
                logger.error("MISSING REQUIRED ms_run_ref information for this assay, in mzTab mode COMPLETE");
                return false;
            }
        }
        return true;
    }
}
