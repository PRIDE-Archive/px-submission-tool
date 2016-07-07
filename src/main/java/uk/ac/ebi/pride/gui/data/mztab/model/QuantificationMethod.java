package uk.ac.ebi.pride.gui.data.mztab.model;

import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidCvParameterException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-07-07 11:21
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuantificationMethod extends CvParameter {
    public QuantificationMethod(String label, String accession, String name, String value) throws InvalidCvParameterException {
        super(label, accession, name, value);
    }

    public QuantificationMethod(CvParameter cv) {
        super(cv);
    }
}
