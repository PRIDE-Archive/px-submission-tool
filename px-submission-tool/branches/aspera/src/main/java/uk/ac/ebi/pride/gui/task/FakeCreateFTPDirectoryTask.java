package uk.ac.ebi.pride.gui.task;

import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.gui.task.ftp.UploadMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadSuccessMessage;

/**
 * Fake create ftp directory task
 *
 * @author Rui Wang
 * @version $Id$
 */
public class FakeCreateFTPDirectoryTask extends TaskAdapter<Void, UploadMessage> {

    /**
     * ftp login details
     */
    private UploadDetail ftpDetail;

    /**
     * Constructor
     */
    public FakeCreateFTPDirectoryTask(UploadDetail ftpDetail) {
        this.ftpDetail = ftpDetail;
    }

    @Override
    protected Void doInBackground() throws Exception {
        publish(new UploadSuccessMessage(this));

        return null;
    }
}
