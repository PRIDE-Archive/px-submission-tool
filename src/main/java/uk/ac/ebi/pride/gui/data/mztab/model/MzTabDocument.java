package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-06-08 23:31
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class MzTabDocument implements ValidableProduct {
    private static final Logger logger = LoggerFactory.getLogger(MzTabDocument.class);

    // mzTab Sections
    private MetaData metaData = null;
    private ProteinData proteinData = null;
    private PsmData psmData = null;
    private SmallMoleculeData smallMoleculeData = null;
    private PeptideData peptideData = null;

    public MzTabDocument() {
        metaData = null;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    public ProteinData getProteinData() {
        return proteinData;
    }

    public void setProteinData(ProteinData proteinData) {
        this.proteinData = proteinData;
    }

    public PsmData getPsmData() {
        return psmData;
    }

    public void setPsmData(PsmData psmData) {
        this.psmData = psmData;
    }

    public SmallMoleculeData getSmallMoleculeData() {
        return smallMoleculeData;
    }

    public void setSmallMoleculeData(SmallMoleculeData smallMoleculeData) {
        this.smallMoleculeData = smallMoleculeData;
    }

    public PeptideData getPeptideData() {
        return peptideData;
    }

    public void setPeptideData(PeptideData peptideData) {
        this.peptideData = peptideData;
    }

    @Override
    public boolean validate(MzTabSectionValidator validator) throws ValidationException {
        // Call Validate on every subproduct
        // Validate Metadata section (required)
        if (getMetaData() == null) {
            logger.error("MISSING REQUIRED Metadata section!!! Seriously! What are you doing!?!?");
            return false;
        }
        if (!getMetaData().validate(this, validator)) {
            logger.error("Metadata section is NOT VALID, please, check logging messages");
            return false;
        }
        // Validate protein section
        if ((getProteinData() != null) && (!getProteinData().validate(this, validator))) {
            logger.error("Protein section is NOT VALID, please, check logging messages");
            return false;
        }
        // Validate Peptide section
        if ((getPeptideData() != null) && (!getPeptideData().validate(this, validator))) {
            logger.error("Peptide section is NOT VALID, please, check logging messages");
            return false;
        }
        // Validate PSM section
        if ((getPsmData() != null) && (!getPsmData().validate(this, validator))) {
            logger.error("PSM section is NOT VALID, please, check logging messages");
            return false;
        }
        // Validate Small Molecules section
        if ((getSmallMoleculeData() != null) && (!getSmallMoleculeData().validate(this, validator))) {
            logger.error("Small Molecule section is NOT VALID, please, check logging messages");
            return false;
        }
        // TODO - apply document wide validation criteria (like requirements depending on mzTab type and mode specified)
        return true;
    }
}
