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
    public boolean validate() throws ValidationException {
        // TODO - Call Validate on every subproduct
        // TODO - apply document wide validation criteria (like requirements depending on mzTab type and mode specified)
        return false;
    }
}
