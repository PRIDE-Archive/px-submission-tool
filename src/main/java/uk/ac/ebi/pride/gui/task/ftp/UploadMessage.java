package uk.ac.ebi.pride.gui.task.ftp;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.task.Task;

/**
 * Message return by tasks for ftp upload
 *
 * @author Rui Wang
 * @version $Id$
 */
public abstract class UploadMessage {
    private Task source;
    private DataFile dataFile;

    public UploadMessage(Task source, DataFile dataFile) {
        this.source = source;
        this.dataFile = dataFile;
    }

    public Task getSource() {
        return source;
    }

    public DataFile getDataFile() {
        return dataFile;
    }
}
