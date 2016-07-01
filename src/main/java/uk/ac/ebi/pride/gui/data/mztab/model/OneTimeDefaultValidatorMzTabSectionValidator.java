package uk.ac.ebi.pride.gui.data.mztab.model;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-07-01 13:59
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class OneTimeDefaultValidatorMzTabSectionValidator extends DefaultMzTabSectionValidator {
    private boolean hasBeenValidated = false;
    private boolean isValid = false;

    @Override
    public boolean validate(MzTabDocument mzTabDocument, MetaData metaData) {
        return super.validate(mzTabDocument, metaData);
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, ProteinData proteinData) {
        return super.validate(mzTabDocument, proteinData);
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, PeptideData peptideData) {
        return super.validate(mzTabDocument, peptideData);
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, PsmData psmData) {
        return super.validate(mzTabDocument, psmData);
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, SmallMoleculeData smallMoleculeData) {
        return super.validate(mzTabDocument, smallMoleculeData);
    }
}
