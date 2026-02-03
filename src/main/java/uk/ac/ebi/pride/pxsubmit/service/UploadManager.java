package uk.ac.ebi.pride.pxsubmit.service;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileWriter;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unified upload manager that coordinates FTP and Aspera uploads.
 *
 * Features:
 * - Automatic method selection based on availability
 * - Submission summary file creation
 * - Resume support for interrupted uploads
 * - Progress aggregation from underlying services
 * - Unified error handling
 */
public class UploadManager extends Service<UploadManager.UploadResult> {

    private static final Logger logger = LoggerFactory.getLogger(UploadManager.class);

    private static final String SUBMISSION_SUMMARY_FILE = "submission.px";

    // Configuration
    private final Submission submission;
    private final UploadDetail uploadDetail;
    private final UploadMethod preferredMethod;
    private final boolean trainingMode;

    // Tracking uploaded files for resume support
    private final Set<String> uploadedFileNames = new HashSet<>();
    private boolean summaryFileUploaded = false;

    // Progress properties
    private final IntegerProperty totalFiles = new SimpleIntegerProperty(0);
    private final IntegerProperty uploadedFiles = new SimpleIntegerProperty(0);
    private final LongProperty totalBytes = new SimpleLongProperty(0);
    private final LongProperty uploadedBytes = new SimpleLongProperty(0);
    private final StringProperty currentFileName = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");
    private final DoubleProperty overallProgress = new SimpleDoubleProperty(0);
    private final ObjectProperty<UploadMethod> activeMethod = new SimpleObjectProperty<>();

    // Upload log
    private final ObservableList<String> uploadLog = FXCollections.observableArrayList();

    public UploadManager(Submission submission, UploadDetail uploadDetail,
                         UploadMethod preferredMethod, boolean trainingMode) {
        this.submission = submission;
        this.uploadDetail = uploadDetail;
        this.preferredMethod = preferredMethod;
        this.trainingMode = trainingMode;

        // Calculate totals
        List<DataFile> files = submission.getDataFiles();
        this.totalFiles.set(files.size() + 1); // +1 for submission summary
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
        return new UploadTask();
    }

    // Property accessors
    public IntegerProperty totalFilesProperty() { return totalFiles; }
    public IntegerProperty uploadedFilesProperty() { return uploadedFiles; }
    public LongProperty totalBytesProperty() { return totalBytes; }
    public LongProperty uploadedBytesProperty() { return uploadedBytes; }
    public StringProperty currentFileNameProperty() { return currentFileName; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public DoubleProperty overallProgressProperty() { return overallProgress; }
    public ObjectProperty<UploadMethod> activeMethodProperty() { return activeMethod; }
    public ObservableList<String> getUploadLog() { return uploadLog; }

    /**
     * Mark a file as already uploaded (for resume support)
     */
    public void markFileUploaded(String fileName) {
        uploadedFileNames.add(fileName);
    }

    /**
     * Check if a file was already uploaded
     */
    public boolean isFileUploaded(String fileName) {
        return uploadedFileNames.contains(fileName);
    }

    /**
     * Mark summary file as uploaded
     */
    public void markSummaryFileUploaded() {
        summaryFileUploaded = true;
    }

    /**
     * Main upload task
     */
    private class UploadTask extends Task<UploadResult> {

        @Override
        protected UploadResult call() throws Exception {
            log("Starting upload process...");

            if (trainingMode) {
                log("TRAINING MODE: Files will not be uploaded to production");
                return simulateUpload();
            }

            // Determine upload method
            UploadMethod method = determineUploadMethod();
            Platform.runLater(() -> activeMethod.set(method));
            log("Using upload method: " + method);

            // Prepare files to upload (filter already uploaded)
            List<DataFile> filesToUpload = prepareFilesToUpload();
            log("Files to upload: " + filesToUpload.size());

            if (filesToUpload.isEmpty() && summaryFileUploaded) {
                log("All files already uploaded");
                return new UploadResult(true, submission.getDataFiles().size(), 0,
                        method, "All files were already uploaded");
            }

            // Create submission summary file
            File summaryFile = null;
            if (!summaryFileUploaded) {
                summaryFile = createSubmissionSummaryFile();
                if (summaryFile != null) {
                    DataFile summaryDataFile = new DataFile();
                    summaryDataFile.setFile(summaryFile);
                    filesToUpload.add(0, summaryDataFile);
                }
            }

            // Execute upload
            try {
                switch (method) {
                    case ASPERA:
                        return uploadWithAspera(filesToUpload);
                    case FTP:
                    default:
                        return uploadWithFtp(filesToUpload);
                }
            } finally {
                // Cleanup temp files
                if (summaryFile != null && summaryFile.exists()) {
                    try {
                        Files.delete(summaryFile.toPath());
                        Files.delete(summaryFile.getParentFile().toPath());
                    } catch (IOException e) {
                        logger.warn("Failed to cleanup temp files", e);
                    }
                }
            }
        }

        private UploadMethod determineUploadMethod() {
            if (preferredMethod == UploadMethod.ASPERA) {
                String ascpPath = AsperaUploadService.findAscpPath();
                if (ascpPath != null) {
                    return UploadMethod.ASPERA;
                }
                log("Aspera not available, falling back to FTP");
            }
            return UploadMethod.FTP;
        }

        private List<DataFile> prepareFilesToUpload() {
            List<DataFile> toUpload = new ArrayList<>();
            for (DataFile file : submission.getDataFiles()) {
                String fileName = file.getFileName();
                if (!uploadedFileNames.contains(fileName)) {
                    toUpload.add(file);
                } else {
                    log("Skipping (already uploaded): " + fileName);
                }
            }
            return toUpload;
        }

        private File createSubmissionSummaryFile() {
            try {
                log("Creating submission summary file...");
                updateStatus("Creating submission summary...");

                // Create temp directory
                SecureRandom random = new SecureRandom();
                Path tempDir = Files.createTempDirectory("px-submit-" + random.nextLong());
                File summaryFile = tempDir.resolve(SUBMISSION_SUMMARY_FILE).toFile();

                // Write submission
                SubmissionFileWriter.write(submission, summaryFile);
                log("Created: " + summaryFile.getAbsolutePath());

                return summaryFile;
            } catch (SubmissionFileException | IOException e) {
                logger.error("Failed to create submission summary file", e);
                log("ERROR: Failed to create submission summary: " + e.getMessage());
                return null;
            }
        }

        private UploadResult uploadWithFtp(List<DataFile> files) throws Exception {
            log("Starting FTP upload...");
            updateStatus("Uploading via FTP...");

            FtpUploadService ftpService = new FtpUploadService(files, uploadDetail);

            // Bind progress
            ftpService.uploadedFilesProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> {
                    uploadedFiles.set(newVal.intValue());
                    overallProgress.set((double) newVal.intValue() / totalFiles.get());
                });
            });

            ftpService.uploadedBytesProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> uploadedBytes.set(newVal.longValue()));
            });

            ftpService.currentFileNameProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> currentFileName.set(newVal));
            });

            // Forward logs
            ftpService.getUploadLog().addListener(
                    (javafx.collections.ListChangeListener.Change<? extends String> c) -> {
                        while (c.next()) {
                            if (c.wasAdded()) {
                                Platform.runLater(() -> uploadLog.addAll(c.getAddedSubList()));
                            }
                        }
                    });

            // Run synchronously
            ftpService.start();

            // Wait for completion
            while (ftpService.isRunning()) {
                if (isCancelled()) {
                    ftpService.cancel();
                    break;
                }
                Thread.sleep(100);
            }

            FtpUploadService.UploadResult ftpResult = ftpService.getValue();

            if (ftpResult != null && ftpResult.isSuccess()) {
                summaryFileUploaded = true;
                for (DataFile file : files) {
                    uploadedFileNames.add(file.getFileName());
                }
            }

            return new UploadResult(
                    ftpResult != null && ftpResult.isSuccess(),
                    ftpResult != null ? ftpResult.getSuccessCount() : 0,
                    ftpResult != null ? ftpResult.getFailureCount() : files.size(),
                    UploadMethod.FTP,
                    ftpResult != null && !ftpResult.isSuccess() ? "FTP upload failed" : null
            );
        }

        private UploadResult uploadWithAspera(List<DataFile> files) throws Exception {
            log("Starting Aspera upload...");
            updateStatus("Uploading via Aspera...");

            String ascpPath = AsperaUploadService.findAscpPath();
            if (ascpPath == null) {
                log("ERROR: Aspera binary not found, falling back to FTP");
                return uploadWithFtp(files);
            }

            AsperaUploadService asperaService = new AsperaUploadService(files, uploadDetail, ascpPath);

            // Bind progress
            asperaService.uploadedFilesProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> {
                    uploadedFiles.set(newVal.intValue());
                    overallProgress.set((double) newVal.intValue() / totalFiles.get());
                });
            });

            asperaService.uploadedBytesProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> uploadedBytes.set(newVal.longValue()));
            });

            asperaService.currentFileNameProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> currentFileName.set(newVal));
            });

            asperaService.statusMessageProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> statusMessage.set(newVal));
            });

            // Forward logs
            asperaService.getUploadLog().addListener(
                    (javafx.collections.ListChangeListener.Change<? extends String> c) -> {
                        while (c.next()) {
                            if (c.wasAdded()) {
                                Platform.runLater(() -> uploadLog.addAll(c.getAddedSubList()));
                            }
                        }
                    });

            // Run
            asperaService.start();

            // Wait for completion
            while (asperaService.isRunning()) {
                if (isCancelled()) {
                    asperaService.cancel();
                    break;
                }
                Thread.sleep(100);
            }

            AsperaUploadService.UploadResult asperaResult = asperaService.getValue();

            if (asperaResult != null && asperaResult.isSuccess()) {
                summaryFileUploaded = true;
                for (DataFile file : files) {
                    uploadedFileNames.add(file.getFileName());
                }
            }

            return new UploadResult(
                    asperaResult != null && asperaResult.isSuccess(),
                    asperaResult != null ? asperaResult.getSuccessCount() : 0,
                    asperaResult != null ? asperaResult.getFailureCount() : files.size(),
                    UploadMethod.ASPERA,
                    asperaResult != null ? asperaResult.getErrorMessage() : "Aspera upload failed"
            );
        }

        private UploadResult simulateUpload() throws InterruptedException {
            log("Simulating file upload (Training Mode)...");

            List<DataFile> files = submission.getDataFiles();
            int total = files.size();

            for (int i = 0; i < total; i++) {
                if (isCancelled()) {
                    return new UploadResult(false, i, total - i, null, "Cancelled");
                }

                DataFile file = files.get(i);
                String fileName = file.getFileName();

                Platform.runLater(() -> {
                    currentFileName.set(fileName);
                    statusMessage.set("Simulating: " + fileName);
                });

                // Simulate progress
                for (int p = 0; p <= 100; p += 20) {
                    if (isCancelled()) break;
                    Thread.sleep(20);
                }

                final int completed = i + 1;
                Platform.runLater(() -> {
                    uploadedFiles.set(completed);
                    overallProgress.set((double) completed / total);
                });

                log("Simulated: " + fileName);
            }

            Platform.runLater(() -> {
                statusMessage.set("Simulation complete");
                overallProgress.set(1.0);
            });

            return new UploadResult(true, total, 0, null, "Training mode - simulated");
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
    }

    /**
     * Upload result
     */
    public static class UploadResult {
        private final boolean success;
        private final int successCount;
        private final int failureCount;
        private final UploadMethod method;
        private final String message;

        public UploadResult(boolean success, int successCount, int failureCount,
                           UploadMethod method, String message) {
            this.success = success;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.method = method;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public UploadMethod getMethod() { return method; }
        public String getMessage() { return message; }
    }
}
