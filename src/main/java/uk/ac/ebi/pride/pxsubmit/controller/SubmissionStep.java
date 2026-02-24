package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics;
import uk.ac.ebi.pride.pxsubmit.service.ApiService;
import uk.ac.ebi.pride.pxsubmit.service.ChecksumService;
import uk.ac.ebi.pride.pxsubmit.service.ServiceFactory;
import uk.ac.ebi.pride.pxsubmit.service.UploadManager;
import uk.ac.ebi.pride.pxsubmit.model.UploadCheckpoint;
import uk.ac.ebi.pride.pxsubmit.util.DebugMode;
import uk.ac.ebi.pride.pxsubmit.util.UploadCheckpointManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Final step for uploading files and completing submission.
 * Shows progress for checksum calculation and file upload.
 */
public class SubmissionStep extends AbstractWizardStep {

    // UI components
    private VBox statusBox;
    private ProgressBar overallProgress;
    private Label overallStatus;
    private Label currentFileLabel;
    private ProgressBar currentFileProgress;
    private ListView<String> logListView;
    private Button startButton;
    private Button cancelButton;
    private Button retryButton;
    private Button pauseButton;

    // Live statistics labels
    private Label elapsedTimeLabel;
    private Label transferRateLabel;
    private Label uploadedBytesLabel;
    private Label etaLabel;
    private HBox liveStatsBox;
    private Timeline statsTimeline;

    // State
    private final BooleanProperty uploading = new SimpleBooleanProperty(false);
    private final BooleanProperty completed = new SimpleBooleanProperty(false);
    private final StringProperty ticketId = new SimpleStringProperty();

    // Services
    private ApiService apiService;
    private UploadManager uploadManager;
    private TransferStatistics lastTransferStatistics;
    private long uploadStartTimeMs;
    private long pausedDurationMs;
    private long pauseStartTimeMs;

    // File progress bar chart
    private HBox fileProgressBar;
    private Region completedSegment;
    private Region remainingSegment;
    private Label fileProgressLabel;

    public SubmissionStep(SubmissionModel model) {
        super("submission",
              "Upload Files",
              "Your files are being uploaded to PRIDE",
              model);
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        // Status section
        statusBox = new VBox(12);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setStyle(
            "-fx-background-color: #f8f9fa; " +
            "-fx-border-color: #e9ecef; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 20;");
        statusBox.setMaxWidth(600);

        Label titleLabel = new Label("Ready to Upload");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        overallStatus = new Label("Click 'Start Upload' to begin");
        overallStatus.setStyle("-fx-text-fill: #666;");

        overallProgress = new ProgressBar(0);
        overallProgress.setPrefWidth(400);
        overallProgress.setVisible(false);

        currentFileLabel = new Label();
        currentFileLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        currentFileLabel.setVisible(false);

        currentFileProgress = new ProgressBar(0);
        currentFileProgress.setPrefWidth(400);
        currentFileProgress.setVisible(false);

        // Upload method selection
        VBox uploadMethodBox = new VBox(8);
        uploadMethodBox.setAlignment(Pos.CENTER_LEFT);
        uploadMethodBox.setPadding(new Insets(10, 0, 10, 0));

        Label uploadMethodLabel = new Label("Upload Method:");
        uploadMethodLabel.setStyle("-fx-font-weight: bold;");

        ToggleGroup uploadToggle = new ToggleGroup();

        RadioButton ftpRadio = new RadioButton("FTP - Standard file transfer (recommended for most users)");
        ftpRadio.setToggleGroup(uploadToggle);
        ftpRadio.setUserData(UploadMethod.FTP);

        RadioButton asperaRadio = new RadioButton("Aspera - High-speed transfer (recommended for large datasets)");
        asperaRadio.setToggleGroup(uploadToggle);
        asperaRadio.setUserData(UploadMethod.ASPERA);

        // Set current selection
        if (model.getUploadMethod() == UploadMethod.ASPERA) {
            asperaRadio.setSelected(true);
        } else {
            ftpRadio.setSelected(true);
            if (model.getUploadMethod() == null) {
                model.setUploadMethod(UploadMethod.FTP);
            }
        }

        // Update model on selection change
        uploadToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getUserData() instanceof UploadMethod) {
                model.setUploadMethod((UploadMethod) newVal.getUserData());
            }
        });

        VBox radioBox = new VBox(5, ftpRadio, asperaRadio);
        radioBox.setPadding(new Insets(0, 0, 0, 10));

        uploadMethodBox.getChildren().addAll(uploadMethodLabel, radioBox);

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        startButton = new Button("Start Upload");
        startButton.setStyle("-fx-background-color: #0066cc; -fx-text-fill: white;");
        startButton.setOnAction(e -> startUpload());

        cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> cancelUpload());
        cancelButton.setVisible(false);

        retryButton = new Button("Retry Upload");
        retryButton.setStyle("-fx-background-color: #ffc107; -fx-text-fill: #333;");
        retryButton.setOnAction(e -> startUpload());
        retryButton.setVisible(false);

        pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> togglePause());
        pauseButton.setVisible(false);

        buttonBox.getChildren().addAll(startButton, pauseButton, cancelButton, retryButton);

        // Live statistics during upload
        elapsedTimeLabel = new Label("Elapsed: --");
        transferRateLabel = new Label("Rate: --");
        uploadedBytesLabel = new Label("Transferred: --");
        String statsStyle = "-fx-text-fill: #555; -fx-font-size: 11px;";
        elapsedTimeLabel.setStyle(statsStyle);
        transferRateLabel.setStyle(statsStyle);
        uploadedBytesLabel.setStyle(statsStyle);

        etaLabel = new Label("ETA: --");
        etaLabel.setStyle(statsStyle);

        liveStatsBox = new HBox(20, elapsedTimeLabel, transferRateLabel, uploadedBytesLabel, etaLabel);
        liveStatsBox.setAlignment(Pos.CENTER);
        liveStatsBox.setVisible(false);

        // File progress bar chart
        fileProgressLabel = new Label("Uploading 0/0 (0%)");
        fileProgressLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        fileProgressLabel.setVisible(false);

        completedSegment = new Region();
        completedSegment.setStyle("-fx-background-color: #28a745; -fx-background-radius: 4 0 0 4;");
        completedSegment.setMinHeight(24);

        remainingSegment = new Region();
        remainingSegment.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 0 4 4 0;");
        remainingSegment.setMinHeight(24);

        fileProgressBar = new HBox();
        fileProgressBar.setMaxWidth(400);
        fileProgressBar.setPrefWidth(400);
        fileProgressBar.setStyle("-fx-background-radius: 4;");
        fileProgressBar.setVisible(false);
        HBox.setHgrow(completedSegment, Priority.NEVER);
        HBox.setHgrow(remainingSegment, Priority.NEVER);
        fileProgressBar.getChildren().addAll(completedSegment, remainingSegment);

        statusBox.getChildren().addAll(
            titleLabel, overallStatus,
            uploadMethodBox,
            overallProgress, currentFileLabel, currentFileProgress,
            fileProgressLabel, fileProgressBar,
            liveStatsBox,
            buttonBox
        );

        // Log section
        TitledPane logPane = new TitledPane();
        logPane.setText("Upload Log");
        logPane.setExpanded(true);

        logListView = new ListView<>();
        logListView.setPrefHeight(180);
        logListView.setMinHeight(100);
        logPane.setContent(logListView);

        root.getChildren().addAll(statusBox, logPane);

        scrollPane.setContent(root);
        return scrollPane;
    }

    @Override
    protected void initializeStep() {
        // Valid when upload is complete
        valid.bind(completed);
    }

    @Override
    protected void onStepEntering() {
        // Reset state
        uploading.set(false);
        completed.set(false);
        overallProgress.setProgress(0);
        currentFileProgress.setProgress(0);
        logListView.getItems().clear();
        retryButton.setVisible(false);
        pauseButton.setVisible(false);
        liveStatsBox.setVisible(false);
        fileProgressLabel.setVisible(false);
        fileProgressBar.setVisible(false);
        lastTransferStatistics = null;
        pausedDurationMs = 0;
        pauseStartTimeMs = 0;
        stopStatsTimeline();

        // Test mode message
        if (model.isTrainingMode()) {
            addLog("TEST MODE: Files will not actually be uploaded");
        }

        // Shut down any previous ApiService before creating a new one
        if (apiService != null) {
            apiService.shutdown();
        }

        // Initialize API service
        apiService = ServiceFactory.getInstance().createApiService(model.getUserName(), model.getPassword());

        // Resume from checkpoint: pre-set ticketId so we reuse the same upload folder
        UploadCheckpoint pending = model.getPendingCheckpoint();
        if (pending != null && pending.getUploadFolder() != null) {
            ticketId.set(pending.getUploadFolder());
            addLog("Resuming interrupted upload (folder: " + pending.getUploadFolder() + ")");
        }
    }

    @Override
    protected void onStepLeaving() {
        stopStatsTimeline();

        // Cancel any running upload
        if (uploadManager != null && uploadManager.isRunning()) {
            uploadManager.cancel();
        }
        uploadManager = null;

        if (apiService != null) {
            apiService.shutdown();
            apiService = null;
        }
    }

    private void startUpload() {
        uploading.set(true);
        startButton.setDisable(true);
        cancelButton.setVisible(true);
        retryButton.setVisible(false);
        pauseButton.setVisible(true);
        pauseButton.setText("Pause");
        overallProgress.setVisible(true);
        liveStatsBox.setVisible(true);
        startStatsTimeline();

        addLog("Starting submission...");
        uploadStartTimeMs = System.currentTimeMillis();
        pausedDurationMs = 0;
        pauseStartTimeMs = 0;
        fileProgressLabel.setVisible(true);
        fileProgressBar.setVisible(true);

        // Checksums should already be computed by ChecksumComputationStep
        if (!model.areChecksumsCalculated() || model.getChecksums().isEmpty()) {
            addLog("ERROR: Checksums not computed. Please go back and complete the checksum step.");
            handleError("Checksums not computed");
            return;
        }

        addLog("Using pre-computed checksums (" + model.getChecksums().size() + " files)");
        DebugMode.log("CHECKSUM", "Using pre-computed checksums from ChecksumComputationStep");

        // Write checksum.txt file for upload
        writeChecksumFile(model.getChecksums());

        // Save checkpoint before starting upload
        saveCheckpoint();

        Platform.runLater(() -> {
            overallProgress.setProgress(0.2);
            // Get upload details
            getUploadDetails();
        });
    }

    /**
     * Write checksum.txt file and add it to the file list for upload
     */
    private void writeChecksumFile(Map<DataFile, String> checksums) {
        try {
            // Write checksum.txt to the current working directory
            File workingDir = new File(System.getProperty("user.dir"));
            File checksumFile = ChecksumService.writeChecksumFile(checksums, workingDir);

            // Check if checksum.txt is already in the file list
            boolean alreadyExists = model.getFiles().stream()
                .anyMatch(f -> "checksum.txt".equals(f.getFileName()));

            if (!alreadyExists) {
                // Add checksum.txt to the file list for upload
                DataFile checksumDataFile = new DataFile();
                checksumDataFile.setFile(checksumFile);
                checksumDataFile.setFileType(ProjectFileType.OTHER);
                model.addFile(checksumDataFile);
            }

            addLog("Checksum file created: checksum.txt");
            DebugMode.log("CHECKSUM", "checksum.txt created at: " + checksumFile.getAbsolutePath());

            // Log individual checksums in debug mode
            for (Map.Entry<DataFile, String> entry : checksums.entrySet()) {
                DebugMode.log("CHECKSUM", entry.getKey().getFileName() + " -> " + entry.getValue());
            }

        } catch (IOException ex) {
            addLog("WARNING: Could not write checksum file - " + ex.getMessage());
            logger.warn("Could not write checksum file", ex);
            DebugMode.log("CHECKSUM", "ERROR writing checksum.txt: " + ex.getMessage());
        }
    }

    private void getUploadDetails() {
        updateStatus("Getting upload credentials...");
        addLog("Requesting upload details from server...");

        if (model.isTrainingMode()) {
            // Simulate for test mode
            addLog("Test mode: Simulating upload credentials");
            Platform.runLater(() -> {
                overallProgress.setProgress(0.4);
                simulateUpload();
            });
            return;
        }

        UploadMethod method = model.getUploadMethod();
        if (method == null) {
            method = UploadMethod.FTP;
            addLog("No upload method selected, defaulting to FTP");
        }

        addLog("Requesting " + method + " upload credentials...");
        final UploadMethod finalMethod = method;

        // Reuse existing ticketId for retries (avoids getting a new upload folder)
        String existingTicketId = ticketId.get();
        apiService.getUploadDetails(method, existingTicketId)
            .thenAccept(uploadDetail -> {
                addLog("Upload details received successfully");
                addLog("Host: " + uploadDetail.getHost());
                addLog("Port: " + uploadDetail.getPort());
                addLog("Folder: " + uploadDetail.getFolder());
                addLog("Method: " + uploadDetail.getMethod());

                model.setUploadDetail(uploadDetail);
                ticketId.set(uploadDetail.getFolder());

                Platform.runLater(() -> {
                    overallProgress.setProgress(0.4);
                    uploadFiles(uploadDetail);
                });
            })
            .exceptionally(ex -> {
                logger.error("Failed to get upload details", ex);
                addLog("ERROR: Failed to get upload details");
                addLog("ERROR: " + ex.getMessage());
                addLog("Please check your internet connection and try again.");
                addLog("If the problem persists, contact pride-support@ebi.ac.uk");
                handleError("Failed to get upload credentials: " + ex.getMessage());
                return null;
            });
    }

    private void uploadFiles(UploadDetail uploadDetail) {
        updateStatus("Uploading files...");
        UploadMethod method = model.getUploadMethod() != null ? model.getUploadMethod() : UploadMethod.FTP;
        addLog("Starting file upload via " + method + "...");
        addLog("Number of files to upload: " + model.getFiles().size());

        // Validate upload details
        if (uploadDetail == null) {
            addLog("ERROR: Upload details are null");
            handleError("Upload configuration error");
            return;
        }

        if (uploadDetail.getHost() == null || uploadDetail.getHost().isEmpty()) {
            addLog("ERROR: Upload host is not configured");
            handleError("Upload configuration error: Missing host");
            return;
        }

        if (uploadDetail.getFolder() == null || uploadDetail.getFolder().isEmpty()) {
            addLog("ERROR: Upload folder is not configured");
            handleError("Upload configuration error: Missing folder");
            return;
        }

        currentFileLabel.setVisible(true);
        currentFileProgress.setVisible(true);

        // Create UploadManager with real upload services
        uploadManager = ServiceFactory.getInstance().createUploadManager(
                model.getSubmission(),
                model.isResubmissionMode() ? model.getResubmission() : null,
                uploadDetail,
                method,
                model.isTrainingMode()
        );

        // Pre-seed already uploaded files from checkpoint (resume scenario)
        UploadCheckpoint pending = model.getPendingCheckpoint();
        if (pending != null && pending.getUploadedFileNames() != null) {
            for (String fileName : pending.getUploadedFileNames()) {
                uploadManager.markFileUploaded(fileName);
            }
            addLog("Resuming upload: " + pending.getUploadedFileNames().size() + " files already uploaded");
        }

        // Wire per-file checkpoint callback
        uploadManager.setFileUploadedCallback(fileName -> {
            UploadCheckpointManager.markFileUploaded(fileName);
            logger.debug("Checkpoint updated: {} marked as uploaded", fileName);
        });

        // Update checkpoint with upload details (host/folder now known)
        updateCheckpointWithUploadDetails(uploadDetail);

        // Bind overall progress bar to bytes transferred
        uploadManager.uploadedBytesProperty().addListener((obs, oldVal, newVal) -> {
            long total = uploadManager.totalBytesProperty().get();
            double progress = total > 0 ? (double) newVal.longValue() / total : 0;
            Platform.runLater(() -> overallProgress.setProgress(progress));
        });

        uploadManager.currentFileNameProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> currentFileLabel.setText("Uploading: " + newVal));
        });

        // Bind current file progress bar
        uploadManager.currentFileProgressProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> currentFileProgress.setProgress(newVal.doubleValue()));
        });

        uploadManager.statusMessageProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> updateStatus(newVal));
        });

        // Forward upload logs to UI
        uploadManager.getUploadLog().addListener(
                (javafx.collections.ListChangeListener.Change<? extends String> c) -> {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            Platform.runLater(() -> {
                                for (String log : c.getAddedSubList()) {
                                    logListView.getItems().add(log);
                                }
                                logListView.scrollTo(logListView.getItems().size() - 1);
                            });
                        }
                    }
                });

        // Handle completion
        uploadManager.setOnSucceeded(e -> {
            UploadManager.UploadResult result = uploadManager.getValue();
            if (result.isSuccess()) {
                addLog("Upload completed successfully via " + result.getMethod());
                // Mark all files as uploaded
                for (DataFile file : model.getFiles()) {
                    model.markFileUploaded(file);
                }
                Platform.runLater(this::completeSubmission);
            } else {
                addLog("Upload failed: " + result.getMessage());
                addLog("Uploaded: " + result.getSuccessCount() + ", Failed: " + result.getFailureCount());
                handleError("Upload failed: " + result.getMessage());
            }
        });

        uploadManager.setOnFailed(e -> {
            Throwable ex = e.getSource().getException();
            addLog("ERROR: Upload failed - " + ex.getMessage());
            handleError("Upload failed: " + ex.getMessage());
        });

        uploadManager.setOnCancelled(e -> {
            addLog("Upload cancelled");
            handleError("Upload cancelled");
        });

        // Start upload
        uploadManager.start();
    }

    private void simulateUpload() {
        updateStatus("Simulating upload (Test Mode)...");
        addLog("Test mode: Simulating file upload...");

        currentFileLabel.setVisible(true);
        currentFileProgress.setVisible(true);

        // Use UploadManager in test mode - it handles simulation internally
        uploadManager = ServiceFactory.getInstance().createUploadManager(
                model.getSubmission(),
                null, // No upload detail needed for test mode
                UploadMethod.FTP,
                true  // Test mode
        );

        // Bind progress
        uploadManager.overallProgressProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                double baseProgress = 0.4;
                double uploadProgress = newVal.doubleValue() * 0.5;
                overallProgress.setProgress(baseProgress + uploadProgress);
            });
        });

        uploadManager.currentFileNameProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> currentFileLabel.setText("Simulating: " + newVal));
        });

        uploadManager.getUploadLog().addListener(
                (javafx.collections.ListChangeListener.Change<? extends String> c) -> {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            Platform.runLater(() -> {
                                for (String log : c.getAddedSubList()) {
                                    logListView.getItems().add(log);
                                }
                                logListView.scrollTo(logListView.getItems().size() - 1);
                            });
                        }
                    }
                });

        uploadManager.setOnSucceeded(e -> {
            addLog("Test mode: Simulation completed successfully");
            Platform.runLater(this::completeSubmission);
        });

        uploadManager.setOnFailed(e -> {
            addLog("ERROR: Simulation failed - " + e.getSource().getException().getMessage());
            handleError("Simulation failed");
        });

        uploadManager.start();
    }

    private void completeSubmission() {
        stopStatsTimeline();
        // Capture transfer statistics before nulling uploadManager
        if (uploadManager != null) {
            lastTransferStatistics = uploadManager.getTransferStatistics();
        }

        updateStatus("Completing submission...");
        addLog("Finalizing submission...");
        overallProgress.setProgress(0.95);
        startButton.setDisable(true);
        cancelButton.setVisible(false);

        // In test mode, skip actual completion
        if (model.isTrainingMode()) {
            addLog("Test mode: Submission simulated successfully");
            finishSubmission("TEST-" + System.currentTimeMillis());
            return;
        }

        // Call the appropriate WS endpoint to complete and get the ticket/reference ID
        if (model.isResubmissionMode()) {
            addLog("Submitting resubmission to PRIDE server...");
            apiService.completeResubmission(model.getUploadDetail())
                .thenAccept(referenceDetail -> {
                    String reference = referenceDetail.getReference();
                    addLog("Resubmission reference received: " + reference);
                    finishSubmission(reference);
                })
                .exceptionally(ex -> {
                    logger.error("Failed to complete resubmission", ex);
                    addLog("ERROR: Failed to finalize resubmission: " + ex.getMessage());
                    handleError("Failed to finalize resubmission: " + ex.getMessage());
                    return null;
                });
        } else {
            addLog("Submitting to PRIDE server...");
            apiService.completeSubmission(model.getUploadDetail())
                .thenAccept(referenceDetail -> {
                    String reference = referenceDetail.getReference();
                    addLog("Submission reference received: " + reference);
                    finishSubmission(reference);
                })
                .exceptionally(ex -> {
                    logger.error("Failed to complete submission", ex);
                    addLog("ERROR: Failed to finalize submission: " + ex.getMessage());
                    handleError("Failed to finalize submission: " + ex.getMessage());
                    return null;
                });
        }
    }

    private void finishSubmission(String submissionId) {
        // Delete checkpoint on successful submission
        UploadCheckpointManager.delete();
        model.setPendingCheckpoint(null);
        logger.info("Upload checkpoint removed after successful submission");

        Platform.runLater(() -> {
            overallProgress.setProgress(1.0);
            currentFileLabel.setVisible(false);
            currentFileProgress.setVisible(false);
            cancelButton.setVisible(false);
            pauseButton.setVisible(false);
            liveStatsBox.setVisible(false);

            completed.set(true);
            uploading.set(false);

            updateStatus("Submission Complete!");
            addLog("=".repeat(40));
            addLog("Submission ID: " + submissionId);

            // Log transfer statistics
            if (lastTransferStatistics != null) {
                long durationMs = lastTransferStatistics.getTotalDurationMs();
                addLog(String.format("Duration: %s", formatDuration(durationMs)));
                addLog(String.format("Files: %d succeeded, %d failed",
                    lastTransferStatistics.getFilesCompleted(), lastTransferStatistics.getFilesFailed()));
                addLog(String.format("Transferred: %s at %.2f MB/s",
                    formatBytes(lastTransferStatistics.getBytesTransferred()),
                    lastTransferStatistics.getAverageRateMBps()));
            }
            addLog("=".repeat(40));

            // Update UI
            Label successLabel = new Label("Your submission has been completed successfully!");
            successLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 14px;");

            Label idLabel = new Label("Submission ID: " + submissionId);
            idLabel.setStyle("-fx-font-weight: bold;");

            statusBox.getChildren().clear();
            statusBox.getChildren().addAll(
                new Label("Submission Complete!") {{
                    setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #28a745;");
                }},
                successLabel,
                idLabel
            );

            // Add statistics summary panel
            if (lastTransferStatistics != null) {
                VBox statsPanel = createStatsSummaryPanel(lastTransferStatistics);
                statusBox.getChildren().add(statsPanel);
            }

            statusBox.getChildren().add(
                new Label("You will receive a confirmation email shortly.")
            );
        });
    }

    private void cancelUpload() {
        uploading.set(false);
        if (uploadManager != null && uploadManager.isRunning()) {
            uploadManager.cancel();
        }
        uploadManager = null;
        addLog("Upload cancelled by user");
        handleError("Upload cancelled");
    }

    private void togglePause() {
        if (uploadManager == null) return;
        if (uploadManager.isUploadPaused()) {
            // Resume: accumulate paused duration
            if (pauseStartTimeMs > 0) {
                pausedDurationMs += System.currentTimeMillis() - pauseStartTimeMs;
                pauseStartTimeMs = 0;
            }
            uploadManager.resumeUpload();
            pauseButton.setText("Pause");
            addLog("Upload resumed by user");
        } else {
            // Pause: record when pause started
            pauseStartTimeMs = System.currentTimeMillis();
            uploadManager.pauseUpload();
            pauseButton.setText("Resume");
            addLog("Upload paused by user");
        }
    }

    /**
     * Save checkpoint before upload starts. Captures all model state needed to resume.
     */
    private void saveCheckpoint() {
        try {
            UploadCheckpoint checkpoint = new UploadCheckpoint();
            checkpoint.setUserName(model.getUserName());
            checkpoint.setTimestamp(System.currentTimeMillis());
            checkpoint.setResubmissionMode(model.isResubmissionMode());

            // Upload method
            UploadMethod method = model.getUploadMethod();
            checkpoint.setUploadMethod(method != null ? method.name() : "FTP");

            // Files
            java.util.List<UploadCheckpoint.FileEntry> fileEntries = new ArrayList<>();
            for (DataFile df : model.getFiles()) {
                String filePath = df.getFile() != null ? df.getFile().getAbsolutePath() : "";
                String fileName = df.getFileName();
                String fileType = df.getFileType() != null ? df.getFileType().name() : "OTHER";
                fileEntries.add(new UploadCheckpoint.FileEntry(filePath, fileName, fileType));
            }
            checkpoint.setFiles(fileEntries);

            // Checksums (fileName -> checksum)
            Map<String, String> checksumMap = new HashMap<>();
            for (Map.Entry<DataFile, String> entry : model.getChecksums().entrySet()) {
                checksumMap.put(entry.getKey().getFileName(), entry.getValue());
            }
            checkpoint.setChecksums(checksumMap);

            // submission.px path
            File workingDir = new File(System.getProperty("user.dir"));
            File submissionPx = new File(workingDir, "submission.px");
            if (submissionPx.exists()) {
                checkpoint.setSubmissionPxPath(submissionPx.getAbsolutePath());
            }

            UploadCheckpointManager.save(checkpoint);
            addLog("Upload checkpoint saved");
        } catch (Exception e) {
            logger.warn("Could not save upload checkpoint", e);
        }
    }

    /**
     * Update existing checkpoint with upload details once they're received from server.
     */
    private void updateCheckpointWithUploadDetails(UploadDetail uploadDetail) {
        try {
            UploadCheckpoint checkpoint = UploadCheckpointManager.load();
            if (checkpoint != null) {
                checkpoint.setUploadHost(uploadDetail.getHost());
                checkpoint.setUploadPort(uploadDetail.getPort());
                checkpoint.setUploadFolder(uploadDetail.getFolder());
                UploadCheckpointManager.save(checkpoint);
            }
        } catch (Exception e) {
            logger.warn("Could not update checkpoint with upload details", e);
        }
    }

    private void handleError(String message) {
        stopStatsTimeline();
        // Capture stats on failure too
        if (uploadManager != null && lastTransferStatistics == null) {
            lastTransferStatistics = uploadManager.getTransferStatistics();
        }
        Platform.runLater(() -> {
            uploading.set(false);
            startButton.setDisable(false);
            cancelButton.setVisible(false);
            pauseButton.setVisible(false);
            retryButton.setVisible(true);
            overallProgress.setVisible(false);
            currentFileLabel.setVisible(false);
            currentFileProgress.setVisible(false);
            liveStatsBox.setVisible(false);
            fileProgressLabel.setVisible(false);
            fileProgressBar.setVisible(false);

            updateStatus("Error: " + message);
            overallStatus.setStyle("-fx-text-fill: #dc3545;");
        });
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> {
            overallStatus.setText(status);
            overallStatus.setStyle("-fx-text-fill: #666;");
        });
    }

    private void addLog(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            logListView.getItems().add("[" + timestamp + "] " + message);
            logListView.scrollTo(logListView.getItems().size() - 1);
        });
        logger.info(message);
    }

    @Override
    public boolean showBackButton() {
        return !uploading.get() && !completed.get();
    }

    @Override
    public String getNextButtonText() {
        return completed.get() ? "Finish" : "Submit";
    }

    @Override
    public boolean isFinalStep() {
        return true;
    }

    // ==================== Live Statistics ====================

    private void startStatsTimeline() {
        stopStatsTimeline();
        statsTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateLiveStats()));
        statsTimeline.setCycleCount(Timeline.INDEFINITE);
        statsTimeline.play();
    }

    private void stopStatsTimeline() {
        if (statsTimeline != null) {
            statsTimeline.stop();
            statsTimeline = null;
        }
    }

    private void updateLiveStats() {
        if (uploadManager == null) return;

        boolean paused = uploadManager.isUploadPaused();

        // Calculate active (non-paused) elapsed time
        long now = System.currentTimeMillis();
        long totalElapsed = now - uploadStartTimeMs;
        long currentPauseDuration = (paused && pauseStartTimeMs > 0)
                ? (now - pauseStartTimeMs) : 0;
        long activeElapsed = totalElapsed - pausedDurationMs - currentPauseDuration;
        if (activeElapsed < 0) activeElapsed = 0;

        long transferred = uploadManager.uploadedBytesProperty().get();
        long total = uploadManager.totalBytesProperty().get();
        double rateMBps = activeElapsed > 0
                ? (transferred / (1024.0 * 1024.0)) / (activeElapsed / 1000.0)
                : 0;

        elapsedTimeLabel.setText("Elapsed: " + formatDuration(activeElapsed));
        uploadedBytesLabel.setText("Transferred: " + formatBytes(transferred));

        if (paused) {
            transferRateLabel.setText("Rate: Paused");
            etaLabel.setText("ETA: Paused");
        } else {
            transferRateLabel.setText(String.format("Rate: %.2f MB/s", rateMBps));
            // ETA calculation
            if (rateMBps > 0 && transferred > 0 && transferred < total) {
                long remainingBytes = total - transferred;
                double bytesPerMs = rateMBps * 1024.0 * 1024.0 / 1000.0;
                long etaMs = (long) (remainingBytes / bytesPerMs);
                etaLabel.setText("ETA: " + formatDuration(etaMs));
            } else if (transferred >= total && total > 0) {
                etaLabel.setText("ETA: Complete");
            } else {
                etaLabel.setText("ETA: Calculating...");
            }
        }

        // Update file progress label and bar
        int uploaded = uploadManager.uploadedFilesProperty().get();
        int totalFiles = uploadManager.totalFilesProperty().get();
        if (totalFiles > 0) {
            int pct = (int) Math.round(100.0 * uploaded / totalFiles);
            fileProgressLabel.setText(String.format("Uploading %d/%d (%d%%)", uploaded, totalFiles, pct));

            double fraction = (double) uploaded / totalFiles;
            double barWidth = fileProgressBar.getPrefWidth();
            completedSegment.setPrefWidth(barWidth * fraction);
            remainingSegment.setPrefWidth(barWidth * (1.0 - fraction));
        }
    }

    private VBox createStatsSummaryPanel(TransferStatistics stats) {
        VBox panel = new VBox(4);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(10, 0, 10, 0));
        panel.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 6; -fx-padding: 10;");

        Label header = new Label("Transfer Summary");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label durationLabel = new Label("Duration: " + formatDuration(stats.getTotalDurationMs()));
        Label filesLabel = new Label(String.format("Files: %d succeeded, %d failed",
            stats.getFilesCompleted(), stats.getFilesFailed()));
        Label bytesLabel = new Label(String.format("Data transferred: %s",
            formatBytes(stats.getBytesTransferred())));
        Label rateLabel = new Label(String.format("Average rate: %.2f MB/s",
            stats.getAverageRateMBps()));

        String labelStyle = "-fx-text-fill: #333; -fx-font-size: 12px;";
        durationLabel.setStyle(labelStyle);
        filesLabel.setStyle(labelStyle);
        bytesLabel.setStyle(labelStyle);
        rateLabel.setStyle(labelStyle);

        panel.getChildren().addAll(header, durationLabel, filesLabel, bytesLabel, rateLabel);
        return panel;
    }

    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
