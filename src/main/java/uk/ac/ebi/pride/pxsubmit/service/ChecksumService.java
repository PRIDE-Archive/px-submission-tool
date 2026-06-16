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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    public static final String CHECKSUM_FILE_NAME = "checksum.txt";
    public static final String CHECKSUM_FILE_FORMAT = "<File_name><TAB><checksum>";

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
                .filter(f -> !isChecksumFile(f))
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
        ChecksumValidationResult entryValidation = validateChecksumEntries(checksums);
        if (!entryValidation.valid()) {
            throw new IOException("Invalid checksum entries:\n" + entryValidation.formattedDetails());
        }

        File checksumFile = new File(directory, CHECKSUM_FILE_NAME);

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(
                checksumFile.toPath(), StandardCharsets.UTF_8))) {
            for (Map.Entry<DataFile, String> entry : orderedChecksumEntries(checksums)) {
                DataFile dataFile = entry.getKey();
                String checksum = entry.getValue();
                // Format: filename TAB checksum
                writer.println(getFileName(dataFile) + "\t" + checksum);
            }
        }

        logger.info("Checksum file written: {}", checksumFile.getAbsolutePath());
        return checksumFile;
    }

    /**
     * Write checksum.txt and verify that its contents match the selected upload files.
     */
    public static File writeChecksumFile(
            Map<DataFile, String> checksums,
            Collection<DataFile> selectedFiles,
            File directory
    ) throws IOException {
        ChecksumValidationResult coverageValidation = validateChecksumCoverage(selectedFiles, checksums);
        if (!coverageValidation.valid()) {
            throw new IOException("Checksum coverage validation failed:\n" +
                    coverageValidation.formattedDetails());
        }

        File checksumFile = writeChecksumFile(checksums, directory);
        ChecksumValidationResult fileValidation =
                validateChecksumFileCoverage(checksumFile, selectedFiles);
        if (!fileValidation.valid()) {
            throw new IOException("Generated checksum.txt validation failed:\n" +
                    fileValidation.formattedDetails());
        }
        return checksumFile;
    }

    /**
     * Verify that every selected upload file has one checksum entry and every
     * checksum entry belongs to a selected upload file.
     */
    public static ChecksumValidationResult validateChecksumCoverage(
            Collection<DataFile> selectedFiles,
            Map<DataFile, String> checksums
    ) {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> selectedNames = countSelectedFileNames(selectedFiles, errors);
        Map<String, Integer> checksumNames = countChecksumFileNames(checksums, errors);

        for (String selectedName : selectedNames.keySet()) {
            if (!checksumNames.containsKey(selectedName)) {
                errors.add("Missing checksum entry for selected file: " + selectedName);
            }
        }

        for (String checksumName : checksumNames.keySet()) {
            if (!selectedNames.containsKey(checksumName)) {
                errors.add("Checksum entry does not match a selected upload file: " + checksumName);
            }
        }

        return new ChecksumValidationResult(List.copyOf(errors));
    }

    /**
     * Verify a generated checksum.txt file against the selected upload files.
     */
    public static ChecksumValidationResult validateChecksumFileCoverage(
            File checksumFile,
            Collection<DataFile> selectedFiles
    ) throws IOException {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> selectedNames = countSelectedFileNames(selectedFiles, errors);
        Map<String, Integer> checksumFileNames = new LinkedHashMap<>();

        if (checksumFile == null || !checksumFile.isFile()) {
            errors.add("checksum.txt was not created.");
            return new ChecksumValidationResult(List.copyOf(errors));
        }

        List<String> lines = Files.readAllLines(checksumFile.toPath(), StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;
            if (line == null || line.isBlank()) {
                errors.add("Line " + lineNumber + " is blank. Expected " + CHECKSUM_FILE_FORMAT + ".");
                continue;
            }

            String[] parts = line.split("\t", -1);
            if (parts.length != 2) {
                errors.add("Line " + lineNumber + " has invalid format. Expected " +
                        CHECKSUM_FILE_FORMAT + ".");
                continue;
            }

            String fileName = parts[0];
            String checksum = parts[1];
            if (fileName.isBlank()) {
                errors.add("Line " + lineNumber + " has an empty file name.");
                continue;
            }
            if (checksum.isBlank()) {
                errors.add("Line " + lineNumber + " has an empty checksum for file: " + fileName);
            }
            checksumFileNames.merge(fileName, 1, Integer::sum);
        }

        addDuplicateErrors("checksum.txt", checksumFileNames, errors);

        for (String selectedName : selectedNames.keySet()) {
            if (!checksumFileNames.containsKey(selectedName)) {
                errors.add("checksum.txt is missing selected upload file: " + selectedName);
            }
        }

        for (String checksumName : checksumFileNames.keySet()) {
            if (!selectedNames.containsKey(checksumName)) {
                errors.add("checksum.txt contains a file not selected for upload: " + checksumName);
            }
        }

        return new ChecksumValidationResult(List.copyOf(errors));
    }

    public static Optional<String> findChecksumForFile(Map<DataFile, String> checksums, DataFile dataFile) {
        if (checksums == null || dataFile == null) {
            return Optional.empty();
        }

        String directMatch = checksums.get(dataFile);
        if (directMatch != null) {
            return Optional.of(directMatch);
        }

        String targetFileName = getFileName(dataFile);
        if (targetFileName.isBlank()) {
            return Optional.empty();
        }

        return checksums.entrySet().stream()
                .filter(entry -> targetFileName.equals(getFileName(entry.getKey())))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    public static boolean isChecksumFile(DataFile dataFile) {
        return CHECKSUM_FILE_NAME.equalsIgnoreCase(getFileName(dataFile));
    }

    public record ChecksumValidationResult(List<String> errors) {
        public boolean valid() {
            return errors.isEmpty();
        }

        public String formattedDetails() {
            return String.join("\n", errors);
        }
    }

    private static ChecksumValidationResult validateChecksumEntries(Map<DataFile, String> checksums) {
        List<String> errors = new ArrayList<>();
        countChecksumFileNames(checksums, errors);
        return new ChecksumValidationResult(List.copyOf(errors));
    }

    private static Map<String, Integer> countSelectedFileNames(
            Collection<DataFile> selectedFiles,
            List<String> errors
    ) {
        Map<String, Integer> names = new LinkedHashMap<>();
        if (selectedFiles == null) {
            return names;
        }

        for (DataFile dataFile : selectedFiles) {
            if (isChecksumFile(dataFile)) {
                continue;
            }

            String fileName = getFileName(dataFile);
            if (fileName.isBlank()) {
                errors.add("Selected upload file has an empty file name.");
                continue;
            }
            names.merge(fileName, 1, Integer::sum);
        }

        addDuplicateErrors("selected upload files", names, errors);
        return names;
    }

    private static Map<String, Integer> countChecksumFileNames(
            Map<DataFile, String> checksums,
            List<String> errors
    ) {
        Map<String, Integer> names = new LinkedHashMap<>();
        if (checksums == null || checksums.isEmpty()) {
            return names;
        }

        for (Map.Entry<DataFile, String> entry : checksums.entrySet()) {
            DataFile dataFile = entry.getKey();
            String fileName = getFileName(dataFile);
            String checksum = entry.getValue();

            if (fileName.isBlank()) {
                errors.add("Checksum entry has an empty file name.");
                continue;
            }
            if (CHECKSUM_FILE_NAME.equalsIgnoreCase(fileName)) {
                errors.add("checksum.txt must not include a checksum entry for itself.");
                continue;
            }
            if (checksum == null || checksum.isBlank()) {
                errors.add("Missing checksum value for file: " + fileName);
            }
            names.merge(fileName, 1, Integer::sum);
        }

        addDuplicateErrors("checksum entries", names, errors);
        return names;
    }

    private static void addDuplicateErrors(String source, Map<String, Integer> names, List<String> errors) {
        for (Map.Entry<String, Integer> entry : names.entrySet()) {
            if (entry.getValue() > 1) {
                errors.add("Duplicate file name in " + source + ": " + entry.getKey());
            }
        }
    }

    private static List<Map.Entry<DataFile, String>> orderedChecksumEntries(Map<DataFile, String> checksums) {
        if (checksums == null || checksums.isEmpty()) {
            return List.of();
        }

        return checksums.entrySet().stream()
                .sorted(Comparator.comparing(entry -> getFileName(entry.getKey()), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static String getFileName(DataFile dataFile) {
        if (dataFile == null) {
            return "";
        }
        if (dataFile.getFileName() != null && !dataFile.getFileName().isBlank()) {
            return dataFile.getFileName();
        }
        if (dataFile.getFile() != null) {
            return dataFile.getFile().getName();
        }
        return "";
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
