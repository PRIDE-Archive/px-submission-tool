package uk.ac.ebi.pride.gui.data.mztab.model;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-07-01 13:59
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This subclass makes sure that the default validation strategy is run only once per mzTab section
 */
public class OneTimeDefaultValidatorMzTabSectionValidator extends DefaultMzTabSectionValidator {
    private boolean hasBeenValidated = false;
    private boolean isValid = false;

    @Override
    public boolean validate(MzTabDocument mzTabDocument, MetaData metaData) throws MzTabSectionValidatorException {
        if (!hasBeenValidated) {
            isValid = super.validate(mzTabDocument, metaData);
            hasBeenValidated = true;
        }
        return isValid;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, ProteinData proteinData) throws MzTabSectionValidatorException {
        if (!hasBeenValidated) {
            isValid = super.validate(mzTabDocument, proteinData);
            hasBeenValidated = true;
        }
        return isValid;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, PeptideData peptideData) throws MzTabSectionValidatorException {
        if (!hasBeenValidated) {
            isValid = super.validate(mzTabDocument, peptideData);
            hasBeenValidated = true;
        }
        return isValid;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, PsmData psmData) throws MzTabSectionValidatorException {
        if (!hasBeenValidated) {
            isValid = super.validate(mzTabDocument, psmData);
            hasBeenValidated = true;
        }
        return isValid;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, SmallMoleculeData smallMoleculeData) throws MzTabSectionValidatorException {
        if (!hasBeenValidated) {
            isValid = super.validate(mzTabDocument, smallMoleculeData);
            hasBeenValidated = true;
        }
        return isValid;
    }
}
