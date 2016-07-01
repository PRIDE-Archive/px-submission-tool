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
    public boolean validate(MzTabDocument mzTabDocument, MetaData metaData) {
        // TODO
        return false;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, ProteinData proteinData) {
        // TODO
       return false;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, PeptideData peptideData) {
        // TODO
        return false;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, PsmData psmData) {
        // TODO
        return false;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, SmallMoleculeData smallMoleculeData) {
        // TODO
        return false;
    }
}
