package uk.ac.ebi.pride.gui.aspera;

/**
 * Exception raised when an ascp CLI transfer fails to start or complete.
 */
public class AscpTransferException extends Exception {

    public AscpTransferException(String message) {
        super(message);
    }

    public AscpTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}
