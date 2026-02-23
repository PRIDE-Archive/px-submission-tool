package uk.ac.ebi.pride.pxsubmit.service;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.model.DataFile;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Service for calculating file checksums asynchronously.
 * Uses multi-threading for parallel checksum calculation.
 *
 * Features:
 * - Multi-threaded checksum calculation
 * - Progress reporting per file
 * - Cancellation support
 * - Caching of calculated checksums
 * - Writes checksum.txt file
 *
 * Usage:
 * <pre>
 * ChecksumService service = new ChecksumService(files);
 *
 * // Bind to progress
 * progressBar.progressProperty().bind(service.progressProperty());
 * statusLabel.textProperty().bind(service.messageProperty());
 *
 * service.setOnSucceeded(e -> {
 *     Map<DataFile, String> checksums = service.getValue();
 *     // Checksums calculated
 * });
 *
 * service.start();
 * </pre>
 */
public class ChecksumService extends Service<Map<DataFile, String>> {

    private static final Logger logger = LoggerFactory.getLogger(ChecksumService.class);

    // Number of threads for parallel processing
    private static final int DEFAULT_THREAD_COUNT = Math.max(2,
        Runtime.getRuntime().availableProcessors() / 2);

    // Bounded LRU cache of already calculated checksums (file path -> checksum)
    private static final int MAX_CACHE_SIZE = 500;
    private static final Map<String, String> checksumCache = Collections.synchronizedMap(
        new LinkedHashMap<String, String>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        });

    private final ObservableList<DataFile> files;
    private final int threadCount;

    // Additional progress info
    private final StringProperty currentFileName = new SimpleStringProperty();
    private final IntegerProperty filesProcessed = new SimpleIntegerProperty(0);
    private final IntegerProperty totalFiles = new SimpleIntegerProperty(0);

    public ChecksumService(ObservableList<DataFile> files) {
        this(files, DEFAULT_THREAD_COUNT);
    }

    public ChecksumService(ObservableList<DataFile> files, int threadCount) {
        this.files = files;
        this.threadCount = threadCount;
    }

    @Override
    protected Task<Map<DataFile, String>> createTask() {
        return new ChecksumTask(files, threadCount);
    }

    /**
     * The checksum calculation task
     */
    private class ChecksumTask extends Task<Map<DataFile, String>> {

        private final List<DataFile> taskFiles;
        private final int threads;

        public ChecksumTask(List<DataFile> files, int threads) {
            this.taskFiles = files;
            this.threads = threads;
        }

        @Override
        protected Map<DataFile, String> call() throws Exception {
            logger.info("Starting checksum calculation for {} files with {} threads",
                taskFiles.size(), threads);

            Map<DataFile, String> results = new ConcurrentHashMap<>();

            // Filter out checksum.txt if present
            List<DataFile> filesToProcess = taskFiles.stream()
                .filter(f -> !"checksum.txt".equals(f.getFileName()))
                .toList();

            int total = filesToProcess.size();
            Platform.runLater(() -> totalFiles.set(total));

            if (total == 0) {
                updateMessage("No files to process");
                return results;
            }

            ExecutorService executor = Executors.newFixedThreadPool(threads, r -> {
                Thread t = new Thread(r, "Checksum-Worker");
                t.setDaemon(true);
                return t;
            });

            try {
                // Submit all tasks
                Map<DataFile, Future<String>> futures = new HashMap<>();
                for (DataFile file : filesToProcess) {
                    futures.put(file, executor.submit(() -> calculateChecksum(file)));
                }

                // Collect results
                int processed = 0;
                for (Map.Entry<DataFile, Future<String>> entry : futures.entrySet()) {
                    if (isCancelled()) {
                        logger.info("Checksum calculation cancelled");
                        break;
                    }

                    DataFile dataFile = entry.getKey();
                    try {
                        String checksum = entry.getValue().get(5, TimeUnit.MINUTES);
                        if (checksum != null) {
                            results.put(dataFile, checksum);
                        }
                    } catch (TimeoutException e) {
                        logger.error("Timeout calculating checksum for: {}", dataFile.getFileName());
                    } catch (Exception e) {
                        logger.error("Error calculating checksum for {}: {}",
                            dataFile.getFileName(), e.getMessage());
                    }

                    processed++;
                    final int current = processed;
                    Platform.runLater(() -> filesProcessed.set(current));
                    updateProgress(processed, total);
                    updateMessage(String.format("Processed %d of %d files", processed, total));
                }

                logger.info("Checksum calculation completed. Processed {} files", results.size());

            } finally {
                executor.shutdown();
            }

            return results;
        }

        private String calculateChecksum(DataFile dataFile) throws IOException {
            File file = dataFile.getFile();
            String filePath = file.getAbsolutePath();

            // Update current file name
            String fileName = file.getName();
            Platform.runLater(() -> currentFileName.set(fileName));
            updateMessage("Calculating checksum: " + fileName);

            // Check cache
            if (checksumCache.containsKey(filePath)) {
                logger.debug("Using cached checksum for: {}", fileName);
                return checksumCache.get(filePath);
            }

            // Validate file
            if (!file.exists()) {
                throw new IOException("File does not exist: " + filePath);
            }
            if (!file.canRead()) {
                throw new IOException("Cannot read file: " + filePath);
            }
            if (file.isDirectory()) {
                throw new IOException("Cannot calculate checksum for directory: " + filePath);
            }

            // Calculate SHA-1 checksum
            logger.debug("Calculating SHA-1 checksum for: {}", fileName);
            try (FileInputStream fis = new FileInputStream(file)) {
                String checksum = DigestUtils.sha1Hex(fis);

                // Cache the result
                checksumCache.put(filePath, checksum);

                logger.debug("Checksum calculated for {}: {}", fileName, checksum);
                return checksum;
            }
        }
    }

    /**
     * Write checksum.txt file for submission
     */
    public static File writeChecksumFile(Map<DataFile, String> checksums, File directory) throws IOException {
        File checksumFile = new File(directory, "checksum.txt");

        try (PrintWriter writer = new PrintWriter(new FileWriter(checksumFile))) {
            for (Map.Entry<DataFile, String> entry : checksums.entrySet()) {
                DataFile dataFile = entry.getKey();
                String checksum = entry.getValue();
                // Format: checksum TAB filename
                writer.println(checksum + "\t" + dataFile.getFileName());
            }
        }

        logger.info("Checksum file written: {}", checksumFile.getAbsolutePath());
        return checksumFile;
    }

    /**
     * Clear the checksum cache
     */
    public static void clearCache() {
        checksumCache.clear();
    }

    /**
     * Get cache size
     */
    public static int getCacheSize() {
        return checksumCache.size();
    }

    // ==================== Property Accessors ====================

    public ReadOnlyStringProperty currentFileNameProperty() {
        return currentFileName;
    }

    public String getCurrentFileName() {
        return currentFileName.get();
    }

    public ReadOnlyIntegerProperty filesProcessedProperty() {
        return filesProcessed;
    }

    public int getFilesProcessed() {
        return filesProcessed.get();
    }

    public ReadOnlyIntegerProperty totalFilesProperty() {
        return totalFiles;
    }

    public int getTotalFiles() {
        return totalFiles.get();
    }
}
