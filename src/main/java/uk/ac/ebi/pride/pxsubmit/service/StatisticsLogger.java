package uk.ac.ebi.pride.pxsubmit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics.FileTransferStat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Dedicated service for logging transfer statistics in a structured, pipe-delimited format.
 * This format enables easy parsing and analysis of upload performance data.
 *
 * Log format specification:
 * - SESSION_START|sessionId|method|timestamp|totalFiles|totalBytes
 * - FILE_START|sessionId|fileName|fileSize|timestamp
 * - FILE_COMPLETE|sessionId|fileName|fileSize|duration_ms|rate_MBps|success|retries|error
 * - SESSION_END|sessionId|method|totalDuration_ms|filesSuccess|filesFailed|bytesTransferred|avgRate_MBps
 */
public class StatisticsLogger {

    private static final Logger statsLogger = LoggerFactory.getLogger("TRANSFER_STATISTICS");
    private static final Logger appLogger = LoggerFactory.getLogger(StatisticsLogger.class);

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(ZoneId.of("UTC"));

    private static final String DELIMITER = "|";

    private StatisticsLogger() {
        // Utility class, prevent instantiation
    }

    /**
     * Log the start of a transfer session.
     *
     * @param stats The transfer statistics object
     */
    public static void logSessionStart(TransferStatistics stats) {
        String message = String.join(DELIMITER,
                "SESSION_START",
                stats.getSessionId(),
                stats.getUploadMethod(),
                formatTimestamp(stats.getStartTime()),
                String.valueOf(stats.getTotalFiles()),
                String.valueOf(stats.getTotalBytes())
        );
        statsLogger.info(message);
        appLogger.debug("Transfer session started: {}", stats.getSessionId());
    }

    /**
     * Log the start of a file transfer.
     *
     * @param fileStat The file transfer statistics
     */
    public static void logFileStart(FileTransferStat fileStat) {
        String message = String.join(DELIMITER,
                "FILE_START",
                fileStat.getSessionId(),
                sanitizeFileName(fileStat.getFileName()),
                String.valueOf(fileStat.getFileSize()),
                formatTimestamp(fileStat.getStartTime())
        );
        statsLogger.info(message);
    }

    /**
     * Log the completion of a file transfer (success or failure).
     *
     * @param fileStat The file transfer statistics
     */
    public static void logFileComplete(FileTransferStat fileStat) {
        String errorField = fileStat.getError() != null ?
                sanitizeError(fileStat.getError()) : "";

        String message = String.join(DELIMITER,
                "FILE_COMPLETE",
                fileStat.getSessionId(),
                sanitizeFileName(fileStat.getFileName()),
                String.valueOf(fileStat.getFileSize()),
                String.valueOf(fileStat.getDurationMs()),
                String.format("%.3f", fileStat.getRateMBps()),
                String.valueOf(fileStat.isSuccess()),
                String.valueOf(fileStat.getRetries()),
                errorField
        );
        statsLogger.info(message);

        if (fileStat.isSuccess()) {
            appLogger.debug("File transfer completed: {} ({}ms, {:.2f} MB/s)",
                    fileStat.getFileName(), fileStat.getDurationMs(), fileStat.getRateMBps());
        } else {
            appLogger.warn("File transfer failed: {} - {}", fileStat.getFileName(), fileStat.getError());
        }
    }

    /**
     * Log the completion of a transfer session.
     *
     * @param stats The transfer statistics object
     */
    public static void logSessionSummary(TransferStatistics stats) {
        String message = String.join(DELIMITER,
                "SESSION_END",
                stats.getSessionId(),
                stats.getUploadMethod(),
                String.valueOf(stats.getTotalDurationMs()),
                String.valueOf(stats.getFilesCompleted()),
                String.valueOf(stats.getFilesFailed()),
                String.valueOf(stats.getBytesTransferred()),
                String.format("%.3f", stats.getAverageRateMBps())
        );
        statsLogger.info(message);

        // Also log a human-readable summary to the application logger
        appLogger.info("Transfer session {} completed: {}/{} files successful, {} failed, " +
                        "{} bytes transferred in {}ms (avg {:.2f} MB/s)",
                stats.getSessionId(),
                stats.getFilesCompleted(),
                stats.getTotalFiles(),
                stats.getFilesFailed(),
                stats.getBytesTransferred(),
                stats.getTotalDurationMs(),
                stats.getAverageRateMBps());
    }

    /**
     * Log a custom event related to a session.
     *
     * @param sessionId The session ID
     * @param eventType The event type (e.g., "RETRY", "TIMEOUT", "CONNECTION_ERROR")
     * @param details Additional details about the event
     */
    public static void logEvent(String sessionId, String eventType, String details) {
        String message = String.join(DELIMITER,
                eventType,
                sessionId,
                formatTimestamp(Instant.now()),
                sanitizeError(details)
        );
        statsLogger.info(message);
    }

    /**
     * Log a connection attempt.
     *
     * @param sessionId The session ID
     * @param host The target host
     * @param port The target port
     * @param success Whether connection was successful
     * @param durationMs Connection duration in milliseconds
     */
    public static void logConnectionAttempt(String sessionId, String host, int port,
                                            boolean success, long durationMs) {
        String message = String.join(DELIMITER,
                "CONNECTION",
                sessionId,
                host,
                String.valueOf(port),
                String.valueOf(success),
                String.valueOf(durationMs),
                formatTimestamp(Instant.now())
        );
        statsLogger.info(message);
    }

    /**
     * Log a retry attempt for a file.
     *
     * @param sessionId The session ID
     * @param fileName The file being retried
     * @param retryNumber The retry attempt number
     * @param reason The reason for retry
     */
    public static void logRetry(String sessionId, String fileName, int retryNumber, String reason) {
        String message = String.join(DELIMITER,
                "RETRY",
                sessionId,
                sanitizeFileName(fileName),
                String.valueOf(retryNumber),
                formatTimestamp(Instant.now()),
                sanitizeError(reason)
        );
        statsLogger.info(message);
    }

    /**
     * Format an Instant to a standardized timestamp string.
     */
    private static String formatTimestamp(Instant instant) {
        return TIMESTAMP_FORMATTER.format(instant);
    }

    /**
     * Sanitize a filename for safe inclusion in the log (remove pipe characters).
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        return fileName.replace("|", "_").replace("\n", " ").replace("\r", "");
    }

    /**
     * Sanitize an error message for safe inclusion in the log.
     */
    private static String sanitizeError(String error) {
        if (error == null) {
            return "";
        }
        return error.replace("|", ";").replace("\n", " ").replace("\r", "");
    }
}
