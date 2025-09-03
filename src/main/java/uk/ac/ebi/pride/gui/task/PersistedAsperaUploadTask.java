package uk.ac.ebi.pride.gui.task;

import com.asperasoft.faspmanager.Encryption;
import com.asperasoft.faspmanager.FaspManager;
import com.asperasoft.faspmanager.FaspManagerException;
import com.asperasoft.faspmanager.FileInfo;
import com.asperasoft.faspmanager.FileState;
import com.asperasoft.faspmanager.InitializationException;
import com.asperasoft.faspmanager.Manifest;
import com.asperasoft.faspmanager.Overwrite;
import com.asperasoft.faspmanager.Policy;
import com.asperasoft.faspmanager.RemoteLocation;
import com.asperasoft.faspmanager.Resume;
import com.asperasoft.faspmanager.SessionStats;
import com.asperasoft.faspmanager.TransferEvent;
import com.asperasoft.faspmanager.TransferListener;
import com.asperasoft.faspmanager.XferParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.submission.DropBoxDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.gui.aspera.PersistedAsperaFileUploader;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.task.ftp.UploadErrorMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadProgressMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadSuccessMessage;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;

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
     *
     * @throws FaspManagerException         problems using the Aspera API to perform the upload
     * @throws UnsupportedEncodingException problems creating a temporary file
     */
    void asperaUpload() throws FaspManagerException, UnsupportedEncodingException {
        // Prepare submission to calculate totalFileSize and set up filesToSubmitIter
        prepareSubmission();
        
        String ascpLocation = chooseAsperaBinary();
        logger.debug("Aspera binary location {}", ascpLocation);
        File executable = new File(ascpLocation);
        XferParams defaultTransferParams = getDefaultTransferParams();
        PersistedAsperaFileUploader uploader = new PersistedAsperaFileUploader(executable, submissionRecord);
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
     *
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
        xferParams.resumeCheck = Resume.FILE_ATTRIBUTES;
        xferParams.preCalculateJobSize = Boolean.parseBoolean(appContext.getProperty("aspera.xfer.preCalculateJobSize"));
        xferParams.createPath = Boolean.parseBoolean(appContext.getProperty("aspera.xfer.createPath"));
        xferParams.persist = true;
        return xferParams;
    }

    /**
     * Processes a file session event.
     *
     * @param transferEvent the transfer event
     * @param sessionStats  the session status
     * @param fileInfo      the file information
     */
    @Override
    public void fileSessionEvent(TransferEvent transferEvent, SessionStats sessionStats, FileInfo fileInfo) {
        FaspManager faspManager;
        try {
            faspManager = FaspManager.getSingleton();
        } catch (InitializationException e) {
            FaspManager.destroy();
            String initErrorMsg = "Failed to initialize Aspera transfer manager" +
                "\n\nThis could be due to network restrictions, firewall settings, or network instability." +
                "\n\nAlternative options:" +
                "\n1. Go back one step and select FTP upload instead" +
                "\n2. Use Globus for file transfer: https://www.ebi.ac.uk/pride/markdownpage/globus" +
                "\n3. Contact your system administrator to enable Aspera ports (TCP & UDP 33001)";
            publish(new UploadErrorMessage(this, null, initErrorMsg));
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
                        String faspErrorMsg = "Failed to upload file via Aspera: " + file.getName() +
                            "\n\nThis could be due to network restrictions, firewall settings, or network instability." +
                            "\n\nAlternative options:" +
                            "\n1. Go back one step and select FTP upload instead" +
                            "\n2. Use Globus for file transfer: https://www.ebi.ac.uk/pride/markdownpage/globus" +
                            "\n3. Contact your system administrator to enable Aspera ports (TCP & UDP 33001)";
                        publish(new UploadErrorMessage(this, null, faspErrorMsg));
                    }
                    count++;
                }
                break;
            case PROGRESS:
                int uploadedNumOfFiles = (int) sessionStats.getFilesComplete();
                long transferredBytes = sessionStats.getTotalTransferredBytes();
                logger.debug("Aspera transfer in progress");
                logger.debug("Total files: {}", totalNumOfFiles);
                logger.debug("Files uploaded: {}", uploadedNumOfFiles);
                logger.debug("Total file size: {} bytes", totalFileSize);
                logger.debug("Uploaded file size: {} bytes", transferredBytes);
                
                // Always publish progress, even if totalFileSize is 0
                // Use transferredBytes as the total if totalFileSize is 0
                long effectiveTotalSize = totalFileSize > 0 ? totalFileSize : Math.max(transferredBytes, 1);
                publish(new UploadProgressMessage(this, null, effectiveTotalSize, transferredBytes, totalNumOfFiles, uploadedNumOfFiles));
                break;
            case FILE_ERROR:
                logger.info("File " + fileInfo.getName() + "Failed to submit" + fileInfo.getErrDescription());
                String fileErrorMsg = "Failed to upload file via Aspera: " + fileInfo.getName() + 
                    "\n\nAlternative: Consider using Globus for file transfer." +
                    "\nVisit: https://www.ebi.ac.uk/pride/markdownpage/globus";
                publish(new UploadErrorMessage(this, null, fileErrorMsg));
                break;
            case FILE_STOP:
                if (fileInfo.getState().equals(FileState.FINISHED)) {
                    finishedFileCount++;
                    String[] fileNameArray = fileInfo.getName().split("/");
                    logger.info("File {} uploaded successfully", fileNameArray[fileNameArray.length - 1]);
                    
                    // Calculate current progress based on completed files
                    // If totalFileSize is 0, use file count progress instead
                    long currentProgress;
                    if (totalFileSize > 0) {
                        currentProgress = (finishedFileCount * totalFileSize) / totalNumOfFiles;
                    } else {
                        // Use file count progress when totalFileSize is 0
                        currentProgress = (finishedFileCount * 100) / totalNumOfFiles;
                    }
                    publish(new UploadProgressMessage(this, null, totalFileSize > 0 ? totalFileSize : 100, currentProgress, totalNumOfFiles, finishedFileCount));
                    
                    // last file has finished uploading, add a new one
                    if (filesToSubmitIter.hasNext()) {
                        File file = filesToSubmitIter.next();
                        try {
                            faspManager.addSource(sessionStats.getId(), file.getAbsolutePath(), folder + File.separator + file.getName());
                        } catch (FaspManagerException e) {
                            FaspManager.destroy();
                            String faspErrorMsg = "Failed to upload file via Aspera: " + file.getName() +
                                "\n\nThis could be due to network restrictions, firewall settings, or network instability." +
                                "\n\nAlternative options:" +
                                "\n1. Go back one step and select FTP upload instead" +
                                "\n2. Use Globus for file transfer: https://www.ebi.ac.uk/pride/markdownpage/globus" +
                                "\n3. Contact your system administrator to enable Aspera ports (TCP & UDP 33001)";
                            publish(new UploadErrorMessage(this, null, faspErrorMsg));
                        }
                    } else {
                        if (sessionStats.getFilesComplete() == totalNumOfFiles + 1) {
                            FaspManager.destroy();
                            // Send final progress message with appropriate total size
                            long finalTotalSize = totalFileSize > 0 ? totalFileSize : 100;
                            publish(new UploadProgressMessage(this, null, finalTotalSize, finalTotalSize, totalNumOfFiles, totalNumOfFiles));
                            publish(new UploadSuccessMessage(this));
                            logger.info("Aspera all files transferred");
                        }
                    }
                }
                break;
            case SESSION_STOP:
                // Send final progress message with appropriate total size
                long finalTotalSize = totalFileSize > 0 ? totalFileSize : 100;
                publish(new UploadProgressMessage(this, null, finalTotalSize, finalTotalSize, totalNumOfFiles, totalNumOfFiles));
                publish(new UploadSuccessMessage(this));
                FaspManager.destroy();
                logger.info("Aspera Session Stop");
                break;
            case SESSION_ERROR:
                logger.error("Aspera session Error: " + transferEvent.getDescription());
                FaspManager.destroy();
                String asperaErrorMsg = "Aspera transfer failed: " + transferEvent.getDescription() + 
                    "\n\nThis could be due to network restrictions, firewall settings, or network instability." +
                    "\n\nAlternative options:" +
                    "\n1. Go back one step and select FTP upload instead" +
                    "\n2. Use Globus for file transfer: https://www.ebi.ac.uk/pride/markdownpage/globus" +
                    "\n3. Contact your system administrator to enable Aspera ports (TCP & UDP 33001)";
                publish(new UploadErrorMessage(this, null, asperaErrorMsg));
                break;
        }
    }
}
