package uk.ac.ebi.pride.gui.data.mztab.exceptions;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.exceptions
 * Timestamp: 2016-06-08 14:49
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class InvalidMetaDataException extends RuntimeException {
    public InvalidMetaDataException(String message) {
        super(message);
    }
}
