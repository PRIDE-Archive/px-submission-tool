package uk.ac.ebi.pride.pxsubmit.service;

import com.asperasoft.faspmanager.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import uk.ac.ebi.pride.archive.submission.model.submission.DropBoxDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.config.AppConfig;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics.FileTransferStat;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
public class AsperaUploadService extends AbstractUploadService {

    // Configuration constants
    private static final long TRANSFER_TIMEOUT_MS = 60 * 60 * 1000; // 60 minutes
    private static final long PROGRESS_TIMEOUT_MS = 30 * 60 * 1000;  // 30 minutes
    private static final int MAX_RETRIES = 3;
    private static final int TCP_PORT = 33001;
    private static final int UDP_PORT = 33001;
    private static final int TARGET_RATE_KBPS = 100000; // 100 Mbps
    private static final int MIN_RATE_KBPS = 100;

    // Aspera-specific properties
    private final String ascpPath;
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final DoubleProperty overallProgress = new SimpleDoubleProperty(0);
    private final Map<String, FileTransferStat> activeFileStats = new ConcurrentHashMap<>();

    // Pause/resume support
    private volatile String currentTransferId;
    private volatile boolean paused = false;

    public AsperaUploadService(List<DataFile> files, UploadDetail uploadDetail, String ascpPath) {
        super(files, uploadDetail);
        this.ascpPath = ascpPath;
    }

    @Override
    protected Task<UploadResult> createTask() {
        return new AsperaUploadTask();
    }

    // Aspera-specific property accessors
    public StringProperty statusMessageProperty() { return statusMessage; }
    public DoubleProperty overallProgressProperty() { return overallProgress; }

    /**
     * Pause the Aspera transfer by setting the target rate to 0.
     */
    public void pause() {
        if (currentTransferId != null && !paused) {
            try {
                FaspManager.getSingleton().setRate(currentTransferId, 0, 0, Policy.FIXED);
                paused = true;
                logger.info("Aspera transfer paused");
            } catch (Exception e) {
                logger.warn("Failed to pause Aspera transfer: {}", e.getMessage());
            }
        }
    }

    /**
     * Resume a paused Aspera transfer by restoring the target rate.
     */
    public void resume() {
        if (currentTransferId != null && paused) {
            try {
                FaspManager.getSingleton().setRate(currentTransferId, TARGET_RATE_KBPS, MIN_RATE_KBPS, Policy.FAIR);
                paused = false;
                logger.info("Aspera transfer resumed");
            } catch (Exception e) {
                logger.warn("Failed to resume Aspera transfer: {}", e.getMessage());
            }
        }
    }

    public boolean isPaused() {
        return paused;
    }

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
            // Validate upload details
            if (uploadDetail == null) {
                throw new IllegalStateException("Upload detail is null");
            }
            if (uploadDetail.getHost() == null || uploadDetail.getHost().isEmpty()) {
                throw new IllegalStateException("Aspera host is not configured");
            }
            if (uploadDetail.getDropBox() == null) {
                throw new IllegalStateException("DropBox credentials are not configured");
            }
            if (uploadDetail.getFolder() == null || uploadDetail.getFolder().isEmpty()) {
                throw new IllegalStateException("Upload folder is not configured");
            }

            AsperaUploadService.this.log("Starting Aspera upload of " + files.size() + " files");
            AsperaUploadService.this.log("Target: " + uploadDetail.getHost());
            AsperaUploadService.this.log("Folder: " + uploadDetail.getFolder());
            AsperaUploadService.this.log("Username: " + (uploadDetail.getDropBox().getUserName() != null ?
                uploadDetail.getDropBox().getUserName() : "(anonymous)"));

            // Initialize transfer statistics
            transferStatistics = new TransferStatistics("ASPERA", files.size(), totalBytesProperty().get());
            StatisticsLogger.logSessionStart(transferStatistics);

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

                UploadResult result;
                if (!completed || transferTimedOut.get()) {
                    AsperaUploadService.this.log("Transfer timed out");
                    result = UploadResult.asperaResult(false, finishedFileCount, files.size() - finishedFileCount,
                            "Transfer timed out");
                } else if (transferSuccess.get()) {
                    AsperaUploadService.this.log("Aspera upload completed successfully!");
                    result = UploadResult.asperaResult(true, files.size(), 0, null);
                } else {
                    AsperaUploadService.this.log("Aspera upload failed: " + errorMessage);
                    result = UploadResult.asperaResult(false, finishedFileCount, files.size() - finishedFileCount,
                            errorMessage);
                }

                // Complete statistics and log summary
                if (transferStatistics != null) {
                    transferStatistics.setSessionComplete();
                    StatisticsLogger.logSessionSummary(transferStatistics);
                }

                return result;

            } catch (Exception e) {
                logger.error("Aspera upload failed", e);
                AsperaUploadService.this.log("Error: " + e.getMessage());

                // Complete statistics and log summary
                if (transferStatistics != null) {
                    transferStatistics.setSessionComplete();
                    StatisticsLogger.logSessionSummary(transferStatistics);
                }

                return UploadResult.asperaResult(false, finishedFileCount, files.size() - finishedFileCount,
                        e.getMessage());
            }
        }

        private void initializeAspera() throws FaspManagerException, IOException {
            AsperaUploadService.this.log("Initializing Aspera...");
            updateStatus("Initializing Aspera transfer manager...");

            // Validate ascp path
            if (ascpPath == null || ascpPath.isEmpty()) {
                throw new IOException("Aspera binary path is not configured");
            }

            File ascpFile = new File(ascpPath);
            if (!ascpFile.exists()) {
                throw new IOException("Aspera binary not found at: " + ascpPath);
            }
            if (!ascpFile.canExecute()) {
                throw new IOException("Aspera binary is not executable: " + ascpPath);
            }

            logger.info("Using Aspera binary: {}", ascpPath);
            Environment.setFasp2ScpPath(ascpPath);

            FaspManager faspManager = FaspManager.getSingleton();
            faspManager.addListener(this);

            AsperaUploadService.this.log("Aspera initialized successfully");
        }

        private List<File> prepareFiles() {
            List<File> preparedFiles = new ArrayList<>();
            for (DataFile dataFile : files) {
                if (dataFile.getFile() != null && dataFile.getFile().exists()) {
                    File file = dataFile.getFile();
                    preparedFiles.add(file);

                    // Start tracking this file in statistics
                    if (transferStatistics != null) {
                        FileTransferStat fileStat = transferStatistics.startFile(file.getName(), file.length());
                        activeFileStats.put(file.getName(), fileStat);
                        StatisticsLogger.logFileStart(fileStat);
                    }
                }
            }
            AsperaUploadService.this.log("Prepared " + preparedFiles.size() + " files for upload");
            return preparedFiles;
        }

        private void startTransfer(List<File> transferFiles) throws IOException, FaspManagerException {
            if (transferFiles.isEmpty()) {
                throw new IOException("No files to transfer");
            }

            DropBoxDetail dropBox = uploadDetail.getDropBox();

            LocalLocation localFiles = new LocalLocation();
            for (File file : transferFiles) {
                if (!file.exists() || !file.canRead()) continue;
                localFiles.addPath(file.getAbsolutePath());
            }

            String username = dropBox.getUserName();
            String password = dropBox.getPassword();

            RemoteLocation remoteLocation = new RemoteLocation(uploadDetail.getHost(), username, password);
            remoteLocation.addPath(uploadDetail.getFolder());

            XferParams params = createTransferParams();
            TransferOrder order = new TransferOrder(localFiles, remoteLocation, params);

            int attempt = 0;
            while (attempt < MAX_RETRIES) {
                try {
                    String transferId = FaspManager.getSingleton().startTransfer(order);
                    currentTransferId = transferId;
                    log("Transfer started with ID: " + transferId);
                    return;
                } catch (Exception e) {
                    attempt++;
                    log("Transfer attempt failed (" + attempt + "): " + e.getMessage());
                    if (attempt >= MAX_RETRIES) {
                        throw e;
                    }
                    try {
                        Thread.sleep(10000); // wait 10s before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
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

            // FIX 1: Use stable resume
            params.resumeCheck = Resume.FILE_ATTRIBUTES;

            // FIX 2: Enable pre-calculation (important for large files)
            params.preCalculateJobSize = true;

            params.createPath = true;

            return params;
        }

        private void startMonitoring() {
            Thread monitorThread = new Thread(() -> {
                try {
                    while (!transferComplete.await(60, TimeUnit.SECONDS)) {
                        long now = System.currentTimeMillis();

                        long startTime = transferStartTime.get();
                        long lastProgress = lastProgressTime.get();

                        if (startTime <= 0 || lastProgress <= 0) {
                            continue;
                        }

                        long sinceStart = now - startTime;
                        long sinceProgress = now - lastProgress;

                        // Only logging — NEVER kill transfer
                        if (sinceProgress > 15 * 60 * 1000) { // 15 min
                            logger.warn("Slow transfer: no progress for {} min", sinceProgress / 60000);
                        }

                        if (sinceStart > 6 * 60 * 60 * 1000) { // 6 hours
                            logger.info("Long running transfer: {} hours", sinceStart / (1000 * 60 * 60));
                        }
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
            long now = System.currentTimeMillis();

            switch (event) {
                case CONNECTING:
                    AsperaUploadService.this.log("Connecting to server...");
                    updateStatus("Connecting...");
                    lastProgressTime.set(System.currentTimeMillis());
                    break;

                case SESSION_START:
                    AsperaUploadService.this.log("Transfer session started");
                    updateStatus("Transfer in progress...");
                    lastProgressTime.set(System.currentTimeMillis());
                    break;

                case PROGRESS:
                    lastProgressTime.set(System.currentTimeMillis());
                    if (stats != null) {
                        int completed = (int) stats.getFilesComplete();
                        long transferred = stats.getTotalTransferredBytes();

                        // Estimate per-file progress from byte data
                        long completedFilesBytes = 0;
                        for (int i = 0; i < Math.min(completed, files.size()); i++) {
                            if (files.get(i).getFile() != null) {
                                completedFilesBytes += files.get(i).getFile().length();
                            }
                        }
                        long currentFileTransferred = transferred - completedFilesBytes;
                        long currentFileSize = (completed < files.size() && files.get(completed).getFile() != null)
                                ? files.get(completed).getFile().length() : 0;
                        double fileProg = (currentFileSize > 0)
                                ? Math.min(1.0, (double) currentFileTransferred / currentFileSize) : 0;
                        double finalFileProg = fileProg;

                        Platform.runLater(() -> {
                            uploadedFilesProperty().set(completed);
                            uploadedBytesProperty().set(transferred);
                            currentFileProgressProperty().set(finalFileProg);

                            double progress = totalFilesProperty().get() > 0 ?
                                    (double) completed / totalFilesProperty().get() : 0;
                            overallProgress.set(progress);
                        });

                        updateStatus(String.format("Uploading: %d/%d files (%.1f%%)",
                                completed, totalFilesProperty().get(), (double) completed / totalFilesProperty().get() * 100));
                    }
                    break;

                case FILE_STOP:
                    if (fileInfo != null && fileInfo.getState() == FileState.FINISHED) {
                        finishedFileCount++;
                        logger.info("Completed file: {}", fileInfo.getName());

                        if (finishedFileCount >= files.size()) {
                            transferSuccess.set(true);
                            transferComplete.countDown();
                            String fileName = extractFileName(fileInfo.getName());
                            AsperaUploadService.this.log("Completed: " + fileName);

                            // Track file completion in statistics
                            FileTransferStat fileStat = activeFileStats.get(fileName);
                            if (transferStatistics != null && fileStat != null) {
                                long fileSize = fileStat.getFileSize();
                                transferStatistics.completeFile(fileName, fileSize);
                                StatisticsLogger.logFileComplete(fileStat);
                                activeFileStats.remove(fileName);
                            }

                            Platform.runLater(() -> {
                                uploadedFilesProperty().set(finishedFileCount);
                                currentFileNameProperty().set(fileName);
                                double progress = (double) finishedFileCount / totalFilesProperty().get();
                                overallProgress.set(progress);
                            });           }
                    }
                    break;

                case SESSION_STOP:
                    AsperaUploadService.this.log("Transfer session completed");

                    // Log session stop event
                    if (transferStatistics != null) {
                        StatisticsLogger.logEvent(
                                transferStatistics.getSessionId(),
                                "SESSION_STOP",
                                "Transfer session completed normally"
                        );
                    }

                    transferSuccess.set(true);
                    transferComplete.countDown();
                    break;

                case FILE_ERROR:
                    if (fileInfo != null) {
                        String errorFile = extractFileName(fileInfo.getName());
                        String error = fileInfo.getErrDescription();
                        AsperaUploadService.this.log("File error: " + errorFile + " - " + error);
                        errorMessage = "Failed to upload " + errorFile + ": " + error;

                        // Track file failure in statistics
                        FileTransferStat failedFileStat = activeFileStats.get(errorFile);
                        if (transferStatistics != null) {
                            transferStatistics.failFile(errorFile, error, 0);
                            if (failedFileStat != null) {
                                StatisticsLogger.logFileComplete(failedFileStat);
                                activeFileStats.remove(errorFile);
                            }
                        }
                    }
                    break;

                case SESSION_ERROR:
                    AsperaUploadService.this.log("Session error: " + event.getDescription());
                    errorMessage = "Aspera session error: " + event.getDescription();

                    // Log session error event
                    if (transferStatistics != null) {
                        StatisticsLogger.logEvent(
                                transferStatistics.getSessionId(),
                                "SESSION_ERROR",
                                event.getDescription()
                        );
                    }

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

        private String formatSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024L * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
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
        String ascpLocation = resolveConfiguredAscpLocation();
        if (ascpLocation == null || ascpLocation.isEmpty()) {
//            logger.error("No Aspera binary configured for current platform");
            return null;
        }

        String ascpPath = getApplicationBasePath() + File.separator + ascpLocation;
        if (isAsperaAvailable(ascpPath)) {
            return ascpPath;
        }

//        logger.error("Configured Aspera binary not found or not executable: {}", ascpPath);
        return null;
    }

    private static String resolveConfiguredAscpLocation() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        String configKey;
        if (osName.contains("mac")) {
            configKey = "aspera.client.mac.binary";
        } else if (osName.contains("win")) {
            configKey = arch.contains("64") ? "aspera.client.windows64.binary" : "aspera.client.windows32.binary";
        } else if (osName.contains("nux") || osName.contains("nix")) {
            configKey = arch.contains("64") ? "aspera.client.linux64.binary" : "aspera.client.linux32.binary";
        } else {
//            logger.error("Unsupported platform detected: {} arch: {}", osName, arch);
            return null;
        }

        return AppConfig.getInstance().getProperty(configKey);
    }

    private static String getApplicationBasePath() {
        try {
            File location = new File(AsperaUploadService.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI());
            File baseDir = location.isFile() ? location.getParentFile() : location;
            if (baseDir == null) {
                return new File(".").getAbsolutePath();
            }

            // In local dev runs classes are typically under target/classes or target/test-classes.
            // Move up to the project root so bundled "aspera/..." resolves correctly.
            String path = baseDir.getAbsolutePath().replace('\\', '/');
            if (path.endsWith("/target/classes") || path.endsWith("/target/test-classes")) {
                File projectRoot = baseDir.getParentFile() != null ? baseDir.getParentFile().getParentFile() : null;
                if (projectRoot != null) {
                    return projectRoot.getAbsolutePath();
                }
            }

            return baseDir.getAbsolutePath();
        } catch (Exception e) {
//            logger.warn("Failed to resolve application base path, using working directory", e);
            return new File(".").getAbsolutePath();
        }
    }
}
