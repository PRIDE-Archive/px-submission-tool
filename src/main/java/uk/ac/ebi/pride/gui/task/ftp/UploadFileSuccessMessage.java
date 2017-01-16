package uk.ac.ebi.pride.gui.task.ftp;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.toolsuite.gui.task.Task;

/**
 * Message to indicate a upload has been successful
 *
 * @author Rui Wang
 * @version $Id$
 */
public class UploadFileSuccessMessage extends UploadMessage{

    public UploadFileSuccessMessage(Task source, DataFile dataFile) {
        super(source, dataFile);
    }
}
