package uk.ac.ebi.pride.pxsubmit.service;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics.FileTransferStat;

import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JavaFX Service for FTP file uploads with concurrent upload support.
 *
 * Features:
 * - Concurrent uploads (configurable, default 3)
 * - Progress reporting via JavaFX properties
 * - Retry logic with exponential backoff
 * - Cancellation support
 * - Resume capability for interrupted uploads
 */
public class FtpUploadService extends AbstractUploadService {

    // Configuration constants
    private static final int DEFAULT_CONCURRENT_UPLOADS = 3;
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;
    private static final int CONNECTION_TIMEOUT_MS = 30000;
    private static final int DATA_TIMEOUT_MS = 300000;
    private static final int SIZE_VERIFY_RETRIES = 3;
    private static final long SIZE_VERIFY_DELAY_MS = 500;

    // FTP-specific properties
    private final int concurrentUploads;

    // Status tracking
    private final ObservableList<DataFile> completedFiles = FXCollections.observableArrayList();
    private final ObservableList<DataFile> failedFiles = FXCollections.observableArrayList();

    // Thread pool for concurrent uploads
    private ExecutorService uploadExecutor;

    // Pause/resume support
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();

    public FtpUploadService(List<DataFile> files, UploadDetail uploadDetail) {
        this(files, uploadDetail, DEFAULT_CONCURRENT_UPLOADS);
    }

    public FtpUploadService(List<DataFile> files, UploadDetail uploadDetail, int concurrentUploads) {
        super(files, uploadDetail);
        this.concurrentUploads = concurrentUploads;
    }

    @Override
    protected Task<UploadResult> createTask() {
        return new FtpUploadTask();
    }

    @Override
    public boolean cancel() {
        if (uploadExecutor != null) {
            uploadExecutor.shutdownNow();
        }
        // Wake up any paused thread so it can observe the cancellation
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        return super.cancel();
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    // FTP-specific property accessors
    public ObservableList<DataFile> getCompletedFiles() { return completedFiles; }
    public ObservableList<DataFile> getFailedFiles() { return failedFiles; }

    /**
     * Main upload task that coordinates concurrent file uploads
     */
    private class FtpUploadTask extends Task<UploadResult> {

        private final AtomicLong bytesUploaded = new AtomicLong(0);
        private final AtomicInteger filesUploaded = new AtomicInteger(0);
        private final BlockingQueue<DataFile> fileQueue = new LinkedBlockingQueue<>();

        @Override
        protected UploadResult call() throws Exception {
            // Validate upload details
            if (uploadDetail == null) {
                throw new IllegalStateException("Upload detail is null");
            }
            if (uploadDetail.getHost() == null || uploadDetail.getHost().isEmpty()) {
                throw new IllegalStateException("FTP host is not configured");
            }
            if (uploadDetail.getDropBox() == null) {
                throw new IllegalStateException("DropBox credentials are not configured");
            }
            if (uploadDetail.getDropBox().getUserName() == null ||
                uploadDetail.getDropBox().getUserName().isEmpty()) {
                throw new IllegalStateException("FTP username is not configured");
            }
            if (uploadDetail.getDropBox().getPassword() == null ||
                uploadDetail.getDropBox().getPassword().isEmpty()) {
                throw new IllegalStateException("FTP password is not configured");
            }

            FtpUploadService.this.log("Starting FTP upload of " + files.size() + " files");
            FtpUploadService.this.log("Target: " + uploadDetail.getHost() + ":" + uploadDetail.getPort());
            FtpUploadService.this.log("Folder: " + uploadDetail.getFolder());
            FtpUploadService.this.log("Username: " + uploadDetail.getDropBox().getUserName());

            // Initialize transfer statistics
            transferStatistics = new TransferStatistics("FTP", files.size(), totalBytesProperty().get());
            StatisticsLogger.logSessionStart(transferStatistics);

            // Create FTP directory FIRST (before parallel uploads)
            createFtpDirectory();

            // Initialize file queue
            fileQueue.addAll(files);

            // Create thread pool for concurrent uploads
            uploadExecutor = Executors.newFixedThreadPool(concurrentUploads, r -> {
                Thread t = new Thread(r, "FTP-Upload-Worker");
                t.setDaemon(true);
                return t;
            });

            // Submit upload workers
            CompletionService<SingleFileResult> completionService =
                    new ExecutorCompletionService<>(uploadExecutor);

            int submittedTasks = 0;
            for (int i = 0; i < Math.min(concurrentUploads, files.size()); i++) {
                DataFile file = fileQueue.poll();
                if (file != null) {
                    completionService.submit(() -> uploadSingleFile(file));
                    submittedTasks++;
                }
            }

            // Process results and submit more files as uploads complete
            while (submittedTasks > 0 || (!fileQueue.isEmpty() && !isCancelled())) {
                if (isCancelled()) {
                    FtpUploadService.this.log("Upload cancelled by user");
                    break;
                }

                // If paused with no in-flight tasks, wait for resume
                if (paused && submittedTasks == 0 && !fileQueue.isEmpty()) {
                    FtpUploadService.this.log("Upload paused");
                    waitWhilePaused();
                    if (isCancelled()) break;
                    FtpUploadService.this.log("Upload resumed");
                    for (int i = 0; i < Math.min(concurrentUploads, fileQueue.size()); i++) {
                        DataFile file = fileQueue.poll();
                        if (file != null) {
                            completionService.submit(() -> uploadSingleFile(file));
                            submittedTasks++;
                        }
                    }
                    continue;
                }

                if (submittedTasks == 0) break;

                Future<SingleFileResult> future = completionService.poll(1, TimeUnit.SECONDS);
                if (future != null) {
                    submittedTasks--;
                    try {
                        SingleFileResult result = future.get();
                        handleFileResult(result);

                        // Submit next file if available and not paused
                        if (!paused) {
                            DataFile nextFile = fileQueue.poll();
                            if (nextFile != null && !isCancelled()) {
                                completionService.submit(() -> uploadSingleFile(nextFile));
                                submittedTasks++;
                            }
                        }
                    } catch (ExecutionException e) {
                        logger.error("Upload task failed", e.getCause());
                    }
                }

                // Update overall progress
                updateOverallProgress();
            }

            // Shutdown executor
            uploadExecutor.shutdown();
            if (!uploadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                uploadExecutor.shutdownNow();
            }

            // Build result
            UploadResult result = UploadResult.ftpResult(
                    completedFiles.size() == files.size(),
                    completedFiles.size(),
                    failedFiles.size(),
                    bytesUploaded.get()
            );

            // Complete statistics and log summary
            if (transferStatistics != null) {
                transferStatistics.setSessionComplete();
                StatisticsLogger.logSessionSummary(transferStatistics);
            }

            if (result.isSuccess()) {
                FtpUploadService.this.log("Upload completed successfully!");
            } else {
                FtpUploadService.this.log("Upload completed with " + failedFiles.size() + " failed files");
            }

            return result;
        }

        private void waitWhilePaused() {
            synchronized (pauseLock) {
                while (paused && !isCancelled()) {
                    try {
                        pauseLock.wait(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        private void handleFileResult(SingleFileResult result) {
            Platform.runLater(() -> {
                if (result.success) {
                    completedFiles.add(result.file);
                    uploadedFilesProperty().set(completedFiles.size());
                    FtpUploadService.this.log("Uploaded: " + result.file.getFileName());
                } else {
                    failedFiles.add(result.file);
                    FtpUploadService.this.log("Failed: " + result.file.getFileName() + " - " + result.errorMessage);
                }
            });
        }

        /**
         * Create FTP directory before uploads begin (like master branch CreateFTPDirectoryTask)
         */
        private void createFtpDirectory() throws IOException {
            FTPClient ftp = new FTPClient();
            ftp.setControlKeepAliveTimeout(Duration.ofSeconds(300));

            try {
                FtpUploadService.this.log("Creating FTP directory...");
                System.out.println("FTP: Creating upload directory...");

                ftp.connect(uploadDetail.getHost(), uploadDetail.getPort());

                int reply = ftp.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    String errorMsg = "Failed to connect to FTP server - Reply code: " + reply;
                    System.err.println("FTP ERROR: " + errorMsg);
                    throw new IOException(errorMsg);
                }

                String username = uploadDetail.getDropBox().getUserName();
                if (!ftp.login(username, uploadDetail.getDropBox().getPassword())) {
                    String errorMsg = "FTP login failed for user: " + username;
                    System.err.println("FTP ERROR: " + errorMsg);
                    throw new IOException(errorMsg);
                }

                System.out.println("FTP: Connected as " + username);

                // Enter passive mode
                ftp.enterLocalPassiveMode();

                // Create directory using folder name (like master branch)
                File folder = new File(uploadDetail.getFolder());
                String folderName = folder.getName();

                logger.info("Creating directory: {}", folderName);
                System.out.println("FTP: Creating directory: " + folderName);

                // mkd creates the directory
                ftp.mkd(folderName);

                // Check if successful (mkd returns 257 on success)
                int mkdReply = ftp.getReplyCode();
                if (mkdReply == 257) {
                    FtpUploadService.this.log("Directory created successfully: " + folderName);
                    System.out.println("FTP: Directory created successfully: " + folderName);
                } else if (mkdReply == 550) {
                    // Directory might already exist, which is OK
                    FtpUploadService.this.log("Directory may already exist: " + folderName);
                    System.out.println("FTP: Directory may already exist (continuing): " + folderName);
                } else {
                    FtpUploadService.this.log("Directory creation returned: " + ftp.getReplyString());
                    System.out.println("FTP: Directory creation reply: " + ftp.getReplyString());
                }

            } finally {
                if (ftp.isConnected()) {
                    try {
                        ftp.logout();
                        ftp.disconnect();
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                }
            }
        }

        private void updateOverallProgress() {
            long uploaded = bytesUploaded.get();
            long total = totalBytesProperty().get();
            double progress = total > 0 ? (double) uploaded / total : 0;

            Platform.runLater(() -> {
                uploadedBytesProperty().set(uploaded);
                uploadedFilesProperty().set(filesUploaded.get());
            });

            updateProgress(uploaded, total);
            updateMessage(String.format("Uploaded %d of %d files (%.1f%%)",
                    filesUploaded.get(), files.size(), progress * 100));
        }

        private SingleFileResult uploadSingleFile(DataFile dataFile) {
            String fileName = dataFile.getFileName();
            long fileSize = dataFile.getFile() != null ? dataFile.getFile().length() : 0;
            Platform.runLater(() -> currentFileNameProperty().set(fileName));

            // Start file statistics tracking
            FileTransferStat fileStat = null;
            if (transferStatistics != null) {
                fileStat = transferStatistics.startFile(fileName, fileSize);
                StatisticsLogger.logFileStart(fileStat);
            }

            FTPClient ftp = new FTPClient();
            int retryCount = 0;

            while (retryCount < MAX_RETRIES) {
                if (isCancelled()) {
                    if (fileStat != null) {
                        transferStatistics.failFile(fileName, "Cancelled", retryCount);
                        StatisticsLogger.logFileComplete(fileStat);
                    }
                    return new SingleFileResult(dataFile, false, "Cancelled");
                }

                try {
                    // Configure FTP client
                    ftp.setBufferSize(BUFFER_SIZE);
                    ftp.setControlKeepAliveTimeout(Duration.ofSeconds(60));
                    ftp.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                    ftp.setDefaultTimeout(DATA_TIMEOUT_MS);

                    // Connect
                    long connectStart = System.currentTimeMillis();
                    logger.info("Connecting to FTP {}:{} for file: {}",
                        uploadDetail.getHost(), uploadDetail.getPort(), fileName);
                    ftp.connect(uploadDetail.getHost(), uploadDetail.getPort());

                    int replyCode = ftp.getReplyCode();
                    if (!FTPReply.isPositiveCompletion(replyCode)) {
                        String errorMsg = "FTP connection failed - Reply code: " + replyCode + " - " + ftp.getReplyString();
                        System.err.println("FTP ERROR: " + errorMsg);
                        throw new IOException(errorMsg);
                    }
                    logger.debug("FTP connected successfully");

                    // Login
                    String username = uploadDetail.getDropBox().getUserName();
                    logger.debug("Attempting FTP login as user: {}", username);
                    if (!ftp.login(username, uploadDetail.getDropBox().getPassword())) {
                        String errorMsg = "FTP login failed for user: " + username + " - Reply: " + ftp.getReplyString();
                        System.err.println("FTP ERROR: " + errorMsg);
                        throw new IOException(errorMsg);
                    }
                    logger.debug("FTP login successful");
                    System.out.println("FTP: Connected and logged in successfully as " + username);

                    // Validate connection responsiveness with NOOP
                    if (!ftp.sendNoOp()) {
                        logger.warn("FTP NOOP check failed, connection may be unresponsive");
                    }

                    // Log connection success
                    long connectDuration = System.currentTimeMillis() - connectStart;
                    if (transferStatistics != null) {
                        StatisticsLogger.logConnectionAttempt(
                            transferStatistics.getSessionId(),
                            uploadDetail.getHost(),
                            uploadDetail.getPort(),
                            true,
                            connectDuration
                        );
                    }

                    // Configure transfer mode
                    ftp.enterLocalPassiveMode();

                    // Validate passive mode is working
                    if (ftp.getPassiveHost() == null) {
                        logger.warn("Passive mode may not be properly configured");
                    }

                    ftp.setFileType(FTP.BINARY_FILE_TYPE);

                    // Change to upload directory (use folder.getName() like master branch)
                    String folder = uploadDetail.getFolder();
                    if (folder != null && !folder.isEmpty()) {
                        File folderFile = new File(folder);
                        String folderName = folderFile.getName();

                        logger.info("Changing to directory: {}", folderName);
                        System.out.println("FTP: Changing to directory: " + folderName);

                        if (!ftp.changeWorkingDirectory(folderName)) {
                            String errorMsg = "Failed to change to directory: " + folderName + " - Reply: " + ftp.getReplyString();
                            logger.error(errorMsg);
                            System.err.println("FTP ERROR: " + errorMsg);
                            throw new IOException(errorMsg);
                        }
                        logger.info("Successfully in directory: {}", folderName);
                        System.out.println("FTP: Successfully in directory: " + folderName);
                    }

                    // Upload file (with resume support on retry)
                    try (InputStream input = createInputStream(dataFile)) {
                        long resumeOffset = 0;

                        // On retry, attempt to resume from partial upload
                        if (retryCount > 0 && fileSize > 0) {
                            try {
                                long remoteSize = getRemoteFileSize(ftp, fileName);
                                if (remoteSize > 0 && remoteSize < fileSize) {
                                    long skipped = input.skip(remoteSize);
                                    if (skipped == remoteSize) {
                                        ftp.setRestartOffset(remoteSize);
                                        resumeOffset = remoteSize;
                                        bytesUploaded.addAndGet(remoteSize);
                                        logger.info("Resuming upload of {} from offset {}", fileName, remoteSize);
                                    } else {
                                        // Skip failed, fall back to full upload
                                        logger.warn("Skip failed for resume (expected {}, got {}), uploading from start", remoteSize, skipped);
                                    }
                                }
                            } catch (Exception resumeEx) {
                                // Resume not supported or failed, fall back to full upload
                                logger.debug("Resume not available for {}: {}", fileName, resumeEx.getMessage());
                            }
                        }

                        final long effectiveOffset = resumeOffset;
                        // Use custom stream to track progress
                        ProgressInputStream progressStream = new ProgressInputStream(input, fileSize - effectiveOffset,
                                (bytesRead, total) -> {
                                    bytesUploaded.addAndGet(bytesRead);
                                    if (fileSize > 0) {
                                        double fileProgress = (double) (effectiveOffset + bytesRead) / fileSize;
                                        Platform.runLater(() -> currentFileProgressProperty().set(fileProgress));
                                    }
                                });

                        boolean stored = ftp.storeFile(fileName, progressStream);
                        if (!stored) {
                            throw new IOException("FTP storeFile failed: " + ftp.getReplyString());
                        }
                    }

                    // Verify upload with retry loop for size verification
                    if (fileSize > 0) {
                        boolean sizeVerified = false;
                        long uploadedSize = -1;

                        for (int verifyAttempt = 0; verifyAttempt < SIZE_VERIFY_RETRIES && !sizeVerified; verifyAttempt++) {
                            if (verifyAttempt > 0) {
                                Thread.sleep(SIZE_VERIFY_DELAY_MS);
                            }
                            uploadedSize = getRemoteFileSize(ftp, fileName);
                            if (uploadedSize == fileSize) {
                                sizeVerified = true;
                            } else {
                                logger.debug("Size verification attempt {} for {}: expected {}, got {}",
                                    verifyAttempt + 1, fileName, fileSize, uploadedSize);
                            }
                        }

                        if (!sizeVerified) {
                            throw new IOException("Size mismatch after " + SIZE_VERIFY_RETRIES +
                                    " verification attempts: expected " + fileSize + ", got " + uploadedSize);
                        }
                    }

                    // Track successful upload in statistics
                    if (transferStatistics != null && fileStat != null) {
                        transferStatistics.completeFile(fileName, fileSize);
                        StatisticsLogger.logFileComplete(fileStat);
                    }

                    filesUploaded.incrementAndGet();
                    logger.info("Successfully uploaded: {}", fileName);
                    System.out.println("FTP: Successfully uploaded: " + fileName);
                    return new SingleFileResult(dataFile, true, null);

                } catch (Exception e) {
                    retryCount++;
                    String errorMsg = "Upload attempt " + retryCount + " failed for " + fileName + ": " + e.getMessage();
                    logger.warn(errorMsg);
                    System.err.println("FTP ERROR: " + errorMsg);

                    // Log retry event
                    if (transferStatistics != null && fileStat != null) {
                        fileStat.incrementRetries();
                        StatisticsLogger.logRetry(
                            transferStatistics.getSessionId(),
                            fileName,
                            retryCount,
                            e.getMessage()
                        );
                    }

                    if (retryCount < MAX_RETRIES) {
                        try {
                            long delay = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, retryCount - 1);
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            if (transferStatistics != null) {
                                transferStatistics.failFile(fileName, "Interrupted", retryCount);
                                if (fileStat != null) {
                                    StatisticsLogger.logFileComplete(fileStat);
                                }
                            }
                            return new SingleFileResult(dataFile, false, "Interrupted");
                        }
                    }
                } finally {
                    // Cleanup
                    try {
                        if (ftp.isConnected()) {
                            ftp.logout();
                            ftp.disconnect();
                        }
                    } catch (IOException e) {
                        logger.warn("Error disconnecting FTP", e);
                    }
                }
            }

            String errorMsg = "Failed after " + MAX_RETRIES + " attempts";
            logger.error("Upload failed for {}: {}", fileName, errorMsg);
            System.err.println("FTP ERROR: Upload failed for " + fileName + ": " + errorMsg);

            // Track failed upload in statistics
            if (transferStatistics != null) {
                transferStatistics.failFile(fileName, errorMsg, retryCount);
                if (fileStat != null) {
                    StatisticsLogger.logFileComplete(fileStat);
                }
            }

            return new SingleFileResult(dataFile, false, errorMsg);
        }

        private InputStream createInputStream(DataFile dataFile) throws IOException {
            if (dataFile.isFile() && dataFile.getFile() != null) {
                return new BufferedInputStream(new FileInputStream(dataFile.getFile()));
            } else if (dataFile.isUrl() && dataFile.getUrl() != null) {
                return new BufferedInputStream(dataFile.getUrl().openStream());
            }
            throw new IOException("No valid file source for: " + dataFile.getFileName());
        }

        private long getRemoteFileSize(FTPClient ftp, String fileName) throws IOException {
            if (ftp.sendCommand("SIZE", fileName) == FTPReply.FILE_STATUS) {
                String reply = ftp.getReplyString();
                String[] parts = reply.split(" ");
                if (parts.length >= 2) {
                    return Long.parseLong(parts[1].trim());
                }
            }
            return -1;
        }
    }

    /**
     * Result for a single file upload
     */
    private static class SingleFileResult {
        final DataFile file;
        final boolean success;
        final String errorMessage;

        SingleFileResult(DataFile file, boolean success, String errorMessage) {
            this.file = file;
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Input stream wrapper that tracks progress
     */
    private static class ProgressInputStream extends FilterInputStream {
        private final long totalSize;
        private final ProgressCallback callback;
        private long bytesRead = 0;
        private long lastReported = 0;
        private long lastReportTime = System.currentTimeMillis();
        private static final long REPORT_INTERVAL = 1048576; // Report every 1MB
        private static final long REPORT_MIN_INTERVAL_MS = 200; // At most every 200ms

        public interface ProgressCallback {
            void onProgress(long bytesRead, long total);
        }

        public ProgressInputStream(InputStream in, long totalSize, ProgressCallback callback) {
            super(in);
            this.totalSize = totalSize;
            this.callback = callback;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                bytesRead++;
                reportProgress();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = super.read(b, off, len);
            if (n > 0) {
                bytesRead += n;
                reportProgress();
            }
            return n;
        }

        private void reportProgress() {
            boolean isComplete = bytesRead == totalSize;
            boolean sizeThreshold = bytesRead - lastReported >= REPORT_INTERVAL;
            boolean timeThreshold = System.currentTimeMillis() - lastReportTime >= REPORT_MIN_INTERVAL_MS;

            if (isComplete || (sizeThreshold && timeThreshold)) {
                callback.onProgress(bytesRead - lastReported, totalSize);
                lastReported = bytesRead;
                lastReportTime = System.currentTimeMillis();
            }
        }
    }
}
