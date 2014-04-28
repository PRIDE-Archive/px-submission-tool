package uk.ac.ebi.pride.gui.task.ftp;

import uk.ac.ebi.pride.gui.task.Task;

/**
 * Success message after an entire submission has finished
 *
 * @author Rui Wang
 * @version $Id$
 */
public class UploadSuccessMessage extends UploadMessage {

    public UploadSuccessMessage(Task source) {
        super(source, null);
    }
}
