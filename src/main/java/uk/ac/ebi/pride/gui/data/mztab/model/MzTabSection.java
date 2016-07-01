package uk.ac.ebi.pride.gui.data.mztab.model;

import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidProteinSection;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-07-01 13:45
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Common behaviour across all the sections
 */
public abstract class MzTabSection {
    /**
     * Implements the validation of a particular subsection by using the given validation strategy
     * @param mzTabDocument context for performing the validation
     * @param validator validation algorithm/strategy
     * @throws InvalidProteinSection thrown if the section is not valid
     */
    public boolean validate(MzTabDocument mzTabDocument, MzTabSectionValidator validator) throws InvalidMzTabSectionException {
        try {
            return validator.validate(mzTabDocument, this);
        } catch (MzTabSectionValidatorException e) {
            throw new InvalidMzTabSectionException("An ERROR occurred while validating mzTab section: " + e.getMessage());
        }
    }
}
