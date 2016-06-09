package uk.ac.ebi.pride.gui.data.mztab.exceptions;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.exceptions
 * Timestamp: 2016-06-09 1:43
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class InvalidMzTabDocument extends RuntimeException {
    public InvalidMzTabDocument(String message) {
        super(message);
    }
}
