package uk.ac.ebi.pride.gui.task.ftp;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.task.Task;

/**
 * Message contains error message generated from upload
 *
 * @author Rui Wang
 * @version $Id$
 */
public class UploadErrorMessage extends UploadMessage {
    private String message;

    public UploadErrorMessage(Task source, DataFile dataFile, String msg) {
        super(source, dataFile);
        this.message = msg;
    }

    public String getMessage() {
        return message;
    }
}
