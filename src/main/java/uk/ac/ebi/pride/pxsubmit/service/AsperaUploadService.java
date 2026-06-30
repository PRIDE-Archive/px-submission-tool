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
    private static final long PROGRESS_TIMEOUT_MS = 30 * 60 * 1000;  // 30 minutes (stall detection)
    private static final int MAX_RETRIES = 3;
    private static final int TCP_PORT = 33001;
    private static final int UDP_PORT = 33001;
    private static final int TARGET_RATE_KBPS = 100000; // 100 Mbps
    private static final int MIN_RATE_KBPS = 100;

    // Maximum number of parallel Aspera sessions. Files are split roughly evenly
    // across this many sessions to speed up many-file uploads while staying well
    // below the connection count that previously exhausted client resources.
    private static final int MAX_PARALLEL_SESSIONS = 6;

    // Aspera-specific properties
    private final String ascpPath;
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final DoubleProperty overallProgress = new SimpleDoubleProperty(0);
    private final Map<String, FileTransferStat> activeFileStats = new ConcurrentHashMap<>();

    // Pause/resume support (tracks active parallel session transfer IDs)
    private final java.util.Set<String> activeTransferIds = ConcurrentHashMap.newKeySet();
    private final Object pauseLock = new Object();
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
     * Pause the Aspera transfer by stopping active sessions. Resume starts new
     * sessions for unfinished files; Aspera's resume mode continues partial files.
     */
    public void pause() {
        synchronized (pauseLock) {
            if (paused) {
                return;
            }
            paused = true;
        }
        Platform.runLater(() -> statusMessage.set("Upload paused"));
        Thread.startVirtualThread(this::stopActiveTransfersForPause);
        logger.info("Aspera transfer pause requested");
    }

    /**
     * Resume a paused Aspera transfer. The running task will start new sessions
     * for files that were not completed before pause.
     */
    public void resume() {
        synchronized (pauseLock) {
            if (!paused) {
                return;
            }
            paused = false;
            pauseLock.notifyAll();
        }
        Platform.runLater(() -> statusMessage.set("Transfer in progress..."));
        logger.info("Aspera transfer resume requested");
    }

    public boolean isPaused() {
        return paused;
    }

    private void applyCurrentPauseState(String transferId) {
        if (paused) {
            stopTransferForPause(transferId);
        }
    }

    private void stopActiveTransfersForPause() {
        for (String transferId : activeTransferIds) {
            stopTransferForPause(transferId);
        }
    }

    private void stopTransferForPause(String transferId) {
        if (transferId == null || transferId.isBlank()) {
            return;
        }

        try {
            FaspManager.getSingleton().stopTransfer(transferId);
            logger.info("Stopped Aspera transfer {} for pause", transferId);
        } catch (Exception stopException) {
            logger.warn("Failed to stop Aspera transfer {} for pause: {}", transferId, stopException.getMessage());
            try {
                FaspManager.getSingleton().cancelTransfer(transferId);
                logger.info("Cancelled Aspera transfer {} for pause", transferId);
            } catch (Exception cancelException) {
                logger.warn("Failed to cancel Aspera transfer {} for pause: {}",
                    transferId, cancelException.getMessage());
            }
        }
    }

    /**
     * Main upload task using Aspera FASP
     */
    private class AsperaUploadTask extends Task<UploadResult> implements TransferListener {

        private final AtomicBoolean transferTimedOut = new AtomicBoolean(false);
        private final AtomicLong lastProgressTime = new AtomicLong(System.currentTimeMillis());
        private final AtomicLong transferStartTime = new AtomicLong(0);

        // Aggregated counters across all parallel sessions
        private final java.util.concurrent.atomic.AtomicInteger finishedFileCount =
                new java.util.concurrent.atomic.AtomicInteger(0);
        private final java.util.concurrent.atomic.AtomicInteger failedFileCount =
                new java.util.concurrent.atomic.AtomicInteger(0);
        // Latest cumulative bytes transferred per session id (summed for overall progress)
        private final Map<String, Long> sessionTransferredBytes = new ConcurrentHashMap<>();

        private volatile String errorMessage = null;
        private volatile CountDownLatch sessionCompleteLatch;
        private volatile int totalFilesToTransfer = 0;

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
                totalFilesToTransfer = filesToUpload.size();

                if (filesToUpload.isEmpty()) {
                    return UploadResult.asperaResult(false, 0, files.size(), "No readable files to upload");
                }

                if (isCancelled()) {
                    return UploadResult.asperaResult(false, 0, filesToUpload.size(), "Transfer cancelled");
                }

                // Transfer files using several parallel Aspera sessions (a few ascp
                // processes, each one SSH control connection) to speed up many-file
                // uploads while staying well below the connection count that
                // previously exhausted client resources.
                boolean completedNormally = transferInParallel(filesToUpload);

                int done = finishedFileCount.get();
                int failed = failedFileCount.get();

                UploadResult result;
                if (transferTimedOut.get()) {
                    AsperaUploadService.this.log("Transfer timed out");
                    result = UploadResult.asperaResult(false, done,
                            totalFilesToTransfer - done, "Transfer timed out");
                } else if (completedNormally && failed == 0 && errorMessage == null) {
                    // Success is based on the absence of failures/errors rather than an
                    // exact completed-count match, because files already present on the
                    // server are reported as SKIPPED (still a success for our purposes).
                    AsperaUploadService.this.log("Aspera upload completed successfully!");
                    result = UploadResult.asperaResult(true, files.size(), 0, null);
                } else {
                    String msg = errorMessage != null ? errorMessage
                            : (failed + " file(s) failed to upload");
                    AsperaUploadService.this.log("Aspera upload failed: " + msg);
                    result = UploadResult.asperaResult(false, done,
                            totalFilesToTransfer - done, msg);
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

                return UploadResult.asperaResult(false, finishedFileCount.get(),
                        files.size() - finishedFileCount.get(), e.getMessage());
            } finally {
                releaseAllSessions();
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

            FaspManager.getSingleton();

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

        /**
         * Split files into up to {@link #MAX_PARALLEL_SESSIONS} groups and transfer
         * each group in its own Aspera session concurrently, then wait for all to finish.
         *
         * @return true if all sessions ended on their own (SESSION_STOP/SESSION_ERROR),
         *         false if cancelled or stalled/timed out.
         */
        private boolean transferInParallel(List<File> filesToUpload)
                throws IOException, FaspManagerException, InterruptedException {
            transferTimedOut.set(false);
            transferStartTime.set(0);
            lastProgressTime.set(System.currentTimeMillis());
            errorMessage = null;
            finishedFileCount.set(0);
            failedFileCount.set(0);

            Platform.runLater(() -> {
                currentFileNameProperty().set("");
                currentFileProgressProperty().set(0);
            });

            List<File> remainingFiles = new ArrayList<>(filesToUpload);
            while (!remainingFiles.isEmpty()) {
                waitWhilePaused();
                if (isCancelled()) {
                    errorMessage = "Transfer cancelled";
                    return false;
                }

                errorMessage = null;
                sessionTransferredBytes.clear();
                List<List<File>> groups = partition(remainingFiles, MAX_PARALLEL_SESSIONS);
                sessionCompleteLatch = new CountDownLatch(groups.size());

                AsperaUploadService.this.log("Uploading " + remainingFiles.size() + " remaining file(s) across "
                        + groups.size() + " parallel session(s)");
                updateStatus("Uploading " + remainingFiles.size() + " remaining file(s)...");

                for (List<File> group : groups) {
                    startSession(group);
                }
                startMonitoring(sessionCompleteLatch, "sessions");

                boolean sessionsFinished = waitForCompletion();
                remainingFiles = remainingFiles(filesToUpload);

                if (isCancelled()) {
                    errorMessage = "Transfer cancelled";
                    return false;
                }
                if (transferTimedOut.get()) {
                    return false;
                }
                if (paused) {
                    AsperaUploadService.this.log("Aspera upload paused. Waiting to resume...");
                    continue;
                }
                if (!sessionsFinished) {
                    return false;
                }
                if (errorMessage != null) {
                    return false;
                }
                if (!remainingFiles.isEmpty()) {
                    errorMessage = "Aspera session ended before all files were uploaded";
                    return false;
                }
            }

            return true;
        }

        /**
         * Distribute files round-robin into at most {@code maxGroups} groups so the
         * counts are as even as possible.
         */
        private List<List<File>> partition(List<File> filesToUpload, int maxGroups) {
            int groupCount = Math.min(maxGroups, filesToUpload.size());
            List<List<File>> groups = new ArrayList<>();
            for (int i = 0; i < groupCount; i++) {
                groups.add(new ArrayList<>());
            }
            for (int i = 0; i < filesToUpload.size(); i++) {
                groups.get(i % groupCount).add(filesToUpload.get(i));
            }
            return groups;
        }

        private List<File> remainingFiles(List<File> originalFiles) {
            List<File> remaining = new ArrayList<>();
            for (File file : originalFiles) {
                if (file != null && activeFileStats.containsKey(file.getName())) {
                    remaining.add(file);
                }
            }
            return remaining;
        }

        private void waitWhilePaused() throws InterruptedException {
            if (!paused) {
                return;
            }

            AsperaUploadService.this.log("Upload paused");
            updateStatus("Upload paused");
            synchronized (pauseLock) {
                while (paused && !isCancelled()) {
                    lastProgressTime.set(System.currentTimeMillis());
                    pauseLock.wait(500);
                }
            }
            if (!isCancelled()) {
                AsperaUploadService.this.log("Upload resumed");
                updateStatus("Transfer in progress...");
            }
        }

        /**
         * Wait for all sessions to complete, polling so we can react to cancellation
         * and detect a stalled transfer (no progress across all sessions) without
         * capping legitimately long transfers.
         */
        private boolean waitForCompletion() throws InterruptedException {
            while (!sessionCompleteLatch.await(1, TimeUnit.SECONDS)) {
                if (isCancelled()) {
                    errorMessage = "Transfer cancelled";
                    cancelAllSessions();
                    return false;
                }

                if (paused) {
                    lastProgressTime.set(System.currentTimeMillis());
                    continue;
                }

                long started = transferStartTime.get();
                long sinceProgress = System.currentTimeMillis() - lastProgressTime.get();
                if (started > 0 && sinceProgress > PROGRESS_TIMEOUT_MS) {
                    transferTimedOut.set(true);
                    errorMessage = "Transfer stalled: no progress for "
                            + (PROGRESS_TIMEOUT_MS / 60000) + " minutes";
                    AsperaUploadService.this.log(errorMessage);
                    cancelAllSessions();
                    return false;
                }
            }
            return true;
        }

        private void startSession(List<File> groupFiles) throws IOException, FaspManagerException {
            DropBoxDetail dropBox = uploadDetail.getDropBox();

            LocalLocation localFiles = new LocalLocation();
            for (File transferFile : groupFiles) {
                if (!transferFile.exists() || !transferFile.canRead()) {
                    throw new IOException("File is not readable: " + transferFile.getAbsolutePath());
                }
                localFiles.addPath(transferFile.getAbsolutePath(), transferFile.getName());
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
                    String transferId = FaspManager.getSingleton().startTransfer(order, this);
                    activeTransferIds.add(transferId);
                    applyCurrentPauseState(transferId);
                    log("Session started with ID " + transferId + " for " + groupFiles.size() + " file(s)");
                    return;
                } catch (Exception e) {
                    attempt++;
                    log("Session start attempt failed (" + attempt + "): " + e.getMessage());
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

        private void cancelAllSessions() {
            for (String transferId : activeTransferIds) {
                try {
                    FaspManager.getSingleton().cancelTransfer(transferId);
                } catch (Exception e) {
                    logger.warn("Failed to cancel Aspera transfer {}: {}", transferId, e.getMessage());
                }
            }
        }

        /**
         * Release every Aspera session/resource for this upload so file descriptors,
         * sockets and ascp subprocesses are not leaked across uploads.
         */
        private void releaseAllSessions() {
            for (String transferId : activeTransferIds) {
                try {
                    FaspManager.getSingleton().stopTransfer(transferId);
                } catch (Exception e) {
                    logger.debug("stopTransfer on finished session {}: {}", transferId, e.getMessage());
                }
            }
            activeTransferIds.clear();
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

        private void startMonitoring(CountDownLatch transferLatch, String fileName) {
            Thread monitorThread = new Thread(() -> {
                try {
                    while (!transferLatch.await(60, TimeUnit.SECONDS)) {
                        long now = System.currentTimeMillis();

                        long startTime = transferStartTime.get();
                        long lastProgress = lastProgressTime.get();

                        if (paused) {
                            continue;
                        }

                        if (startTime <= 0 || lastProgress <= 0) {
                            continue;
                        }

                        long sinceStart = now - startTime;
                        long sinceProgress = now - lastProgress;

                        // Only logging — NEVER kill transfer
                        if (sinceProgress > 15 * 60 * 1000) { // 15 min
                            logger.warn("Slow transfer for {}: no progress for {} min", fileName, sinceProgress / 60000);
                        }

                        if (sinceStart > 6 * 60 * 60 * 1000) { // 6 hours
                            logger.info("Long running transfer for {}: {} hours", fileName, sinceStart / (1000 * 60 * 60));
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
                    if (stats != null) {
                        logger.info("Aspera session started: {}", describeSession(stats));
                        rememberSessionIds(stats);
                        applyCurrentPauseState(stats.getSessionId());
                        applyCurrentPauseState(stats.getXferId() != null ? stats.getXferId().toString() : null);
                    }
                    updateStatus(paused ? "Upload paused" : "Transfer in progress...");
                    transferStartTime.compareAndSet(0, now);
                    lastProgressTime.set(System.currentTimeMillis());
                    break;

                case PROGRESS:
                    lastProgressTime.set(System.currentTimeMillis());
                    if (stats != null) {
                        // Each PROGRESS event reports the cumulative bytes for ITS session.
                        // Track per-session and sum across all sessions for overall progress.
                        if (stats.getSessionId() != null) {
                            sessionTransferredBytes.put(stats.getSessionId(), stats.getTotalTransferredBytes());
                        }
                        long transferred = sessionTransferredBytes.values().stream()
                                .mapToLong(Long::longValue).sum();
                        long totalBytes = totalBytesProperty().get();

                        String currentName = fileInfo != null ? extractFileName(fileInfo.getName()) : null;
                        double fileProg = 0;
                        if (fileInfo != null && fileInfo.getSizeBytes() > 0) {
                            fileProg = Math.min(1.0, (double) fileInfo.getWrittenBytes() / fileInfo.getSizeBytes());
                        }
                        final double finalFileProg = fileProg;
                        final int done = finishedFileCount.get();

                        Platform.runLater(() -> {
                            uploadedFilesProperty().set(done);
                            uploadedBytesProperty().set(transferred);
                            if (currentName != null) {
                                currentFileNameProperty().set(currentName);
                            }
                            currentFileProgressProperty().set(finalFileProg);

                            double progress = totalBytes > 0 ? (double) transferred / totalBytes : 0;
                            overallProgress.set(Math.min(1.0, progress));
                        });

                        updateStatus(String.format("Uploading: %d/%d files (%.1f%%)",
                                done, totalFilesProperty().get(),
                                totalBytes > 0 ? (double) transferred / totalBytes * 100 : 0));
                    }
                    break;

                case FILE_STOP:
                    if (fileInfo != null
                            && (fileInfo.getState() == FileState.FINISHED
                                || fileInfo.getState() == FileState.SKIPPED)) {
                        markFileCompleted(extractFileName(fileInfo.getName()));
                    }
                    break;

                case FILE_SKIP:
                    // File already present on the server (e.g. from a previous attempt);
                    // treat as completed so it does not count as a failure.
                    if (fileInfo != null) {
                        markFileCompleted(extractFileName(fileInfo.getName()));
                    }
                    break;

                case SESSION_STOP:
                    AsperaUploadService.this.log(paused
                            ? "Transfer session stopped for pause"
                            : "Transfer session completed");

                    // Log session stop event
                    if (transferStatistics != null) {
                        StatisticsLogger.logEvent(
                                transferStatistics.getSessionId(),
                                "SESSION_STOP",
                                "Transfer session completed normally"
                        );
                    }

                    signalSessionComplete();
                    break;

                case FILE_ERROR:
                    if (paused) {
                        if (fileInfo != null) {
                            AsperaUploadService.this.log("File transfer stopped for pause: "
                                    + extractFileName(fileInfo.getName()));
                        }
                        break;
                    }
                    if (fileInfo != null) {
                        String errorFile = extractFileName(fileInfo.getName());
                        String error = describeFileError(fileInfo);
                        AsperaUploadService.this.log("File error: " + errorFile + " - " + error);
                        errorMessage = "Failed to upload " + errorFile + ": " + error;
                        failedFileCount.incrementAndGet();

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
                    if (paused) {
                        AsperaUploadService.this.log("Transfer session stopped for pause");
                        signalSessionComplete();
                        break;
                    }
                    String sessionError = describeSessionError(event, stats, fileInfo);
                    logger.error("Aspera session error: {}", sessionError);
                    AsperaUploadService.this.log("Session error: " + sessionError);
                    errorMessage = "Aspera session error: " + sessionError;

                    // Log session error event
                    if (transferStatistics != null) {
                        StatisticsLogger.logEvent(
                                transferStatistics.getSessionId(),
                                "SESSION_ERROR",
                                sessionError
                        );
                    }

                    signalSessionComplete();
                    break;

                default:
                    logger.debug("Unhandled event: {}", event);
            }
        }

        /**
         * Mark a single file (identified by name) as completed within the session.
         * Called once per file from FILE_STOP events.
         */
        private void markFileCompleted(String fileName) {
            if (fileName == null) {
                return;
            }

            // Remove returns null if this file was already completed or is unknown,
            // which guards against double-counting.
            FileTransferStat fileStat = activeFileStats.remove(fileName);
            if (fileStat == null) {
                return;
            }

            int done = finishedFileCount.incrementAndGet();

            logger.info("Completed file: {} ({}/{})", fileName, done, totalFilesToTransfer);
            AsperaUploadService.this.log("Completed: " + fileName
                    + " (" + done + "/" + totalFilesToTransfer + ")");

            if (transferStatistics != null) {
                transferStatistics.completeFile(fileName, fileStat.getFileSize());
                StatisticsLogger.logFileComplete(fileStat);
            }

            Platform.runLater(() -> {
                uploadedFilesProperty().set(done);
                currentFileNameProperty().set(fileName);
                currentFileProgressProperty().set(1.0);
            });
        }

        private void signalSessionComplete() {
            CountDownLatch latch = sessionCompleteLatch;
            if (latch != null) {
                latch.countDown();
            }
        }

        private void rememberSessionIds(SessionStats stats) {
            if (stats == null) {
                return;
            }
            if (stats.getSessionId() != null && !stats.getSessionId().isBlank()) {
                activeTransferIds.add(stats.getSessionId());
            }
            if (stats.getXferId() != null) {
                activeTransferIds.add(stats.getXferId().toString());
            }
        }

        private String describeSessionError(TransferEvent event, SessionStats stats, FileInfo fileInfo) {
            List<String> details = new ArrayList<>();
            appendDetail(details, "event", event != null ? event.getDescription() : null);

            if (stats != null) {
                appendDetail(details, "errorCode", stats.getErrorCode() != 0 ? stats.getErrorCode() : null);
                appendDetail(details, "errorDescription", stats.getErrorDescription());
                appendDetail(details, "state", stats.getState());
                appendDetail(details, "sessionId", stats.getSessionId());
                appendDetail(details, "xferId", stats.getXferId());
                appendDetail(details, "host", stats.getHost());
                appendDetail(details, "user", stats.getUser());
                appendDetail(details, "destination", stats.getDestPath());
                appendDetail(details, "sources", joinPaths(stats.getSourcePaths()));
                appendDetail(details, "transfers",
                        "attempted=" + stats.getTransfersAttempted()
                                + ", passed=" + stats.getTransfersPassed()
                                + ", failed=" + stats.getTransfersFailed()
                                + ", skipped=" + stats.getTransfersSkipped());
                appendDetail(details, "files",
                        "complete=" + stats.getFilesComplete()
                                + ", failed=" + stats.getFilesFailed()
                                + ", skipped=" + stats.getFilesSkipped());
                appendDetail(details, "bytes",
                        stats.getTotalTransferredBytes() + "/" + stats.getPreCalcTotalBytes());
                appendDetail(details, "udpPort", stats.getUdpPort() != 0 ? stats.getUdpPort() : null);
            }

            if (fileInfo != null) {
                appendDetail(details, "file", describeFileError(fileInfo));
            }

            return details.isEmpty() ? "Session error" : String.join("; ", details);
        }

        private String describeSession(SessionStats stats) {
            List<String> details = new ArrayList<>();
            appendDetail(details, "sessionId", stats.getSessionId());
            appendDetail(details, "xferId", stats.getXferId());
            appendDetail(details, "host", stats.getHost());
            appendDetail(details, "destination", stats.getDestPath());
            appendDetail(details, "sources", joinPaths(stats.getSourcePaths()));
            appendDetail(details, "udpPort", stats.getUdpPort() != 0 ? stats.getUdpPort() : null);
            return details.isEmpty() ? "no session details" : String.join("; ", details);
        }

        private String describeFileError(FileInfo fileInfo) {
            List<String> details = new ArrayList<>();
            appendDetail(details, "name", fileInfo.getName());
            appendDetail(details, "state", fileInfo.getState());
            appendDetail(details, "errorCode", fileInfo.getErrCode() != 0 ? fileInfo.getErrCode() : null);
            appendDetail(details, "errorDescription", fileInfo.getErrDescription());
            appendDetail(details, "writtenBytes", fileInfo.getWrittenBytes());
            appendDetail(details, "sizeBytes", fileInfo.getSizeBytes());
            return details.isEmpty() ? "unknown file error" : String.join("; ", details);
        }

        private String joinPaths(String[] paths) {
            if (paths == null || paths.length == 0) {
                return null;
            }
            return String.join(", ", paths);
        }

        private void appendDetail(List<String> details, String label, Object value) {
            if (value == null) {
                return;
            }
            String text = value.toString();
            if (text.trim().isEmpty()) {
                return;
            }
            details.add(label + "=" + text);
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
