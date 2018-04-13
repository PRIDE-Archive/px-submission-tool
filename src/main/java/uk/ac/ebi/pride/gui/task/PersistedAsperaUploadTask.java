package uk.ac.ebi.pride.gui.task;

import com.asperasoft.faspmanager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.submission.DropBoxDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.gui.aspera.PersistedAsperaFileUploader;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.gui.task.ftp.*;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * Uploads data via Aspera.
 */

public class PersistedAsperaUploadTask extends AsperaGeneralTask implements TransferListener {

    public static final Logger logger = LoggerFactory.getLogger(AsperaUploadTask.class);

    /**
     * Finished file count
     */
    private int finishedFileCount;

    /**
     * Constructor used for a new submission
     *
     * @param submissionRecord submission record
     */
    public PersistedAsperaUploadTask(SubmissionRecord submissionRecord) {
        super(submissionRecord);
    }

    /**
     * Handles uploading of files using Asopera
     * @throws FaspManagerException problems using the Aspera API to perform the upload
     * @throws UnsupportedEncodingException problems creating a temporary file
     */
    void asperaUpload() throws FaspManagerException, UnsupportedEncodingException {
        String ascpLocation = chooseAsperaBinary();
        logger.debug("Aspera binary location {}", ascpLocation);
        File executable = new File(ascpLocation);
        XferParams defaultTransferParams = getDefaultTransferParams();
        PersistedAsperaFileUploader uploader = new PersistedAsperaFileUploader(executable);
        uploader.addTransferListener(this);
        final UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        final DropBoxDetail dropBox = uploadDetail.getDropBox();
        RemoteLocation remoteLocation = new RemoteLocation(uploadDetail.getHost(), dropBox.getUserName(), dropBox.getPassword());
        String session = uploader.startTransferSession(remoteLocation, defaultTransferParams);
        logger.debug("Transfer Session ID: {}", session);
    }

    /**
     * Set the default transfer parameters for this transfer.
     * For supported parameters see class #XferParams
     * For additional descriptions see the Aspera documentation
     * of the command line tool. For example at
     * http://download.asperasoft.com/download/docs/ascp/2.7/html/index.html
     * @return the default transfer parameters.
     */
    private static XferParams getDefaultTransferParams() {
        DesktopContext appContext = App.getInstance().getDesktopContext();
        XferParams xferParams = new XferParams();
        xferParams.tcpPort = Integer.parseInt(appContext.getProperty("aspera.xfer.tcpPort"));
        xferParams.udpPort = Integer.parseInt(appContext.getProperty("aspera.xfer.udpPort")); // port used for data transfer
        xferParams.targetRateKbps = Integer.parseInt(appContext.getProperty("aspera.xfer.targetRateKbps"));
        xferParams.minimumRateKbps = Integer.parseInt(appContext.getProperty("aspera.xfer.minimumRateKbps"));
        xferParams.encryption = Encryption.DEFAULT;
        xferParams.overwrite = Overwrite.DIFFERENT;
        xferParams.generateManifest = Manifest.NONE;
        xferParams.policy = Policy.FAIR;
        xferParams.resumeCheck = Resume.SPARSE_CHECKSUM;
        xferParams.preCalculateJobSize = Boolean.parseBoolean(appContext.getProperty("aspera.xfer.preCalculateJobSize"));
        xferParams.createPath = Boolean.parseBoolean(appContext.getProperty("aspera.xfer.createPath"));
        xferParams.persist = true;
        return xferParams;
    }
    /**
     * Processes a file session event.
     * @param transferEvent the transfer event
     * @param sessionStats the session status
     * @param fileInfo the file information
     */
    @Override
    public void fileSessionEvent(TransferEvent transferEvent, SessionStats sessionStats, FileInfo fileInfo) {
        FaspManager faspManager;
        try {
            faspManager = FaspManager.getSingleton();
        } catch (InitializationException e) {
            FaspManager.destroy();
            publish(new UploadErrorMessage(this, null, "Failed to initialize via Aspera manager"));
            return;
        }
        int totalNumOfFiles = submissionRecord.getSubmission().getDataFiles().size();
        UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        String folder = uploadDetail.getFolder();
        switch (transferEvent) {
            case SESSION_START:
                int count = 0;
                while (filesToSubmitIter.hasNext() && count < 20) {
                    File file = filesToSubmitIter.next();
                    try {
                        faspManager.addSource(sessionStats.getId(), file.getAbsolutePath(), folder + File.separator + file.getName());
                    } catch (FaspManagerException e) {
                        FaspManager.destroy();
                        publish(new UploadErrorMessage(this, null, "Failed to upload next file via Aspera: " + file.getName()));
                    }
                    count++;
                }
                break;
            case PROGRESS:
                int uploadedNumOfFiles = (int) sessionStats.getFilesComplete();
                logger.debug("Aspera transfer in progress");
                logger.debug("Total files: ");
                logger.debug("Total files: " + totalNumOfFiles);
                logger.debug("Files uploaded: " + uploadedNumOfFiles);
                logger.debug("Total file size " + totalFileSize);
                logger.debug("Uploaded file size " + sessionStats.getTotalTransferredBytes());
                publish(new UploadProgressMessage(this, null, totalFileSize, sessionStats.getTotalTransferredBytes(), totalNumOfFiles, uploadedNumOfFiles));
                break;
            case FILE_STOP:
                if (fileInfo.getState().equals(FileState.FINISHED)) {
                    finishedFileCount++;
                    // last file has finished uploading, add a new one
                    if (filesToSubmitIter.hasNext()) {
                        File file = filesToSubmitIter.next();
                        try {
                            faspManager.addSource(sessionStats.getId(), file.getAbsolutePath(), folder + File.separator + file.getName());
                        } catch (FaspManagerException e) {
                            FaspManager.destroy();
                            publish(new UploadErrorMessage(this, null, "Failed to upload next file via Aspera: " + file.getName()));
                        }
                    } else {
                        if (finishedFileCount == totalNumOfFiles + 1) {
                            FaspManager.destroy();
                            publish(new UploadProgressMessage(this, null, totalFileSize, totalFileSize, totalNumOfFiles, totalNumOfFiles));
                            publish(new UploadSuccessMessage(this));
                            logger.debug("Aspera Session Stop");
                        }
                    }
                }
                break;
            case SESSION_STOP:
                FaspManager.destroy();
                publish(new UploadProgressMessage(this, null, totalFileSize, totalFileSize, totalNumOfFiles, totalNumOfFiles));
                publish(new UploadSuccessMessage(this));
                logger.debug("Aspera Session Stop");
                break;
            case SESSION_ERROR:
                logger.debug("Aspera session Error: " + transferEvent.getDescription());
                FaspManager.destroy();
                publish(new UploadErrorMessage(this, null, "Failed to upload via Aspera: " + transferEvent.getDescription()));
                break;
        }
    }
}
