package uk.ac.ebi.pride.gui.aspera;

import com.asperasoft.faspmanager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.data.model.DataFile;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced Aspera file uploader with improved reliability and error handling
 *
 * @author Rui Wang
 * @version $Id$
 */
public class PersistedAsperaFileUploader {
    private static final Logger logger = LoggerFactory.getLogger(PersistedAsperaFileUploader.class);
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 5000;

    private final AtomicInteger retryCount = new AtomicInteger(0);
    private final File ascpExecutable;
    private TransferListener transferListener;
    private final SubmissionRecord submissionRecord;

    public PersistedAsperaFileUploader(File ascpExecutable, SubmissionRecord submissionRecord) {
        logger.info("Initializing PersistedAsperaFileUploader with executable: {}", ascpExecutable);
        this.ascpExecutable = ascpExecutable;
        this.submissionRecord = submissionRecord;
        Environment.setFasp2ScpPath(getAscpPath(ascpExecutable));
    }

    public String startTransferSession(RemoteLocation remoteLocation, XferParams transferParameters)
            throws InitializationException, ValidationException, LaunchException {
        logger.info("Starting transfer session with remote location: {}", remoteLocation);
        if (remoteLocation == null) {
            logger.error("Remote location is null");
            throw new IllegalStateException("Cannot upload without remote location being specified!");
        }

        // Configure transfer parameters for reliability
        configureTransferParameters(transferParameters);
        logger.debug("Transfer parameters configured: {}", transferParameters);

        // Disable persistence for more reliable transfers
        transferParameters.persist = false;
        logger.debug("Disabled persistence for reliable transfer");

        // Create LocalLocation with ALL files to ensure reliable transfer
        LocalLocation localFiles = new LocalLocation();
        
        // Add all files to the transfer order for reliable transfer
        if (submissionRecord != null && submissionRecord.getSubmission() != null && 
            !submissionRecord.getSubmission().getDataFiles().isEmpty()) {
            
            for (DataFile dataFile : submissionRecord.getSubmission().getDataFiles()) {
                File file = dataFile.getFile();
                if (file != null && file.exists()) {
                    localFiles.addPath(file.getAbsolutePath());
                    logger.debug("Added file to transfer order: {}", file.getName());
                } else {
                    logger.warn("Skipping invalid file: {}", file != null ? file.getAbsolutePath() : "null");
                }
            }
        }
        
        logger.debug("Created LocalLocation with all files for reliable transfer");

        TransferOrder order = new TransferOrder(localFiles, remoteLocation, transferParameters);
        logger.debug("Transfer order created for reliable transfer");

        // Attempt transfer with retry mechanism
        String sessionId = null;
        while (retryCount.get() < MAX_RETRIES) {
            try {
                logger.info("Attempting transfer (attempt {}/{})", retryCount.get() + 1, MAX_RETRIES);
                sessionId = FaspManager.getSingleton().startTransfer(order);
                logger.info("Transfer started successfully with session ID: {}", sessionId);
                logger.debug("Transfer order details - Remote: {}", remoteLocation.getHost());
                break;
            } catch (Exception e) {
                retryCount.incrementAndGet();
                logger.error("Transfer attempt {} failed: {}", retryCount.get(), e.getMessage(), e);
                if (retryCount.get() < MAX_RETRIES) {
                    try {
                        logger.info("Waiting {}ms before retry", RETRY_DELAY_MS);
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        logger.error("Transfer interrupted during retry", ie);
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Transfer interrupted during retry", ie);
                    }
                } else {
                    logger.error("All transfer attempts failed after {} retries", MAX_RETRIES, e);
                    throw new RuntimeException("Failed to start transfer after " + MAX_RETRIES + " attempts: " + e.getMessage(), e);
                }
            }
        }

        return sessionId;
    }

    private void configureTransferParameters(XferParams params) {
        logger.debug("Configuring transfer parameters");
        // Enable encryption for security
        params.encryption = Encryption.DEFAULT;
        logger.debug("Encryption set to: {}", params.encryption);

        // Configure for reliability
        params.resumeCheck = Resume.FILE_ATTRIBUTES;
        params.overwrite = Overwrite.DIFFERENT;
        params.policy = Policy.FAIR;
        logger.debug("Reliability settings - Resume: {}, Overwrite: {}, Policy: {}",
                params.resumeCheck, params.overwrite, params.policy);

        // Enable path creation
        params.createPath = true;
        logger.debug("Path creation enabled: {}", params.createPath);

        // Enable pre-calculation of job size
        params.preCalculateJobSize = true;
        logger.debug("Pre-calculation of job size enabled: {}", params.preCalculateJobSize);
    }

    private String getAscpPath(File executable) {
        logger.info("Getting ASCP executable path: {}", executable);
        if (executable == null || !executable.exists()) {
            logger.error("ASCP executable not found: {}", executable);
            throw new IllegalArgumentException("Specified ascp executable does not exist.");
        }
        String path = executable.getAbsolutePath();
        logger.debug("ASCP executable path resolved to: {}", path);
        return path;
    }

    public void addTransferListener(TransferListener transferListener) throws InitializationException {
        logger.info("Adding transfer listener: {}", transferListener);
        this.transferListener = transferListener;
        FaspManager.getSingleton().addListener(transferListener);
        logger.debug("Transfer listener added successfully");
    }

    public void removeTransferListener(TransferListener transferListener) throws InitializationException {
        logger.info("Removing transfer listener: {}", transferListener);
        if (this.transferListener != null) {
            FaspManager.getSingleton().removeListener(this.transferListener);
            this.transferListener = null;
            logger.debug("Transfer listener removed successfully");
        } else {
            logger.warn("No transfer listener to remove");
        }
    }

    public void validateTransfer(String sessionId) throws InitializationException {
        logger.info("Validating transfer for session: {}", sessionId);
        SessionStats stats;
        try {
            stats = FaspManager.getSingleton().getSessionStats(sessionId);
        } catch (SessionNotFoundException e) {
            logger.error("Session not found: {}", sessionId);
            throw new RuntimeException("Session not found: " + sessionId, e);
        }
        if (stats == null) {
            logger.error("Invalid session ID: {}", sessionId);
            throw new RuntimeException("Invalid session ID");
        }

        logger.debug("Session stats: state={}, error={}, files complete={}",
                stats.getState(), stats.getErrorDescription(), stats.getFilesComplete());

        if (stats.getState() == SessionState.FAILED) {
            logger.error("Transfer failed: {}", stats.getErrorDescription());
            throw new RuntimeException("Transfer failed: " + stats.getErrorDescription());
        }

        // Check if all files are complete
        int filesComplete = (int) stats.getFilesComplete();
        int totalFiles = submissionRecord.getSubmission().getDataFiles().size();
        if (filesComplete < totalFiles) {
            logger.error("Transfer incomplete: {}/{} files transferred",
                    filesComplete, totalFiles);
            throw new RuntimeException("Transfer incomplete: " +
                    filesComplete + " of " + totalFiles + " files transferred");
        }

        logger.info("Transfer validation successful for session: {}", sessionId);
    }
}
