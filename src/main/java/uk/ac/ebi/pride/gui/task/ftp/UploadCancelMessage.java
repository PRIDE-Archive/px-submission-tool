package uk.ac.ebi.pride.gui.task.ftp;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.task.Task;

/**
 * Message to send when a upload has been cancelled
 *
 * @author Rui Wang
 * @version $Id$
 */
public class UploadCancelMessage extends UploadMessage {

    public UploadCancelMessage(Task source, DataFile dataFile) {
        super(source, dataFile);
    }
}
