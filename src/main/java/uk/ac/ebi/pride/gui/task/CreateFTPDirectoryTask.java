package uk.ac.ebi.pride.gui.task;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.gui.task.ftp.UploadErrorMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadSuccessMessage;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;

import java.io.File;
import java.io.IOException;

/**
 * Create ftp directory for uploading
 *
 * @author Rui Wang
 * @version $Id$
 */
public class CreateFTPDirectoryTask extends TaskAdapter<Void, UploadMessage> {
    private static final Logger logger = LoggerFactory.getLogger(CreateFTPDirectoryTask.class);

    /**
     * ftp login details
     */
    private UploadDetail ftpDetail;
    /**
     * ftp client
     */
    private FTPClient ftp;

    /**
     * Constructor
     */
    public CreateFTPDirectoryTask(UploadDetail ftpDetail) {
        this.ftpDetail = ftpDetail;
    }

    @Override
    protected Void doInBackground() throws Exception {
        ftp = new FTPClient();
        ftp.setControlKeepAliveTimeout(300);

        try {
            int reply;
            ftp.connect(ftpDetail.getHost(), ftpDetail.getPort());
            ftp.login(ftpDetail.getDropBox().getUserName(), ftpDetail.getDropBox().getPassword());

            logger.debug("FTP local address: " + ftp.getLocalAddress().getCanonicalHostName() + ":" + ftp.getLocalPort());
            logger.debug("FTP passive host address: " + ftp.getPassiveHost() + ":" + ftp.getPassivePort());
            logger.debug("FTP remote host address: " + ftp.getRemoteAddress().getCanonicalHostName() + ":" + ftp.getRemotePort());

            // After connection attempt, you should check the reply code to verify
            // success.
            reply = ftp.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                publish(new UploadErrorMessage(this, null, "Failed to connect to FTP server"));
                return null;
            }

            // passive mode
            ftp.enterLocalPassiveMode();

            // change working directory
            File folder = new File(ftpDetail.getFolder());
            ftp.mkd(folder.getName());

            publish(new UploadSuccessMessage(this));

        } catch (IOException e) {
            if (!this.isCancelled()) {
                logger.error("IOException while upload ftp directory", e);
                publish(new UploadErrorMessage(this, null, "Failed to initiate FTP upload"));
            }
        } finally {
            if (!this.isCancelled()) {
                logger.debug("Freeing ftp resources before finishing");
                releaseResource();
            }
        }

        return null;
    }

    /**
     * Release ftp upload resources
     */
    private void releaseResource() {
        if (ftp != null && ftp.isConnected()) {
            try {
                // logout
                ftp.logout();
                // disconnect
                ftp.disconnect();
            } catch (IOException ioe) {
                // do nothing
            }
        }
    }
}
