package uk.ac.ebi.pride.gui.data.mztab.model;

import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidCvParameterException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-07-07 22:20
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class MsRunHashMethod extends CvParameter {
    public MsRunHashMethod(String label, String accession, String name, String value) throws InvalidCvParameterException {
        super(label, accession, name, value);
    }

    public MsRunHashMethod(CvParameter cv) {
        super(cv);
    }
}
