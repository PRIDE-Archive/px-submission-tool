package uk.ac.ebi.pride.gui.data.mztab.model;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-07-01 13:33
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This is kind of a visitor that externalized
 */
public abstract class MzTabSectionValidator {
    public abstract boolean validate(MzTabDocument mzTabDocument, MetaData metaData) throws MzTabSectionValidatorException;
    public abstract boolean validate(MzTabDocument mzTabDocument, ProteinData proteinData) throws MzTabSectionValidatorException;
    public abstract boolean validate(MzTabDocument mzTabDocument, PeptideData peptideData) throws MzTabSectionValidatorException;
    public abstract boolean validate(MzTabDocument mzTabDocument, PsmData psmData) throws MzTabSectionValidatorException;
    public abstract boolean validate(MzTabDocument mzTabDocument, SmallMoleculeData smallMoleculeData) throws MzTabSectionValidatorException;

    public boolean validate(MzTabDocument mzTabDocument, MzTabSection mzTabSection) throws MzTabSectionValidatorException {
        // We don't validate items of the superclass
        return false;
    }
}
