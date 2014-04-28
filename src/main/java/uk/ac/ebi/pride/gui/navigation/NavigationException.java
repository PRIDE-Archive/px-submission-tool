package uk.ac.ebi.pride.gui.navigation;

/**
 * Exception created by navigation components
 *
 * @author Rui Wang
 * @version $Id$
 */
public class NavigationException extends Exception{

    public NavigationException(String message) {
        super(message);
    }

    public NavigationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NavigationException(Throwable cause) {
        super(cause);
    }
}
