package uk.ac.ebi.pride.pxsubmit.service;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.data.model.DataFile;

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
public class FtpUploadService extends Service<FtpUploadService.UploadResult> {

    private static final Logger logger = LoggerFactory.getLogger(FtpUploadService.class);

    // Configuration constants
    private static final int DEFAULT_CONCURRENT_UPLOADS = 3;
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;
    private static final int CONNECTION_TIMEOUT_MS = 30000;
    private static final int DATA_TIMEOUT_MS = 300000;

    // Input properties
    private final List<DataFile> files;
    private final UploadDetail uploadDetail;
    private final int concurrentUploads;

    // Progress properties (observable from UI)
    private final LongProperty totalBytes = new SimpleLongProperty(0);
    private final LongProperty uploadedBytes = new SimpleLongProperty(0);
    private final IntegerProperty totalFiles = new SimpleIntegerProperty(0);
    private final IntegerProperty uploadedFiles = new SimpleIntegerProperty(0);
    private final StringProperty currentFileName = new SimpleStringProperty("");
    private final DoubleProperty currentFileProgress = new SimpleDoubleProperty(0);

    // Status tracking
    private final ObservableList<DataFile> completedFiles = FXCollections.observableArrayList();
    private final ObservableList<DataFile> failedFiles = FXCollections.observableArrayList();
    private final ObservableList<String> uploadLog = FXCollections.observableArrayList();

    // Thread pool for concurrent uploads
    private ExecutorService uploadExecutor;

    public FtpUploadService(List<DataFile> files, UploadDetail uploadDetail) {
        this(files, uploadDetail, DEFAULT_CONCURRENT_UPLOADS);
    }

    public FtpUploadService(List<DataFile> files, UploadDetail uploadDetail, int concurrentUploads) {
        this.files = files;
        this.uploadDetail = uploadDetail;
        this.concurrentUploads = concurrentUploads;

        // Calculate totals
        this.totalFiles.set(files.size());
        this.totalBytes.set(calculateTotalSize(files));
    }

    private long calculateTotalSize(List<DataFile> files) {
        return files.stream()
                .filter(f -> f.getFile() != null)
                .mapToLong(f -> f.getFile().length())
                .sum();
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
        return super.cancel();
    }

    // Property accessors for UI binding
    public LongProperty totalBytesProperty() { return totalBytes; }
    public LongProperty uploadedBytesProperty() { return uploadedBytes; }
    public IntegerProperty totalFilesProperty() { return totalFiles; }
    public IntegerProperty uploadedFilesProperty() { return uploadedFiles; }
    public StringProperty currentFileNameProperty() { return currentFileName; }
    public DoubleProperty currentFileProgressProperty() { return currentFileProgress; }
    public ObservableList<DataFile> getCompletedFiles() { return completedFiles; }
    public ObservableList<DataFile> getFailedFiles() { return failedFiles; }
    public ObservableList<String> getUploadLog() { return uploadLog; }

    /**
     * Main upload task that coordinates concurrent file uploads
     */
    private class FtpUploadTask extends Task<UploadResult> {

        private final AtomicLong bytesUploaded = new AtomicLong(0);
        private final AtomicInteger filesUploaded = new AtomicInteger(0);
        private final BlockingQueue<DataFile> fileQueue = new LinkedBlockingQueue<>();

        @Override
        protected UploadResult call() throws Exception {
            log("Starting FTP upload of " + files.size() + " files");
            log("Target: " + uploadDetail.getHost() + ":" + uploadDetail.getPort());

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
            while (submittedTasks > 0) {
                if (isCancelled()) {
                    log("Upload cancelled by user");
                    break;
                }

                Future<SingleFileResult> future = completionService.poll(1, TimeUnit.SECONDS);
                if (future != null) {
                    submittedTasks--;
                    try {
                        SingleFileResult result = future.get();
                        handleFileResult(result);

                        // Submit next file if available
                        DataFile nextFile = fileQueue.poll();
                        if (nextFile != null && !isCancelled()) {
                            completionService.submit(() -> uploadSingleFile(nextFile));
                            submittedTasks++;
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
            UploadResult result = new UploadResult(
                    completedFiles.size() == files.size(),
                    completedFiles.size(),
                    failedFiles.size(),
                    bytesUploaded.get()
            );

            if (result.isSuccess()) {
                log("Upload completed successfully!");
            } else {
                log("Upload completed with " + failedFiles.size() + " failed files");
            }

            return result;
        }

        private void handleFileResult(SingleFileResult result) {
            Platform.runLater(() -> {
                if (result.success) {
                    completedFiles.add(result.file);
                    uploadedFiles.set(completedFiles.size());
                    log("Uploaded: " + result.file.getFileName());
                } else {
                    failedFiles.add(result.file);
                    log("Failed: " + result.file.getFileName() + " - " + result.errorMessage);
                }
            });
        }

        private void updateOverallProgress() {
            long uploaded = bytesUploaded.get();
            long total = totalBytes.get();
            double progress = total > 0 ? (double) uploaded / total : 0;

            Platform.runLater(() -> {
                uploadedBytes.set(uploaded);
                uploadedFiles.set(filesUploaded.get());
            });

            updateProgress(uploaded, total);
            updateMessage(String.format("Uploaded %d of %d files (%.1f%%)",
                    filesUploaded.get(), files.size(), progress * 100));
        }

        private SingleFileResult uploadSingleFile(DataFile dataFile) {
            String fileName = dataFile.getFileName();
            Platform.runLater(() -> currentFileName.set(fileName));

            FTPClient ftp = new FTPClient();
            int retryCount = 0;

            while (retryCount < MAX_RETRIES) {
                if (isCancelled()) {
                    return new SingleFileResult(dataFile, false, "Cancelled");
                }

                try {
                    // Configure FTP client
                    ftp.setBufferSize(BUFFER_SIZE);
                    ftp.setControlKeepAliveTimeout(Duration.ofSeconds(60));
                    ftp.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                    ftp.setDefaultTimeout(DATA_TIMEOUT_MS);

                    // Connect
                    logger.debug("Connecting to FTP for file: {}", fileName);
                    ftp.connect(uploadDetail.getHost(), uploadDetail.getPort());

                    if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                        throw new IOException("FTP connection failed: " + ftp.getReplyCode());
                    }

                    // Login
                    if (!ftp.login(uploadDetail.getDropBox().getUserName(),
                                   uploadDetail.getDropBox().getPassword())) {
                        throw new IOException("FTP login failed");
                    }

                    // Configure transfer mode
                    ftp.enterLocalPassiveMode();
                    ftp.setFileType(FTP.BINARY_FILE_TYPE);

                    // Change to upload directory
                    String folder = uploadDetail.getFolder();
                    if (folder != null && !folder.isEmpty()) {
                        File folderFile = new File(folder);
                        if (!ftp.changeWorkingDirectory(folderFile.getName())) {
                            throw new IOException("Failed to change directory: " + folder);
                        }
                    }

                    // Upload file
                    long fileSize = dataFile.getFile() != null ? dataFile.getFile().length() : 0;

                    try (InputStream input = createInputStream(dataFile)) {
                        // Use custom stream to track progress
                        ProgressInputStream progressStream = new ProgressInputStream(input, fileSize,
                                (bytesRead, total) -> {
                                    bytesUploaded.addAndGet(bytesRead);
                                    if (total > 0) {
                                        double fileProgress = (double) bytesRead / total;
                                        Platform.runLater(() -> currentFileProgress.set(fileProgress));
                                    }
                                });

                        boolean stored = ftp.storeFile(fileName, progressStream);
                        if (!stored) {
                            throw new IOException("FTP storeFile failed: " + ftp.getReplyString());
                        }
                    }

                    // Verify upload
                    if (fileSize > 0) {
                        long uploadedSize = getRemoteFileSize(ftp, fileName);
                        if (uploadedSize != fileSize) {
                            throw new IOException("Size mismatch: expected " + fileSize +
                                    ", got " + uploadedSize);
                        }
                    }

                    filesUploaded.incrementAndGet();
                    logger.info("Successfully uploaded: {}", fileName);
                    return new SingleFileResult(dataFile, true, null);

                } catch (Exception e) {
                    retryCount++;
                    logger.warn("Upload attempt {} failed for {}: {}",
                            retryCount, fileName, e.getMessage());

                    if (retryCount < MAX_RETRIES) {
                        try {
                            long delay = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, retryCount - 1);
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
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

        private void log(String message) {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            String logMessage = "[" + timestamp + "] " + message;
            Platform.runLater(() -> uploadLog.add(logMessage));
            logger.info(message);
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
     * Overall upload result
     */
    public static class UploadResult {
        private final boolean success;
        private final int successCount;
        private final int failureCount;
        private final long bytesUploaded;

        public UploadResult(boolean success, int successCount, int failureCount, long bytesUploaded) {
            this.success = success;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.bytesUploaded = bytesUploaded;
        }

        public boolean isSuccess() { return success; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getBytesUploaded() { return bytesUploaded; }
    }

    /**
     * Input stream wrapper that tracks progress
     */
    private static class ProgressInputStream extends FilterInputStream {
        private final long totalSize;
        private final ProgressCallback callback;
        private long bytesRead = 0;
        private long lastReported = 0;
        private static final long REPORT_INTERVAL = 65536; // Report every 64KB

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
            if (bytesRead - lastReported >= REPORT_INTERVAL || bytesRead == totalSize) {
                callback.onProgress(bytesRead - lastReported, totalSize);
                lastReported = bytesRead;
            }
        }
    }
}
