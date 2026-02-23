package uk.ac.ebi.pride.pxsubmit.model;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Model class for tracking transfer statistics across upload sessions.
 * Thread-safe for use with concurrent uploads.
 */
public class TransferStatistics {

    private final String sessionId;
    private final String uploadMethod;
    private final Instant startTime;
    private volatile Instant endTime;

    private final int totalFiles;
    private final long totalBytes;

    private final AtomicInteger filesCompleted = new AtomicInteger(0);
    private final AtomicInteger filesFailed = new AtomicInteger(0);
    private final AtomicLong bytesTransferred = new AtomicLong(0);

    private final ConcurrentMap<String, FileTransferStat> fileStats = new ConcurrentHashMap<>();
    private final List<FileTransferStat> completedFileStats = Collections.synchronizedList(new ArrayList<>());

    /**
     * Create a new transfer statistics session.
     *
     * @param uploadMethod The upload method (FTP or ASPERA)
     * @param totalFiles Total number of files to transfer
     * @param totalBytes Total bytes to transfer
     */
    public TransferStatistics(String uploadMethod, int totalFiles, long totalBytes) {
        this.sessionId = UUID.randomUUID().toString().substring(0, 8);
        this.uploadMethod = uploadMethod;
        this.totalFiles = totalFiles;
        this.totalBytes = totalBytes;
        this.startTime = Instant.now();
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getUploadMethod() { return uploadMethod; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public int getTotalFiles() { return totalFiles; }
    public long getTotalBytes() { return totalBytes; }
    public int getFilesCompleted() { return filesCompleted.get(); }
    public int getFilesFailed() { return filesFailed.get(); }
    public long getBytesTransferred() { return bytesTransferred.get(); }

    /**
     * Mark the session as complete.
     */
    public void setSessionComplete() {
        this.endTime = Instant.now();
    }

    /**
     * Get the total session duration in milliseconds.
     * Returns -1 if session is still running.
     */
    public long getTotalDurationMs() {
        if (endTime == null) {
            return Duration.between(startTime, Instant.now()).toMillis();
        }
        return Duration.between(startTime, endTime).toMillis();
    }

    /**
     * Get the average transfer rate in MB/s.
     * Returns 0 if no data transferred or duration is zero.
     */
    public double getAverageRateMBps() {
        long durationMs = getTotalDurationMs();
        if (durationMs <= 0 || bytesTransferred.get() <= 0) {
            return 0.0;
        }
        double seconds = durationMs / 1000.0;
        double megabytes = bytesTransferred.get() / (1024.0 * 1024.0);
        return megabytes / seconds;
    }

    /**
     * Record the start of a file transfer.
     *
     * @param fileName Name of the file
     * @param fileSize Size of the file in bytes
     * @return The FileTransferStat object for tracking
     */
    public FileTransferStat startFile(String fileName, long fileSize) {
        FileTransferStat stat = new FileTransferStat(sessionId, fileName, fileSize);
        fileStats.put(fileName, stat);
        return stat;
    }

    /**
     * Record the successful completion of a file transfer.
     *
     * @param fileName Name of the file
     * @param bytesTransferredForFile Actual bytes transferred
     */
    public void completeFile(String fileName, long bytesTransferredForFile) {
        FileTransferStat stat = fileStats.get(fileName);
        if (stat != null) {
            stat.complete(true, null, bytesTransferredForFile);
            completedFileStats.add(stat);
            filesCompleted.incrementAndGet();
            bytesTransferred.addAndGet(bytesTransferredForFile);
        }
    }

    /**
     * Record a failed file transfer.
     *
     * @param fileName Name of the file
     * @param error Error message
     * @param retries Number of retries attempted
     */
    public void failFile(String fileName, String error, int retries) {
        FileTransferStat stat = fileStats.get(fileName);
        if (stat != null) {
            stat.setRetries(retries);
            stat.complete(false, error, 0);
            completedFileStats.add(stat);
            filesFailed.incrementAndGet();
        } else {
            // File not started, create a failed entry
            FileTransferStat failedStat = new FileTransferStat(sessionId, fileName, 0);
            failedStat.setRetries(retries);
            failedStat.complete(false, error, 0);
            completedFileStats.add(failedStat);
            filesFailed.incrementAndGet();
        }
    }

    /**
     * Get statistics for a specific file.
     *
     * @param fileName Name of the file
     * @return FileTransferStat or null if not found
     */
    public FileTransferStat getFileStat(String fileName) {
        return fileStats.get(fileName);
    }

    /**
     * Get all completed file statistics.
     *
     * @return Unmodifiable list of completed file stats
     */
    public List<FileTransferStat> getCompletedFileStats() {
        return Collections.unmodifiableList(new ArrayList<>(completedFileStats));
    }

    /**
     * Check if all files have been processed (completed or failed).
     */
    public boolean isComplete() {
        return (filesCompleted.get() + filesFailed.get()) >= totalFiles;
    }

    /**
     * Check if the session was successful (all files completed without failures).
     */
    public boolean isSuccessful() {
        return isComplete() && filesFailed.get() == 0;
    }

    @Override
    public String toString() {
        return String.format(
            "TransferStatistics[session=%s, method=%s, files=%d/%d, failed=%d, bytes=%d/%d, rate=%.2f MB/s]",
            sessionId, uploadMethod, filesCompleted.get(), totalFiles, filesFailed.get(),
            bytesTransferred.get(), totalBytes, getAverageRateMBps()
        );
    }

    /**
     * Statistics for an individual file transfer.
     */
    public static class FileTransferStat {
        private final String sessionId;
        private final String fileName;
        private final long fileSize;
        private final Instant startTime;
        private volatile Instant endTime;
        private volatile boolean success;
        private volatile String error;
        private volatile int retries;
        private volatile long bytesTransferred;

        public FileTransferStat(String sessionId, String fileName, long fileSize) {
            this.sessionId = sessionId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.startTime = Instant.now();
            this.retries = 0;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public int getRetries() { return retries; }
        public long getBytesTransferred() { return bytesTransferred; }

        /**
         * Set the number of retry attempts.
         */
        public void setRetries(int retries) {
            this.retries = retries;
        }

        /**
         * Increment retry count.
         */
        public void incrementRetries() {
            this.retries++;
        }

        /**
         * Mark the file transfer as complete.
         *
         * @param success Whether transfer was successful
         * @param error Error message if failed
         * @param bytesTransferred Actual bytes transferred
         */
        public void complete(boolean success, String error, long bytesTransferred) {
            this.endTime = Instant.now();
            this.success = success;
            this.error = error;
            this.bytesTransferred = bytesTransferred;
        }

        /**
         * Get the transfer duration in milliseconds.
         */
        public long getDurationMs() {
            if (endTime == null) {
                return Duration.between(startTime, Instant.now()).toMillis();
            }
            return Duration.between(startTime, endTime).toMillis();
        }

        /**
         * Get the transfer rate in MB/s.
         */
        public double getRateMBps() {
            long durationMs = getDurationMs();
            if (durationMs <= 0 || bytesTransferred <= 0) {
                return 0.0;
            }
            double seconds = durationMs / 1000.0;
            double megabytes = bytesTransferred / (1024.0 * 1024.0);
            return megabytes / seconds;
        }

        @Override
        public String toString() {
            return String.format(
                "FileTransferStat[file=%s, size=%d, duration=%dms, rate=%.2f MB/s, success=%s, retries=%d]",
                fileName, fileSize, getDurationMs(), getRateMBps(), success, retries
            );
        }
    }
}
