package uk.ac.ebi.pride.gui.util;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ValidationMessage {

    private ValidationState state;

    private String message;

    public ValidationMessage(ValidationState state, String message) {
        this.state = state;
        this.message = message;
    }

    public ValidationState getState() {
        return state;
    }

    public void setState(ValidationState state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
