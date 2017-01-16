package uk.ac.ebi.pride.gui.task.ftp;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.toolsuite.gui.task.Task;

/**
 * Information about the upload, can be used to notifying which file is being uploaded.
 *
 * @author Rui Wang
 * @version $Id$
 */
public class UploadInfoMessage extends UploadMessage{
    private String info;

    public UploadInfoMessage(Task source, DataFile dataFile, String info) {
        super(source, dataFile);
        this.info = info;
    }

    public String getInfo() {
        return info;
    }
}
