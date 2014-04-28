package uk.ac.ebi.pride.gui.task;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.task.ftp.*;

/**
 * Fake the ftp file transfer
 *
 * @author Rui Wang
 * @version $Id$
 */
public class FakeFileFTPUploadTask extends TaskAdapter<Void, UploadMessage> {

    private DataFile dataFile;

    public FakeFileFTPUploadTask(DataFile dataFile,
                             FTPDetail ftpDetail) {
        this.dataFile = dataFile;
    }

    @Override
    protected Void doInBackground() throws Exception {
        long fileSize = dataFile.getFile().length()/1024;
        long bytesTransferred = 0;

        while(bytesTransferred <= fileSize) {
            publish(new UploadProgressMessage(this, dataFile, fileSize, bytesTransferred));
            bytesTransferred += 2000;
            Thread.sleep(100);
        }

        publish(new UploadFileSuccessMessage(this, dataFile));

        return null;
    }

    @Override
    protected void cancelled() {
        publish(new UploadCancelMessage(this, dataFile));
    }
}
