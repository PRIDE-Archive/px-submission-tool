package uk.ac.ebi.pride.pxsubmit.service;

import com.asperasoft.faspmanager.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.submission.model.submission.DropBoxDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.data.model.DataFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JavaFX Service for Aspera file uploads.
 *
 * Features:
 * - High-speed transfer using Aspera FASP protocol
 * - Progress reporting via JavaFX properties
 * - Timeout monitoring and automatic error recovery
 * - Event-driven transfer tracking
 */
public class AsperaUploadService extends Service<AsperaUploadService.UploadResult> {

    private static final Logger logger = LoggerFactory.getLogger(AsperaUploadService.class);

    // Configuration constants
    private static final long TRANSFER_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes
    private static final long PROGRESS_TIMEOUT_MS = 2 * 60 * 1000;  // 2 minutes
    private static final int TCP_PORT = 33001;
    private static final int UDP_PORT = 33001;
    private static final int TARGET_RATE_KBPS = 100000; // 100 Mbps
    private static final int MIN_RATE_KBPS = 100;

    // Input properties
    private final List<DataFile> dataFiles;
    private final UploadDetail uploadDetail;
    private final String ascpPath;

    // Progress properties (observable from UI)
    private final IntegerProperty totalFiles = new SimpleIntegerProperty(0);
    private final IntegerProperty uploadedFiles = new SimpleIntegerProperty(0);
    private final LongProperty totalBytes = new SimpleLongProperty(0);
    private final LongProperty uploadedBytes = new SimpleLongProperty(0);
    private final StringProperty currentFileName = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final DoubleProperty overallProgress = new SimpleDoubleProperty(0);

    // Status tracking
    private final ObservableList<String> uploadLog = FXCollections.observableArrayList();

    public AsperaUploadService(List<DataFile> dataFiles, UploadDetail uploadDetail, String ascpPath) {
        this.dataFiles = dataFiles;
        this.uploadDetail = uploadDetail;
        this.ascpPath = ascpPath;

        this.totalFiles.set(dataFiles.size());
        this.totalBytes.set(calculateTotalSize(dataFiles));
    }

    private long calculateTotalSize(List<DataFile> files) {
        return files.stream()
                .filter(f -> f.getFile() != null)
                .mapToLong(f -> f.getFile().length())
                .sum();
    }

    @Override
    protected Task<UploadResult> createTask() {
        return new AsperaUploadTask();
    }

    // Property accessors for UI binding
    public IntegerProperty totalFilesProperty() { return totalFiles; }
    public IntegerProperty uploadedFilesProperty() { return uploadedFiles; }
    public LongProperty totalBytesProperty() { return totalBytes; }
    public LongProperty uploadedBytesProperty() { return uploadedBytes; }
    public StringProperty currentFileNameProperty() { return currentFileName; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public DoubleProperty overallProgressProperty() { return overallProgress; }
    public ObservableList<String> getUploadLog() { return uploadLog; }

    /**
     * Main upload task using Aspera FASP
     */
    private class AsperaUploadTask extends Task<UploadResult> implements TransferListener {

        private final CountDownLatch transferComplete = new CountDownLatch(1);
        private final AtomicBoolean transferSuccess = new AtomicBoolean(false);
        private final AtomicBoolean transferTimedOut = new AtomicBoolean(false);
        private final AtomicLong lastProgressTime = new AtomicLong(System.currentTimeMillis());
        private final AtomicLong transferStartTime = new AtomicLong(0);

        private volatile String errorMessage = null;
        private int finishedFileCount = 0;

        @Override
        protected UploadResult call() throws Exception {
            log("Starting Aspera upload of " + dataFiles.size() + " files");
            log("Target: " + uploadDetail.getHost());

            try {
                // Initialize Aspera
                initializeAspera();

                // Convert DataFiles to Files
                List<File> filesToUpload = prepareFiles();

                // Start transfer
                startTransfer(filesToUpload);

                // Start monitoring thread
                startMonitoring();

                // Wait for transfer to complete
                boolean completed = transferComplete.await(TRANSFER_TIMEOUT_MS + 60000, TimeUnit.MILLISECONDS);

                if (!completed || transferTimedOut.get()) {
                    log("Transfer timed out");
                    return new UploadResult(false, finishedFileCount, dataFiles.size() - finishedFileCount,
                            "Transfer timed out");
                }

                if (transferSuccess.get()) {
                    log("Aspera upload completed successfully!");
                    return new UploadResult(true, dataFiles.size(), 0, null);
                } else {
                    log("Aspera upload failed: " + errorMessage);
                    return new UploadResult(false, finishedFileCount, dataFiles.size() - finishedFileCount,
                            errorMessage);
                }

            } catch (Exception e) {
                logger.error("Aspera upload failed", e);
                log("Error: " + e.getMessage());
                return new UploadResult(false, finishedFileCount, dataFiles.size() - finishedFileCount,
                        e.getMessage());
            }
        }

        private void initializeAspera() throws FaspManagerException, IOException {
            log("Initializing Aspera...");
            updateStatus("Initializing Aspera transfer manager...");

            File ascpFile = new File(ascpPath);
            if (!ascpFile.exists()) {
                throw new IOException("Aspera binary not found: " + ascpPath);
            }

            Environment.setFasp2ScpPath(ascpPath);
            FaspManager faspManager = FaspManager.getSingleton();
            faspManager.addListener(this);

            log("Aspera initialized successfully");
        }

        private List<File> prepareFiles() {
            List<File> files = new ArrayList<>();
            for (DataFile dataFile : dataFiles) {
                if (dataFile.getFile() != null && dataFile.getFile().exists()) {
                    files.add(dataFile.getFile());
                }
            }
            log("Prepared " + files.size() + " files for upload");
            return files;
        }

        private void startTransfer(List<File> files) throws FaspManagerException {
            DropBoxDetail dropBox = uploadDetail.getDropBox();

            // Set up local files
            LocalLocation localFiles = new LocalLocation();
            for (File file : files) {
                localFiles.addPath(file.getAbsolutePath());
                log("Adding file: " + file.getName() + " (" + formatSize(file.length()) + ")");
            }

            // Set up remote location
            String username = dropBox.getUserName() != null ? dropBox.getUserName().trim() : null;
            String password = dropBox.getPassword() != null ? dropBox.getPassword().trim() : null;

            RemoteLocation remoteLocation = new RemoteLocation(uploadDetail.getHost(), username, password);
            remoteLocation.addPath(uploadDetail.getFolder());

            // Set up transfer parameters
            XferParams params = createTransferParams();

            // Create transfer order
            TransferOrder order = new TransferOrder(localFiles, remoteLocation, params);

            // Start transfer
            log("Starting Aspera transfer to: " + uploadDetail.getFolder());
            updateStatus("Connecting to server...");
            transferStartTime.set(System.currentTimeMillis());
            lastProgressTime.set(System.currentTimeMillis());

            String transferId = FaspManager.getSingleton().startTransfer(order);
            log("Transfer started with ID: " + transferId);
        }

        private XferParams createTransferParams() {
            XferParams params = new XferParams();
            params.tcpPort = TCP_PORT;
            params.udpPort = UDP_PORT;
            params.targetRateKbps = TARGET_RATE_KBPS;
            params.minimumRateKbps = MIN_RATE_KBPS;
            params.encryption = Encryption.DEFAULT;
            params.overwrite = Overwrite.DIFFERENT;
            params.generateManifest = Manifest.NONE;
            params.policy = Policy.FAIR;
            params.resumeCheck = Resume.FILE_ATTRIBUTES;
            params.preCalculateJobSize = false;
            params.createPath = true;
            return params;
        }

        private void startMonitoring() {
            Thread monitorThread = new Thread(() -> {
                try {
                    while (!transferComplete.await(60, TimeUnit.SECONDS)) {
                        long now = System.currentTimeMillis();
                        long sinceStart = now - transferStartTime.get();
                        long sinceProgress = now - lastProgressTime.get();

                        if (sinceStart > TRANSFER_TIMEOUT_MS) {
                            logger.error("Transfer timeout after {}ms", sinceStart);
                            transferTimedOut.set(true);
                            errorMessage = "Transfer timed out after " + (sinceStart / 60000) + " minutes";
                            transferComplete.countDown();
                            break;
                        }

                        if (sinceProgress > PROGRESS_TIMEOUT_MS) {
                            logger.error("No progress for {}ms", sinceProgress);
                            transferTimedOut.set(true);
                            errorMessage = "Transfer stuck - no progress for " + (sinceProgress / 60000) + " minutes";
                            transferComplete.countDown();
                            break;
                        }

                        logger.debug("Monitoring: {}s elapsed, {}s since last progress",
                                sinceStart / 1000, sinceProgress / 1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            monitorThread.setDaemon(true);
            monitorThread.setName("Aspera-Monitor");
            monitorThread.start();
        }

        // TransferListener implementation
        @Override
        public void fileSessionEvent(TransferEvent event, SessionStats stats, FileInfo fileInfo) {
            logger.debug("Aspera event: {} stats={}", event, stats);

            switch (event) {
                case CONNECTING:
                    log("Connecting to server...");
                    updateStatus("Connecting...");
                    lastProgressTime.set(System.currentTimeMillis());
                    break;

                case SESSION_START:
                    log("Transfer session started");
                    updateStatus("Transfer in progress...");
                    lastProgressTime.set(System.currentTimeMillis());
                    break;

                case PROGRESS:
                    lastProgressTime.set(System.currentTimeMillis());
                    if (stats != null) {
                        int completed = (int) stats.getFilesComplete();
                        long transferred = stats.getTotalTransferredBytes();

                        Platform.runLater(() -> {
                            uploadedFiles.set(completed);
                            uploadedBytes.set(transferred);

                            double progress = totalFiles.get() > 0 ?
                                    (double) completed / totalFiles.get() : 0;
                            overallProgress.set(progress);
                        });

                        updateStatus(String.format("Uploading: %d/%d files (%.1f%%)",
                                completed, totalFiles.get(), (double) completed / totalFiles.get() * 100));
                    }
                    break;

                case FILE_STOP:
                    if (fileInfo != null && fileInfo.getState() == FileState.FINISHED) {
                        finishedFileCount++;
                        String fileName = extractFileName(fileInfo.getName());
                        log("Completed: " + fileName);

                        Platform.runLater(() -> {
                            uploadedFiles.set(finishedFileCount);
                            currentFileName.set(fileName);

                            double progress = (double) finishedFileCount / totalFiles.get();
                            overallProgress.set(progress);
                        });

                        // Check if all files complete
                        if (finishedFileCount >= dataFiles.size()) {
                            transferSuccess.set(true);
                            transferComplete.countDown();
                        }
                    }
                    break;

                case SESSION_STOP:
                    log("Transfer session completed");
                    transferSuccess.set(true);
                    transferComplete.countDown();
                    break;

                case FILE_ERROR:
                    if (fileInfo != null) {
                        String errorFile = extractFileName(fileInfo.getName());
                        String error = fileInfo.getErrDescription();
                        log("File error: " + errorFile + " - " + error);
                        errorMessage = "Failed to upload " + errorFile + ": " + error;
                    }
                    break;

                case SESSION_ERROR:
                    log("Session error: " + event.getDescription());
                    errorMessage = "Aspera session error: " + event.getDescription();
                    transferComplete.countDown();
                    break;

                default:
                    logger.debug("Unhandled event: {}", event);
            }
        }

        private String extractFileName(String path) {
            if (path == null) return "unknown";
            String[] parts = path.split("[/\\\\]");
            return parts[parts.length - 1];
        }

        private void updateStatus(String status) {
            Platform.runLater(() -> statusMessage.set(status));
            updateMessage(status);
        }

        private void log(String message) {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            String logMessage = "[" + timestamp + "] " + message;
            Platform.runLater(() -> uploadLog.add(logMessage));
            logger.info(message);
        }

        private String formatSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024L * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Upload result
     */
    public static class UploadResult {
        private final boolean success;
        private final int successCount;
        private final int failureCount;
        private final String errorMessage;

        public UploadResult(boolean success, int successCount, int failureCount, String errorMessage) {
            this.success = success;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Check if Aspera is available on this system
     */
    public static boolean isAsperaAvailable(String ascpPath) {
        if (ascpPath == null || ascpPath.isEmpty()) {
            return false;
        }
        File ascpFile = new File(ascpPath);
        return ascpFile.exists() && ascpFile.canExecute();
    }

    /**
     * Find Aspera binary path
     */
    public static String findAscpPath() {
        // Check common locations
        String[] commonPaths = {
                System.getProperty("user.home") + "/Applications/Aspera Connect.app/Contents/Resources/ascp",
                "/Applications/Aspera Connect.app/Contents/Resources/ascp",
                System.getenv("ASPERA_SCP_PATH"),
                System.getProperty("user.home") + "/.aspera/connect/bin/ascp",
                "/usr/local/bin/ascp"
        };

        for (String path : commonPaths) {
            if (path != null && isAsperaAvailable(path)) {
                return path;
            }
        }

        return null;
    }
}
