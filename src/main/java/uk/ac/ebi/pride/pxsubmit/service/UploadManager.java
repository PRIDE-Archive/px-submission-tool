package uk.ac.ebi.pride.pxsubmit.service;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileWriter;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Resubmission;
import uk.ac.ebi.pride.data.model.ResubmissionFileChangeState;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.pxsubmit.config.AppConfig;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
    private final Resubmission resubmission; // null for normal submissions
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
    private final DoubleProperty currentFileProgress = new SimpleDoubleProperty(0);
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");
    private final DoubleProperty overallProgress = new SimpleDoubleProperty(0);
    private final ObjectProperty<UploadMethod> activeMethod = new SimpleObjectProperty<>();

    // Upload log
    private final ObservableList<String> uploadLog = FXCollections.observableArrayList();

    // Aggregated transfer statistics from upload services
    private volatile TransferStatistics aggregatedStats;

    // Current service references for pause/resume
    private volatile FtpUploadService currentFtpService;
    private volatile AsperaUploadService currentAsperaService;

    // Per-file upload completion callback for checkpoint updates
    private Consumer<String> fileUploadedCallback;

    public UploadManager(Submission submission, UploadDetail uploadDetail,
                         UploadMethod preferredMethod, boolean trainingMode) {
        this(submission, null, uploadDetail, preferredMethod, trainingMode);
    }

    public UploadManager(Submission submission, Resubmission resubmission,
                         UploadDetail uploadDetail,
                         UploadMethod preferredMethod, boolean trainingMode) {
        this.submission = submission;
        this.resubmission = resubmission;
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
    public DoubleProperty currentFileProgressProperty() { return currentFileProgress; }
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
     * Pause the current upload.
     */
    public void pauseUpload() {
        if (currentFtpService != null) {
            currentFtpService.pause();
        }
        if (currentAsperaService != null) {
            currentAsperaService.pause();
        }
    }

    /**
     * Resume a paused upload.
     */
    public void resumeUpload() {
        if (currentFtpService != null) {
            currentFtpService.resume();
        }
        if (currentAsperaService != null) {
            currentAsperaService.resume();
        }
    }

    /**
     * Check if the upload is currently paused.
     */
    public boolean isUploadPaused() {
        if (currentFtpService != null) return currentFtpService.isPaused();
        if (currentAsperaService != null) return currentAsperaService.isPaused();
        return false;
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
     * Set a callback that fires when each file is successfully uploaded.
     * Used by SubmissionStep to update the checkpoint file per-file.
     */
    public void setFileUploadedCallback(Consumer<String> callback) {
        this.fileUploadedCallback = callback;
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

                // For resubmission, clear comments before writing (matches old tool behavior)
                if (resubmission != null) {
                    submission.setComments(new ArrayList<>());
                }

                // Write submission
                SubmissionFileWriter.write(submission, summaryFile);

                // Append resubmission file change summary
                if (resubmission != null) {
                    appendResubmissionSummary(summaryFile);
                }

                // Append tool version metadata
                appendToolMetadata(summaryFile);

                log("Created: " + summaryFile.getAbsolutePath());
                log("Submission summary saved to: " + workingDir.getAbsolutePath());

                return summaryFile;
            } catch (Exception e) {
                logger.error("Failed to create submission summary file", e);
                log("ERROR: Failed to create submission summary: " + e.getMessage());
                return null;
            }
        }

        private void appendResubmissionSummary(File file) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                Map<DataFile, ResubmissionFileChangeState> resubMap = resubmission.getResubmission();
                for (Map.Entry<DataFile, ResubmissionFileChangeState> entry : resubMap.entrySet()) {
                    DataFile df = entry.getKey();
                    ResubmissionFileChangeState state = entry.getValue();
                    String typeName = df.getFileType() != null ? df.getFileType().getName() : "OTHER";
                    bw.write("COM\tResubmission\t" + df.getFileName() + "\t" +
                             typeName + "\t" + df.getFileSize() + "\t" + state);
                    bw.newLine();
                }
                bw.newLine();
            } catch (IOException e) {
                logger.error("Failed to append resubmission summary to submission.px", e);
            }
        }

        private void appendToolMetadata(File file) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                AppConfig config = AppConfig.getInstance();
                bw.write("COM\tVersion:" + config.getToolVersion());
                bw.newLine();
                String osName = System.getProperty("os.name");
                String osVersion = System.getProperty("os.version");
                String osArch = System.getProperty("os.arch");
                bw.write("COM\tOperating System:" + osName + " " + osVersion + " (" + osArch + ")");
                bw.newLine();
            } catch (IOException e) {
                logger.error("Failed to append tool metadata to submission.px", e);
            }
        }

        /**
         * Shared logic for binding an AbstractUploadService, running it on the FX thread,
         * waiting for completion, and converting the result to an UploadManager.UploadResult.
         */
        private UploadResult bindAndRunService(AbstractUploadService service,
                                               List<DataFile> files,
                                               UploadMethod method) throws Exception {
            // Bind shared progress properties
            service.uploadedFilesProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> {
                    uploadedFiles.set(newVal.intValue());
                    overallProgress.set((double) newVal.intValue() / totalFiles.get());
                });
                // Per-file callback: file at oldVal index just completed
                int completedIndex = oldVal.intValue();
                if (completedIndex >= 0 && completedIndex < files.size()) {
                    String completedFileName = files.get(completedIndex).getFileName();
                    if (completedFileName != null) {
                        uploadedFileNames.add(completedFileName);
                        if (fileUploadedCallback != null) {
                            fileUploadedCallback.accept(completedFileName);
                        }
                    }
                }
            });

            service.uploadedBytesProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> uploadedBytes.set(newVal.longValue()));
            });

            service.currentFileNameProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> currentFileName.set(newVal));
            });

            service.currentFileProgressProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> currentFileProgress.set(newVal.doubleValue()));
            });

            // Forward logs
            service.getUploadLog().addListener(
                    (javafx.collections.ListChangeListener.Change<? extends String> c) -> {
                        while (c.next()) {
                            if (c.wasAdded()) {
                                Platform.runLater(() -> uploadLog.addAll(c.getAddedSubList()));
                            }
                        }
                    });

            // Capture results on FX thread via callbacks (Service methods require FX thread)
            CountDownLatch doneLatch = new CountDownLatch(1);
            AtomicReference<uk.ac.ebi.pride.pxsubmit.service.UploadResult> serviceResultRef = new AtomicReference<>();
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            AtomicReference<Worker.State> stateRef = new AtomicReference<>();

            Platform.runLater(() -> {
                service.setOnSucceeded(e -> {
                    stateRef.set(Worker.State.SUCCEEDED);
                    serviceResultRef.set(service.getValue());
                    doneLatch.countDown();
                });
                service.setOnFailed(e -> {
                    stateRef.set(Worker.State.FAILED);
                    errorRef.set(service.getException());
                    doneLatch.countDown();
                });
                service.setOnCancelled(e -> {
                    stateRef.set(Worker.State.CANCELLED);
                    doneLatch.countDown();
                });
                service.start();
            });

            // Wait for completion, checking for cancellation
            while (!doneLatch.await(500, TimeUnit.MILLISECONDS)) {
                if (isCancelled()) {
                    Platform.runLater(() -> service.cancel());
                    doneLatch.await();
                    break;
                }
            }

            // Process results
            Worker.State finalState = stateRef.get();

            if (finalState == Worker.State.FAILED) {
                Throwable ex = errorRef.get();
                String errorMsg = method + " service failed: " + (ex != null ? ex.getMessage() : "Unknown error");
                logger.error(errorMsg, ex);
                log("ERROR: " + errorMsg);
                return new UploadResult(false, 0, files.size(), method, errorMsg);
            }

            if (finalState == Worker.State.CANCELLED) {
                log(method + " upload cancelled");
                return new UploadResult(false, 0, files.size(), method, "Upload cancelled");
            }

            uk.ac.ebi.pride.pxsubmit.service.UploadResult serviceResult = serviceResultRef.get();

            if (serviceResult == null) {
                String errorMsg = method + " service returned null result";
                logger.error(errorMsg);
                log("ERROR: " + errorMsg);
                return new UploadResult(false, 0, files.size(), method, errorMsg);
            }

            if (serviceResult.isSuccess()) {
                summaryFileUploaded = true;
                for (DataFile file : files) {
                    uploadedFileNames.add(file.getFileName());
                }
                log(method + " upload completed successfully");
            } else {
                String failMsg = serviceResult.getErrorMessage();
                log(method + " upload failed" + (failMsg != null ? ": " + failMsg : ""));
                log("Succeeded: " + serviceResult.getSuccessCount() +
                    ", Failed: " + serviceResult.getFailureCount());
            }

            // Capture transfer statistics
            aggregatedStats = service.getTransferStatistics();

            return new UploadResult(
                    serviceResult.isSuccess(),
                    serviceResult.getSuccessCount(),
                    serviceResult.getFailureCount(),
                    method,
                    serviceResult.isSuccess() ? null : (serviceResult.getErrorMessage() != null ?
                            serviceResult.getErrorMessage() : method + " upload failed")
            );
        }

        private UploadResult uploadWithFtp(List<DataFile> files) throws Exception {
            log("Starting FTP upload...");
            updateStatus("Uploading via FTP...");

            FtpUploadService ftpService = ServiceFactory.getInstance().createFtpUploadService(files, uploadDetail);
            currentFtpService = ftpService;

            return bindAndRunService(ftpService, files, UploadMethod.FTP);
        }

        private UploadResult uploadWithAspera(List<DataFile> files) throws Exception {
            log("Starting Aspera upload...");
            updateStatus("Uploading via Aspera...");

            String ascpPath = AsperaUploadService.findAscpPath();
            if (ascpPath == null) {
                log("ERROR: Aspera binary not found, falling back to FTP");
                return uploadWithFtp(files);
            }

            AsperaUploadService asperaService = ServiceFactory.getInstance().createAsperaUploadService(
                    files, uploadDetail, ascpPath);
            currentAsperaService = asperaService;

            // Bind Aspera-specific status message
            asperaService.statusMessageProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> statusMessage.set(newVal));
            });

            return bindAndRunService(asperaService, files, UploadMethod.ASPERA);
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
