package uk.ac.ebi.pride.gui.data.mztab.exceptions;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.exceptions
 * Timestamp: 2016-06-27 15:52
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class InvalidProteinSection extends RuntimeException {
    public InvalidProteinSection(String message) {
        super(message);
    }
}
