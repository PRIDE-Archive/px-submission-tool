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
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics;

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

    // Aggregated transfer statistics from upload services
    private volatile TransferStatistics aggregatedStats;

    // Current FTP service reference for pause/resume
    private volatile FtpUploadService currentFtpService;

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
     * Get the aggregated transfer statistics from the most recent upload.
     * @return TransferStatistics or null if no upload has been performed
     */
    public TransferStatistics getTransferStatistics() { return aggregatedStats; }

    /**
     * Pause the current upload. In-flight file transfers will complete,
     * but no new files will be started until resumed.
     */
    public void pauseUpload() {
        if (currentFtpService != null) {
            currentFtpService.pause();
        }
    }

    /**
     * Resume a paused upload.
     */
    public void resumeUpload() {
        if (currentFtpService != null) {
            currentFtpService.resume();
        }
    }

    /**
     * Check if the upload is currently paused.
     */
    public boolean isUploadPaused() {
        return currentFtpService != null && currentFtpService.isPaused();
    }

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
            switch (method) {
                case ASPERA:
                    return uploadWithAspera(filesToUpload);
                case FTP:
                default:
                    return uploadWithFtp(filesToUpload);
            }
            // Note: submission.px is kept in working directory for user reference
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

                // Save submission.px in the current working directory
                File workingDir = new File(System.getProperty("user.dir"));
                File summaryFile = new File(workingDir, SUBMISSION_SUMMARY_FILE);

                // Write submission
                SubmissionFileWriter.write(submission, summaryFile);
                log("Created: " + summaryFile.getAbsolutePath());
                log("Submission summary saved to: " + workingDir.getAbsolutePath());

                return summaryFile;
            } catch (SubmissionFileException e) {
                logger.error("Failed to create submission summary file", e);
                log("ERROR: Failed to create submission summary: " + e.getMessage());
                return null;
            }
        }

        private UploadResult uploadWithFtp(List<DataFile> files) throws Exception {
            log("Starting FTP upload...");
            updateStatus("Uploading via FTP...");

            FtpUploadService ftpService = new FtpUploadService(files, uploadDetail);
            currentFtpService = ftpService;

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

            // Capture results on FX thread via callbacks (Service methods require FX thread)
            java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<FtpUploadService.UploadResult> ftpResultRef = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<Throwable> ftpErrorRef = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<javafx.concurrent.Worker.State> ftpStateRef = new java.util.concurrent.atomic.AtomicReference<>();

            Platform.runLater(() -> {
                ftpService.setOnSucceeded(e -> {
                    ftpStateRef.set(javafx.concurrent.Worker.State.SUCCEEDED);
                    ftpResultRef.set(ftpService.getValue());
                    doneLatch.countDown();
                });
                ftpService.setOnFailed(e -> {
                    ftpStateRef.set(javafx.concurrent.Worker.State.FAILED);
                    ftpErrorRef.set(ftpService.getException());
                    doneLatch.countDown();
                });
                ftpService.setOnCancelled(e -> {
                    ftpStateRef.set(javafx.concurrent.Worker.State.CANCELLED);
                    doneLatch.countDown();
                });
                ftpService.start();
            });

            // Wait for completion, checking for cancellation
            while (!doneLatch.await(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                if (isCancelled()) {
                    Platform.runLater(() -> ftpService.cancel());
                    doneLatch.await();
                    break;
                }
            }

            // Process results captured from FX thread callbacks
            javafx.concurrent.Worker.State finalState = ftpStateRef.get();

            if (finalState == javafx.concurrent.Worker.State.FAILED) {
                Throwable ex = ftpErrorRef.get();
                String errorMsg = "FTP service failed: " + (ex != null ? ex.getMessage() : "Unknown error");
                logger.error(errorMsg, ex);
                log("ERROR: " + errorMsg);
                return new UploadResult(false, 0, files.size(), UploadMethod.FTP, errorMsg);
            }

            if (finalState == javafx.concurrent.Worker.State.CANCELLED) {
                log("FTP upload cancelled");
                return new UploadResult(false, 0, files.size(), UploadMethod.FTP, "Upload cancelled");
            }

            FtpUploadService.UploadResult ftpResult = ftpResultRef.get();

            if (ftpResult == null) {
                String errorMsg = "FTP service returned null result";
                logger.error(errorMsg);
                log("ERROR: " + errorMsg);
                return new UploadResult(false, 0, files.size(), UploadMethod.FTP, errorMsg);
            }

            if (ftpResult.isSuccess()) {
                summaryFileUploaded = true;
                for (DataFile file : files) {
                    uploadedFileNames.add(file.getFileName());
                }
                log("FTP upload completed successfully");
            } else {
                log("FTP upload failed - Succeeded: " + ftpResult.getSuccessCount() +
                    ", Failed: " + ftpResult.getFailureCount());
            }

            // Capture transfer statistics
            aggregatedStats = ftpService.getTransferStatistics();

            return new UploadResult(
                    ftpResult.isSuccess(),
                    ftpResult.getSuccessCount(),
                    ftpResult.getFailureCount(),
                    UploadMethod.FTP,
                    ftpResult.isSuccess() ? null : "FTP upload failed"
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

            // Capture results on FX thread via callbacks (Service methods require FX thread)
            java.util.concurrent.CountDownLatch asperaDoneLatch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<AsperaUploadService.UploadResult> asperaResultRef = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<Throwable> asperaErrorRef = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<javafx.concurrent.Worker.State> asperaStateRef = new java.util.concurrent.atomic.AtomicReference<>();

            Platform.runLater(() -> {
                asperaService.setOnSucceeded(e -> {
                    asperaStateRef.set(javafx.concurrent.Worker.State.SUCCEEDED);
                    asperaResultRef.set(asperaService.getValue());
                    asperaDoneLatch.countDown();
                });
                asperaService.setOnFailed(e -> {
                    asperaStateRef.set(javafx.concurrent.Worker.State.FAILED);
                    asperaErrorRef.set(asperaService.getException());
                    asperaDoneLatch.countDown();
                });
                asperaService.setOnCancelled(e -> {
                    asperaStateRef.set(javafx.concurrent.Worker.State.CANCELLED);
                    asperaDoneLatch.countDown();
                });
                asperaService.start();
            });

            // Wait for completion, checking for cancellation
            while (!asperaDoneLatch.await(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                if (isCancelled()) {
                    Platform.runLater(() -> asperaService.cancel());
                    asperaDoneLatch.await();
                    break;
                }
            }

            // Process results captured from FX thread callbacks
            javafx.concurrent.Worker.State asperaState = asperaStateRef.get();

            if (asperaState == javafx.concurrent.Worker.State.FAILED) {
                Throwable ex = asperaErrorRef.get();
                String errorMsg = "Aspera service failed: " + (ex != null ? ex.getMessage() : "Unknown error");
                logger.error(errorMsg, ex);
                log("ERROR: " + errorMsg);
                return new UploadResult(false, 0, files.size(), UploadMethod.ASPERA, errorMsg);
            }

            if (asperaState == javafx.concurrent.Worker.State.CANCELLED) {
                log("Aspera upload cancelled");
                return new UploadResult(false, 0, files.size(), UploadMethod.ASPERA, "Upload cancelled");
            }

            AsperaUploadService.UploadResult asperaResult = asperaResultRef.get();

            if (asperaResult == null) {
                String errorMsg = "Aspera service returned null result";
                logger.error(errorMsg);
                log("ERROR: " + errorMsg);
                return new UploadResult(false, 0, files.size(), UploadMethod.ASPERA, errorMsg);
            }

            if (asperaResult.isSuccess()) {
                summaryFileUploaded = true;
                for (DataFile file : files) {
                    uploadedFileNames.add(file.getFileName());
                }
                log("Aspera upload completed successfully");
            } else {
                log("Aspera upload failed: " + asperaResult.getErrorMessage());
                log("Succeeded: " + asperaResult.getSuccessCount() +
                    ", Failed: " + asperaResult.getFailureCount());
            }

            // Capture transfer statistics
            aggregatedStats = asperaService.getTransferStatistics();

            return new UploadResult(
                    asperaResult.isSuccess(),
                    asperaResult.getSuccessCount(),
                    asperaResult.getFailureCount(),
                    UploadMethod.ASPERA,
                    asperaResult.getErrorMessage()
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
