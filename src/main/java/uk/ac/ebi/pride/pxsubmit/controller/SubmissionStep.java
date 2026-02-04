package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.ApiService;
import uk.ac.ebi.pride.pxsubmit.service.ChecksumService;
import uk.ac.ebi.pride.pxsubmit.service.UploadManager;
import uk.ac.ebi.pride.pxsubmit.util.DebugMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    // State
    private final BooleanProperty uploading = new SimpleBooleanProperty(false);
    private final BooleanProperty completed = new SimpleBooleanProperty(false);
    private final StringProperty ticketId = new SimpleStringProperty();

    // Services
    private ApiService apiService;
    private ChecksumService checksumService;
    private UploadManager uploadManager;

    public SubmissionStep(SubmissionModel model) {
        super("submission",
              "Upload Files",
              "Your files are being uploaded to PRIDE",
              model);
    }

    @Override
    protected Parent createContent() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        // Status section
        statusBox = new VBox(15);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setStyle(
            "-fx-background-color: #f8f9fa; " +
            "-fx-border-color: #e9ecef; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 30;");
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

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        startButton = new Button("Start Upload");
        startButton.setStyle("-fx-background-color: #0066cc; -fx-text-fill: white;");
        startButton.setOnAction(e -> startUpload());

        cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> cancelUpload());
        cancelButton.setVisible(false);

        buttonBox.getChildren().addAll(startButton, cancelButton);

        statusBox.getChildren().addAll(
            titleLabel, overallStatus,
            overallProgress, currentFileLabel, currentFileProgress,
            buttonBox
        );

        // Log section
        TitledPane logPane = new TitledPane();
        logPane.setText("Upload Log");
        logPane.setExpanded(false);

        logListView = new ListView<>();
        logListView.setPrefHeight(150);
        logPane.setContent(logListView);

        root.getChildren().addAll(statusBox, logPane);

        return root;
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

        // Training mode message
        if (model.isTrainingMode()) {
            addLog("TRAINING MODE: Files will not actually be uploaded");
        }

        // Initialize API service
        apiService = new ApiService(model.getUserName(), model.getPassword());
    }

    @Override
    protected void onStepLeaving() {
        if (apiService != null) {
            apiService.shutdown();
        }
    }

    private void startUpload() {
        uploading.set(true);
        startButton.setDisable(true);
        cancelButton.setVisible(true);
        overallProgress.setVisible(true);

        addLog("Starting submission...");

        // Step 1: Calculate checksums
        calculateChecksums();
    }

    private void calculateChecksums() {
        // Check if checksums were already calculated in SummaryStep
        if (model.areChecksumsCalculated() && !model.getChecksums().isEmpty()) {
            addLog("Using pre-computed checksums (" + model.getChecksums().size() + " files)");
            DebugMode.log("CHECKSUM", "Using pre-computed checksums from SummaryStep");

            // Write checksum.txt file for upload
            writeChecksumFile(model.getChecksums());

            Platform.runLater(() -> {
                overallProgress.setProgress(0.2);
                // Skip to getting upload details
                getUploadDetails();
            });
            return;
        }

        updateStatus("Calculating checksums...");
        addLog("Calculating file checksums...");
        DebugMode.log("CHECKSUM", "Starting checksum calculation for " + model.getFiles().size() + " files");

        checksumService = new ChecksumService(model.getFiles());

        currentFileLabel.setVisible(true);
        currentFileLabel.textProperty().bind(checksumService.currentFileNameProperty());
        currentFileProgress.setVisible(true);
        currentFileProgress.progressProperty().bind(checksumService.progressProperty());

        checksumService.setOnSucceeded(e -> {
            Map<DataFile, String> checksums = checksumService.getValue();
            addLog("Checksums calculated for " + checksums.size() + " files");
            DebugMode.log("CHECKSUM", "Checksums calculated: " + checksums.size());

            // Store checksums in model
            model.setChecksums(checksums);
            DebugMode.log("CHECKSUM", "Checksums stored in model");

            // Write checksum.txt file
            writeChecksumFile(checksums);

            Platform.runLater(() -> {
                currentFileLabel.textProperty().unbind();
                currentFileProgress.progressProperty().unbind();
                overallProgress.setProgress(0.2);

                // Step 2: Get upload details
                getUploadDetails();
            });
        });

        checksumService.setOnFailed(e -> {
            Throwable ex = e.getSource().getException();
            addLog("ERROR: Checksum calculation failed - " + ex.getMessage());
            DebugMode.log("CHECKSUM", "FAILED: " + ex.getMessage());
            handleError("Checksum calculation failed");
        });

        checksumService.start();
    }

    /**
     * Write checksum.txt file and add it to the file list for upload
     */
    private void writeChecksumFile(Map<DataFile, String> checksums) {
        try {
            File tempDir = Files.createTempDirectory("px-submission-").toFile();
            tempDir.deleteOnExit();
            File checksumFile = ChecksumService.writeChecksumFile(checksums, tempDir);
            checksumFile.deleteOnExit();

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
            // Simulate for training mode
            addLog("Training mode: Simulating upload credentials");
            Platform.runLater(() -> {
                overallProgress.setProgress(0.4);
                simulateUpload();
            });
            return;
        }

        UploadMethod method = model.getUploadMethod();
        if (method == null) {
            method = UploadMethod.FTP;
        }

        apiService.getUploadDetails(method)
            .thenAccept(uploadDetail -> {
                model.setUploadDetail(uploadDetail);
                addLog("Upload details received. Target: " + uploadDetail.getFolder());
                ticketId.set(uploadDetail.getFolder());

                Platform.runLater(() -> {
                    overallProgress.setProgress(0.4);
                    uploadFiles(uploadDetail);
                });
            })
            .exceptionally(ex -> {
                addLog("ERROR: Failed to get upload details - " + ex.getMessage());
                handleError("Failed to get upload credentials");
                return null;
            });
    }

    private void uploadFiles(UploadDetail uploadDetail) {
        updateStatus("Uploading files...");
        addLog("Starting file upload via " + model.getUploadMethod() + "...");

        currentFileLabel.setVisible(true);
        currentFileProgress.setVisible(true);

        // Create UploadManager with real upload services
        uploadManager = new UploadManager(
                model.getSubmission(),
                uploadDetail,
                model.getUploadMethod(),
                model.isTrainingMode()
        );

        // Bind progress to UI
        uploadManager.overallProgressProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                double baseProgress = 0.4; // Progress after checksums
                double uploadProgress = newVal.doubleValue() * 0.5; // Upload is 50% of remaining
                overallProgress.setProgress(baseProgress + uploadProgress);
            });
        });

        uploadManager.currentFileNameProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> currentFileLabel.setText("Uploading: " + newVal));
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
        updateStatus("Simulating upload (Training Mode)...");
        addLog("Training mode: Simulating file upload...");

        currentFileLabel.setVisible(true);
        currentFileProgress.setVisible(true);

        // Use UploadManager in training mode - it handles simulation internally
        uploadManager = new UploadManager(
                model.getSubmission(),
                null, // No upload detail needed for training mode
                UploadMethod.FTP,
                true  // Training mode
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
            addLog("Training mode: Simulation completed successfully");
            Platform.runLater(this::completeSubmission);
        });

        uploadManager.setOnFailed(e -> {
            addLog("ERROR: Simulation failed - " + e.getSource().getException().getMessage());
            handleError("Simulation failed");
        });

        uploadManager.start();
    }

    private void completeSubmission() {
        updateStatus("Completing submission...");
        addLog("Finalizing submission...");
        overallProgress.setProgress(0.95);

        // In training mode, skip actual completion
        if (model.isTrainingMode()) {
            addLog("Training mode: Submission simulated successfully");
            finishSubmission("TRAINING-" + System.currentTimeMillis());
            return;
        }

        // TODO: Call API to complete submission
        // For now, simulate completion
        addLog("Submission completed successfully!");
        finishSubmission(ticketId.get());
    }

    private void finishSubmission(String submissionId) {
        Platform.runLater(() -> {
            overallProgress.setProgress(1.0);
            currentFileLabel.setVisible(false);
            currentFileProgress.setVisible(false);
            cancelButton.setVisible(false);

            completed.set(true);
            uploading.set(false);

            updateStatus("Submission Complete!");
            addLog("=".repeat(40));
            addLog("Submission ID: " + submissionId);
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
                idLabel,
                new Label("You will receive a confirmation email shortly.")
            );
        });
    }

    private void cancelUpload() {
        uploading.set(false);
        if (checksumService != null && checksumService.isRunning()) {
            checksumService.cancel();
        }
        if (uploadManager != null && uploadManager.isRunning()) {
            uploadManager.cancel();
        }
        addLog("Upload cancelled by user");
        handleError("Upload cancelled");
    }

    private void handleError(String message) {
        Platform.runLater(() -> {
            uploading.set(false);
            startButton.setDisable(false);
            cancelButton.setVisible(false);
            overallProgress.setVisible(false);
            currentFileLabel.setVisible(false);
            currentFileProgress.setVisible(false);

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
}
