package uk.ac.ebi.pride.gui.data.mztab.model;

import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidCvParameterException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-08 14:08
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class models sample tissue information
 */

public class Tissue extends CvParameter {
    public Tissue(String label, String accession, String name, String value) throws InvalidCvParameterException {
        super(label, accession, name, value);
    }
}
