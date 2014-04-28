package uk.ac.ebi.pride.gui.navigation;

/**
 * Message generated during navigation
 *
 * @author Rui Wang
 * @version $Id$
 */
public class NavigationMessage {
    public enum Type {ERROR, WARNING, INFO, SUCCESS}

    private Type type;

    private String message;

    public NavigationMessage(Type type) {
        this(type, null);
    }

    public NavigationMessage(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
