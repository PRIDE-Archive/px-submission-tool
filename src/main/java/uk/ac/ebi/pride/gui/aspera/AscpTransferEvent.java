package uk.ac.ebi.pride.gui.aspera;

/**
 * Transfer lifecycle events emitted while parsing ascp CLI output.
 */
public enum AscpTransferEvent {
    CONNECTING,
    SESSION_START,
    PROGRESS,
    FILE_STOP,
    FILE_ERROR,
    SESSION_STOP,
    SESSION_ERROR,
    RATE_MODIFICATION;

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
