package uk.ac.ebi.pride.gui.task;

import com.asperasoft.faspmanager.FaspManager;
import com.asperasoft.faspmanager.FaspManagerException;
import com.asperasoft.faspmanager.FileInfo;
import com.asperasoft.faspmanager.FileState;
import com.asperasoft.faspmanager.InitializationException;
import com.asperasoft.faspmanager.SessionStats;
import com.asperasoft.faspmanager.TransferEvent;
import com.asperasoft.faspmanager.TransferListener;
import com.asperasoft.faspmanager.XferParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.submission.DropBoxDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.gui.aspera.AsperaFileUploader;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.task.ftp.UploadErrorMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadProgressMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadSuccessMessage;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.toolsuite.gui.task.Task;

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
        AsperaFileUploader uploader = new AsperaFileUploader(executable);
        final UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        final DropBoxDetail dropBox = uploadDetail.getDropBox();
        
        // Set up the uploader with cleaned credentials
        String cleanUsername = dropBox.getUserName() != null ? dropBox.getUserName().trim() : null;
        String cleanPassword = dropBox.getPassword() != null ? dropBox.getPassword().trim() : null;
        logger.info("Cleaned credentials - Username: '{}', Password: '{}'", cleanUsername, cleanPassword != null ? "[PROVIDED]" : "[NULL]");
        uploader.setRemoteLocation(uploadDetail.getHost(), cleanUsername, cleanPassword);
        XferParams params = AsperaFileUploader.defaultTransferParams();
        params.createPath = true;
        uploader.setTransferParameters(params);
        uploader.setListener(this);
        
        // Log connection details
        logger.info("=== ASPERA CONNECTION DETAILS ===");
        logger.info("Host: {}", uploadDetail.getHost());
        logger.info("Username: {}", dropBox.getUserName());
        logger.info("Password: {}", dropBox.getPassword() != null ? "[PROVIDED]" : "[MISSING]");
        logger.info("Folder: {}", uploadDetail.getFolder());
        logger.info("Total files to upload: {}", filesToSubmit.size());
        logger.info("Files list:");
        int i = 1;
        for (File file : filesToSubmit) {
            logger.info("  {}. {} ({} bytes)", i++, file.getName(), file.length());
        }
        logger.info("================================");
        
        // Additional debugging for RemoteLocation
        logger.info("=== REMOTE LOCATION DEBUG ===");
        logger.info("RemoteLocation host: {}", uploader.getRemoteLocation());
        logger.info("=============================");
        
        
        
        // Upload files using the folder name
        final String folder = uploadDetail.getFolder();
        logger.info("Starting Aspera upload to folder: {}", folder);
        
        try {
            String transferId = uploader.uploadFiles(filesToSubmit, folder);
            logger.info("Transfer started with ID: {}", transferId);
        } catch (FaspManagerException e) {
            logger.error("Aspera upload failed: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during Aspera upload: {}", e.getMessage(), e);
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
        
        // Start transfer monitoring (if enabled)
        transferStartTime.set(System.currentTimeMillis());
        lastProgressTime.set(System.currentTimeMillis()); // Set initial progress time
        if (isMonitoringEnabled()) {
            startTransferMonitoring();
        } else {
            logger.debug("Aspera transfer monitoring disabled for better performance");
        }
        
        // Add a timeout to detect if transfer gets stuck after CONNECTING
        new Thread(() -> {
            try {
                Thread.sleep(15000); // Wait 15 seconds after CONNECTING
                if (!transferCompleted.get() && lastProgressTime.get() == transferStartTime.get()) {
                    logger.warn("Transfer appears to be stuck after CONNECTING event - no progress for 15 seconds");
                    logger.warn("Server: {} | Folder: {}", uploadDetail.getHost(), uploadDetail.getFolder());
                    logger.warn("This could indicate:");
                    logger.warn("• Server-side folder permission issues");
                    logger.warn("• Server processing delays");
                    logger.warn("• Network connectivity problems");
                    logger.warn("• Aspera server overload");
                    logger.warn("Consider trying FTP upload or Globus as alternatives");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
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
        logger.info("Received file session event: {} with stats: {}", transferEvent, sessionStats);
        if (transferCompleted.get()) {
            logger.debug("Transfer already completed, ignoring event: {}", transferEvent);
            return;
        }
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
            case CONNECTING:
                logger.info("=== ASPERA CONNECTION STATUS ===");
                logger.info("CONNECTING event received - attempting to connect to server");
                logger.info("Host: {}", uploadDetail.getHost());
                logger.info("Folder: {}", folder);
                logger.info("Session stats: {}", sessionStats);
                if (sessionStats != null) {
                    logger.info("Connection status - Files complete: {}, Total transferred: {} bytes", 
                        sessionStats.getFilesComplete(), sessionStats.getTotalTransferredBytes());
                }
                logger.info("===============================");
                
                // Update progress time to show we're making progress
                lastProgressTime.set(System.currentTimeMillis());
                break;
            case SESSION_START:
                // SESSION_START event received - transfer is ready (optional event)
                logger.info("=== ASPERA SESSION READY ===");
                logger.info("SESSION_START event received. Transfer is ready to begin");
                logger.info("Total files to submit: {}", totalNumOfFiles);
                logger.info("Session stats: {}", sessionStats);
                logger.info("===========================");
                
                // Update progress tracking
                lastProgressTime.set(System.currentTimeMillis());
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
                    logger.info("=== FILE TRANSFER COMPLETED ===");
                    logger.info("File: {} uploaded successfully", fileNameArray[fileNameArray.length - 1]);
                    logger.info("Progress: {}/{} files completed", sessionStats.getFilesComplete(), totalNumOfFiles);
                    logger.info("Total transferred: {} bytes", sessionStats.getTotalTransferredBytes());
                    logger.info("==============================");
                    
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
                    
                    // Check if all files are complete (no dynamic file addition needed)
                    if (sessionStats.getFilesComplete() == totalNumOfFiles) {
                        transferCompleted.set(true);
                        FaspManager.destroy();
                        // Send final progress message with appropriate total size
                        long finalTotalSize = totalFileSize > 0 ? totalFileSize : 100;
                        publish(new UploadProgressMessage(this, null, finalTotalSize, finalTotalSize, totalNumOfFiles, totalNumOfFiles));
                        publish(new UploadSuccessMessage(this));
                        logger.info("Aspera transfer completed successfully - all {} files transferred", totalNumOfFiles);
                    } else {
                        logger.debug("File completed. Progress: {}/{} files", 
                            sessionStats.getFilesComplete(), totalNumOfFiles);
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
                logger.error("Aspera session Error: {}", transferEvent.getDescription());
                logger.error("Session stats: {}", sessionStats);
                logger.error("File info: {}", fileInfo);
                
                // Get more detailed error information
                String errorDetails = "Session error occurred";
                if (sessionStats != null) {
                    errorDetails += " - Files complete: " + sessionStats.getFilesComplete() + 
                                  ", Total transferred: " + sessionStats.getTotalTransferredBytes() + " bytes";
                }
                if (fileInfo != null) {
                    errorDetails += " - File: " + fileInfo.getName();
                }
                logger.error("Error details: {}", errorDetails);
                
                FaspManager.destroy();
                String asperaErrorMsg = "Aspera transfer failed: " + transferEvent.getDescription() +
                    "\n\nError details: " + errorDetails +
                    "\n\nThis could be due to:" +
                    "\n• Network connectivity issues" +
                    "\n• Firewall blocking Aspera ports (TCP & UDP 33001)" +
                    "\n• Server-side problems" +
                    "\n• Network instability" +
                    "\n• File access permissions" +
                    "\n• Insufficient disk space" +
                    "\n\nAlternative options:" +
                    "\n1. Go back one step and select FTP upload instead" +
                    "\n2. Use Globus for file transfer: https://www.ebi.ac.uk/pride/markdownpage/globus" +
                    "\n3. Contact your system administrator to enable Aspera ports" +
                    "\n4. Try again with a more stable network connection";
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
