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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Uploads data via Aspera.
 */

public class PersistedAsperaUploadTask extends AsperaGeneralTask implements TransferListener {

    public static final Logger logger = LoggerFactory.getLogger(AsperaUploadTask.class);
    
    // Timeout and monitoring constants (configurable via settings)
    private final long TRANSFER_TIMEOUT_MS;
    private final long PROGRESS_TIMEOUT_MS;
    private final long MONITORING_INTERVAL_MS;

    /**
     * Finished file count
     */
    private int finishedFileCount;
    
    /**
     * Transfer monitoring
     */
    private final AtomicLong lastProgressTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong transferStartTime = new AtomicLong(0);
    private final AtomicBoolean transferCompleted = new AtomicBoolean(false);
    private final AtomicBoolean transferTimedOut = new AtomicBoolean(false);

    /**
     * Constructor used for a new submission
     *
     * @param submissionRecord submission record
     */
    public PersistedAsperaUploadTask(SubmissionRecord submissionRecord) {
        super(submissionRecord);
        
        // Initialize timeout values from configuration with fallback defaults
        long transferTimeout = 10 * 60 * 1000; // 10 minutes default
        long progressTimeout = 2 * 60 * 1000;  // 2 minutes default
        long monitoringInterval = 60 * 1000;   // 60 seconds default (less frequent monitoring)
        
        try {
            DesktopContext appContext = App.getInstance().getDesktopContext();
            transferTimeout = Long.parseLong(appContext.getProperty("aspera.timeout.transfer"));
            progressTimeout = Long.parseLong(appContext.getProperty("aspera.timeout.progress"));
            monitoringInterval = Long.parseLong(appContext.getProperty("aspera.timeout.monitoring"));
            logger.debug("Aspera timeout configuration loaded: transfer={}ms, progress={}ms, monitoring={}ms", 
                transferTimeout, progressTimeout, monitoringInterval);
        } catch (Exception e) {
            // Fallback to default values when Desktop is not available (e.g., in tests)
            logger.debug("Using default Aspera timeout configuration: transfer={}ms, progress={}ms, monitoring={}ms", 
                transferTimeout, progressTimeout, monitoringInterval);
        }
        
        // Assign to final fields
        TRANSFER_TIMEOUT_MS = transferTimeout;
        PROGRESS_TIMEOUT_MS = progressTimeout;
        MONITORING_INTERVAL_MS = monitoringInterval;
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
        
        // Start transfer monitoring (if enabled)
        transferStartTime.set(System.currentTimeMillis());
        if (isMonitoringEnabled()) {
            startTransferMonitoring();
        } else {
            logger.debug("Aspera transfer monitoring disabled for better performance");
        }
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
        xferParams.resumeCheck = Resume.SPARSE_CHECKSUM; // Use faster resume check like original
        xferParams.preCalculateJobSize = Boolean.parseBoolean(appContext.getProperty("aspera.xfer.preCalculateJobSize"));
        xferParams.createPath = Boolean.parseBoolean(appContext.getProperty("aspera.xfer.createPath"));
        xferParams.persist = false; // Disable persistence to reduce overhead
        return xferParams;
    }
    
    /**
     * Starts a background thread to monitor transfer progress and detect timeouts
     */
    private void startTransferMonitoring() {
        Thread monitoringThread = new Thread(() -> {
            logger.info("Starting Aspera transfer monitoring");
            try {
                while (!transferCompleted.get() && !transferTimedOut.get()) {
                    Thread.sleep(MONITORING_INTERVAL_MS);
                    
                    long currentTime = System.currentTimeMillis();
                    long timeSinceStart = currentTime - transferStartTime.get();
                    long timeSinceProgress = currentTime - lastProgressTime.get();
                    
                    // Check for overall timeout
                    if (timeSinceStart > TRANSFER_TIMEOUT_MS) {
                        logger.error("Aspera transfer timed out after {} minutes", TRANSFER_TIMEOUT_MS / 60000);
                        transferTimedOut.set(true);
                        handleTransferTimeout();
                        break;
                    }
                    
                    // Check for progress timeout
                    if (timeSinceProgress > PROGRESS_TIMEOUT_MS) {
                        logger.error("Aspera transfer stuck - no progress for {} minutes", PROGRESS_TIMEOUT_MS / 60000);
                        transferTimedOut.set(true);
                        handleTransferStuck();
                        break;
                    }
                    
                    logger.debug("Transfer monitoring: {}s elapsed, {}s since last progress", 
                        timeSinceStart / 1000, timeSinceProgress / 1000);
                }
            } catch (InterruptedException e) {
                logger.info("Transfer monitoring interrupted");
                Thread.currentThread().interrupt();
            }
        });
        monitoringThread.setDaemon(true);
        monitoringThread.setName("AsperaTransferMonitor");
        monitoringThread.start();
    }
    
    /**
     * Handles transfer timeout
     */
    private void handleTransferTimeout() {
        FaspManager.destroy();
        String timeoutMsg = "Aspera transfer timed out after " + (TRANSFER_TIMEOUT_MS / 60000) + " minutes" +
            "\n\nThis could be due to:" +
            "\n• Large file sizes taking longer than expected" +
            "\n• Network instability or slow connection" +
            "\n• Server-side processing delays" +
            "\n\nAlternative options:" +
            "\n1. Go back one step and select FTP upload instead" +
            "\n2. Use Globus for file transfer: https://www.ebi.ac.uk/pride/markdownpage/globus" +
            "\n3. Try again with a more stable network connection";
        publish(new UploadErrorMessage(this, null, timeoutMsg));
    }
    
    /**
     * Handles stuck transfer (no progress)
     */
    private void handleTransferStuck() {
        FaspManager.destroy();
        String stuckMsg = "Aspera transfer appears to be stuck - no progress for " + (PROGRESS_TIMEOUT_MS / 60000) + " minutes" +
            "\n\nThis could be due to:" +
            "\n• Network connectivity issues" +
            "\n• Firewall blocking Aspera ports (TCP & UDP 33001)" +
            "\n• Server-side problems" +
            "\n• Network instability" +
            "\n\nAlternative options:" +
            "\n1. Go back one step and select FTP upload instead" +
            "\n2. Use Globus for file transfer: https://www.ebi.ac.uk/pride/markdownpage/globus" +
            "\n3. Contact your system administrator to enable Aspera ports" +
            "\n4. Try again with a more stable network connection";
        publish(new UploadErrorMessage(this, null, stuckMsg));
    }
    
    /**
     * Checks if transfer monitoring is enabled
     */
    private boolean isMonitoringEnabled() {
        try {
            DesktopContext appContext = App.getInstance().getDesktopContext();
            return Boolean.parseBoolean(appContext.getProperty("aspera.timeout.enableMonitoring"));
        } catch (Exception e) {
            // Default to enabled if configuration is not available
            return true;
        }
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
                // Since we're using persistent transfers, we start with an empty session
                // Add all files to the session now
                logger.info("SESSION_START event received. Adding files to persistent transfer session");
                logger.debug("Total files to submit: {}", totalNumOfFiles);
                logger.debug("Files iterator has next: {}", filesToSubmitIter.hasNext());
                
                int count = 0;
                while (filesToSubmitIter.hasNext() && count < 20) { // Add up to 20 files
                    File file = filesToSubmitIter.next();
                    try {
                        faspManager.addSource(sessionStats.getId(), file.getAbsolutePath(), folder + File.separator + file.getName());
                        logger.info("Added file to transfer: {} (count: {})", file.getName(), count + 1);
                    } catch (FaspManagerException e) {
                        FaspManager.destroy();
                        String faspErrorMsg = "Failed to upload file via Aspera: " + file.getName() +
                            "\n\nThis could be due to network restrictions, firewall settings, or network instability." +
                            "\n\nAlternative options:" +
                            "\n1. Go back one step and select FTP upload instead" +
                            "\n2. Use Globus for file transfer: https://www.ebi.ac.uk/pride/markdownpage/globus" +
                            "\n3. Contact your system administrator to enable Aspera ports (TCP & UDP 33001)";
                        publish(new UploadErrorMessage(this, null, faspErrorMsg));
                        return; // Exit early on error
                    }
                    count++;
                }
                logger.info("Added {} files to transfer session in SESSION_START", count);
                
                // If no files were added, this could cause the transfer to hang
                if (count == 0) {
                    logger.error("No files were added to the transfer session! This will cause the transfer to hang.");
                    FaspManager.destroy();
                    String noFilesErrorMsg = "No files were added to the Aspera transfer session. This could be due to:" +
                        "\n• No files selected for upload" +
                        "\n• File iterator not properly initialized" +
                        "\n• All files were already processed" +
                        "\n\nPlease check your file selection and try again.";
                    publish(new UploadErrorMessage(this, null, noFilesErrorMsg));
                }
                break;
            case PROGRESS:
                // Update progress tracking
                lastProgressTime.set(System.currentTimeMillis());
                
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
                        if (sessionStats.getFilesComplete() == totalNumOfFiles) {
                            transferCompleted.set(true);
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
                transferCompleted.set(true);
                // Send final progress message with appropriate total size
                long finalTotalSize = totalFileSize > 0 ? totalFileSize : 100;
                publish(new UploadProgressMessage(this, null, finalTotalSize, finalTotalSize, totalNumOfFiles, totalNumOfFiles));
                publish(new UploadSuccessMessage(this));
                FaspManager.destroy();
                logger.info("Aspera Session Stop");
                break;
            case SESSION_ERROR:
                transferCompleted.set(true);
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
            default:
                logger.debug("Unhandled Aspera transfer event: {}", transferEvent);
                break;
        }
    }
    
    /**
     * Handle any uncaught exceptions in the background task
     */
    @Override
    protected void failed(Throwable cause) {
        logger.error("Aspera upload task failed with uncaught exception", cause);
        transferCompleted.set(true);
        
        // Create a comprehensive error message
        String errorMessage = "Aspera upload failed due to an unexpected error: " + cause.getMessage() +
            "\n\nThis could be due to:" +
            "\n• Network connectivity issues" +
            "\n• Firewall blocking Aspera ports (TCP & UDP 33001)" +
            "\n• Server-side problems" +
            "\n• Network instability" +
            "\n• System resource limitations" +
            "\n\nAlternative options:" +
            "\n1. Go back one step and select FTP upload instead" +
            "\n2. Use Globus for file transfer: https://www.ebi.ac.uk/pride/markdownpage/globus" +
            "\n3. Contact your system administrator to enable Aspera ports" +
            "\n4. Try again with a more stable network connection";
        
        // Publish the error message to be displayed to the user
        publish(new UploadErrorMessage(this, null, errorMessage));
        
        // Clean up resources
        try {
            FaspManager.destroy();
        } catch (Exception e) {
            logger.warn("Error during FaspManager cleanup", e);
        }
    }
}
