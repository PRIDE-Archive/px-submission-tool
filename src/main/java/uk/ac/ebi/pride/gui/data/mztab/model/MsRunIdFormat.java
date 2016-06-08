package uk.ac.ebi.pride.gui.data.mztab.model;

import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidCvParameterException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-08 11:25
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class MsRunIdFormat extends CvParameter {

    public MsRunIdFormat(String label, String accession, String name, String value) throws InvalidCvParameterException {
        super(label, accession, name, value);
    }
}
