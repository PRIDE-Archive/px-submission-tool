package uk.ac.ebi.pride.gui.data.mztab.model;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-07-01 13:58
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class DefaultMzTabSectionValidator extends MzTabSectionValidator {
    @Override
    public boolean validate(MzTabDocument mzTabDocument, MetaData metaData) throws MzTabSectionValidatorException {
        // TODO
        return false;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, ProteinData proteinData) throws MzTabSectionValidatorException {
        // TODO
       return false;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, PeptideData peptideData) throws MzTabSectionValidatorException {
        // TODO
        return false;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, PsmData psmData) throws MzTabSectionValidatorException {
        // TODO
        return false;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, SmallMoleculeData smallMoleculeData) throws MzTabSectionValidatorException {
        // TODO
        return false;
    }
}
