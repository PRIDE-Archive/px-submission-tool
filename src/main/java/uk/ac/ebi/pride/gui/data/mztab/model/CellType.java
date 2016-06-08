package uk.ac.ebi.pride.gui.data.mztab.model;

import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidCvParameterException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-08 14:12
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class models Sample Cell Type information
 */

public class CellType extends CvParameter {
    public CellType(String label, String accession, String name, String value) throws InvalidCvParameterException {
        super(label, accession, name, value);
    }
}
