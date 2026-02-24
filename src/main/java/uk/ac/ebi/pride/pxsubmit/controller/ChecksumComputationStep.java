package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.ChecksumService;
import uk.ac.ebi.pride.pxsubmit.service.ServiceFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Step for computing file checksums with detailed progress display.
 * Shows all files in a table with status indicators.
 * Only appears if checksums haven't been computed yet.
 */
public class ChecksumComputationStep extends AbstractWizardStep {

    private static final Logger logger = LoggerFactory.getLogger(ChecksumComputationStep.class);

    // UI components
    private TableView<FileChecksumRow> fileTable;
    private ProgressBar overallProgress;
    private Label statusLabel;
    private Label progressLabel;
    private Label etaLabel;
    private Button startButton;
    private Button cancelButton;

    // State
    private final BooleanProperty computing = new SimpleBooleanProperty(false);
    private final BooleanProperty completed = new SimpleBooleanProperty(false);
    private ChecksumService checksumService;
    private final Map<String, FileChecksumRow> fileRowMap = new ConcurrentHashMap<>();
    private long computationStartTime;

    public ChecksumComputationStep(SubmissionModel model) {
        super("checksum-computation",
              "Computing Checksums",
              "Calculating file checksums for integrity verification",
              model);
    }

    @Override
    protected Parent createContent() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));

        // Header
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("File Checksum Computation");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label descLabel = new Label(
            "Computing SHA-1 checksums for all files to ensure data integrity during transfer."
        );
        descLabel.setStyle("-fx-text-fill: #666;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(700);

        header.getChildren().addAll(titleLabel, descLabel);

        // File table
        fileTable = new TableView<>();
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(fileTable, Priority.ALWAYS);

        // Status column (icon)
        TableColumn<FileChecksumRow, Region> statusCol = new TableColumn<>("");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("statusIcon"));
        statusCol.setPrefWidth(50);
        statusCol.setMaxWidth(50);
        statusCol.setMinWidth(50);
        statusCol.setResizable(false);
        statusCol.setSortable(false);

        // Filename column
        TableColumn<FileChecksumRow, String> nameCol = new TableColumn<>("Filename");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        nameCol.setPrefWidth(300);

        // Size column
        TableColumn<FileChecksumRow, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        sizeCol.setPrefWidth(100);

        // Checksum column
        TableColumn<FileChecksumRow, String> checksumCol = new TableColumn<>("Checksum (SHA-1)");
        checksumCol.setCellValueFactory(new PropertyValueFactory<>("checksum"));
        checksumCol.setPrefWidth(280);

        fileTable.getColumns().addAll(statusCol, nameCol, sizeCol, checksumCol);

        // Progress section
        VBox progressSection = new VBox(10);
        progressSection.setPadding(new Insets(15));
        progressSection.setStyle(
            "-fx-background-color: #f8f9fa; " +
            "-fx-border-color: #e9ecef; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5;"
        );

        statusLabel = new Label("Ready to compute checksums");
        statusLabel.setStyle("-fx-font-weight: bold;");

        progressLabel = new Label("0 / 0 files processed");
        progressLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        etaLabel = new Label("");
        etaLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
        etaLabel.setVisible(false);
        etaLabel.setManaged(false);

        overallProgress = new ProgressBar(0);
        overallProgress.setPrefWidth(Double.MAX_VALUE);
        overallProgress.setPrefHeight(25);

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        startButton = new Button("Start Computing Checksums");
        startButton.setStyle(
            "-fx-background-color: #0066cc; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 10 20;"
        );
        startButton.setOnAction(e -> startComputation());

        cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> cancelComputation());
        cancelButton.setVisible(false);

        buttonBox.getChildren().addAll(startButton, cancelButton);

        progressSection.getChildren().addAll(
            statusLabel,
            progressLabel,
            etaLabel,
            overallProgress,
            buttonBox
        );

        root.getChildren().addAll(header, fileTable, progressSection);

        return root;
    }

    @Override
    protected void initializeStep() {
        // Valid when computation is complete
        valid.bind(completed);
    }

    @Override
    protected void onStepEntering() {
        logger.info("=== ENTERING CHECKSUM COMPUTATION STEP ===");
        logger.info("Number of files in model: {}", model.getFiles().size());
        logger.info("Checksums already calculated: {}", model.areChecksumsCalculated());
        logger.info("Number of existing checksums: {}", model.getChecksums().size());

        // Reset state
        computing.set(false);
        completed.set(false);
        overallProgress.setProgress(0);
        fileRowMap.clear();

        // Populate table with files
        populateFileTable();
        logger.info("Files added to table: {}", fileTable.getItems().size());

        // Check if checksums are already computed and still valid
        if (model.areChecksumsCalculated() && !model.getChecksums().isEmpty()) {
            // Count actual files (excluding checksum.txt)
            long fileCount = model.getFiles().stream()
                .filter(f -> !"checksum.txt".equals(f.getFileName()))
                .count();
            long checksumCount = model.getChecksums().size();

            if (fileCount == checksumCount) {
                logger.info("Checksums already computed and valid, marking all as completed");
                markAllAsCompleted();
                completed.set(true);
                logger.info("Step marked as completed, but NOT auto-advancing");
                return;
            } else {
                logger.info("File count changed ({} files vs {} checksums), recomputing", fileCount, checksumCount);
                model.clearChecksums();
            }
        }

        // Auto-start computation
        logger.info("Starting checksum computation automatically");
        Platform.runLater(this::startComputation);
    }

    @Override
    protected void onStepLeaving() {
        // Cleanup if needed
    }

    private void populateFileTable() {
        fileTable.getItems().clear();

        for (DataFile dataFile : model.getFiles()) {
            // Skip checksum.txt itself
            if ("checksum.txt".equals(dataFile.getFileName())) {
                continue;
            }

            FileChecksumRow row = new FileChecksumRow(dataFile);
            fileTable.getItems().add(row);
            fileRowMap.put(dataFile.getFileName(), row);
        }

        int total = fileTable.getItems().size();
        progressLabel.setText("0 / " + total + " files processed");
    }

    private void markAllAsCompleted() {
        Map<DataFile, String> existingChecksums = model.getChecksums();
        for (FileChecksumRow row : fileTable.getItems()) {
            String checksum = existingChecksums.get(row.getDataFile());
            if (checksum != null) {
                row.setChecksum(checksum);
                row.setCompleted(true);
            }
        }
        statusLabel.setText("All checksums already computed");
        overallProgress.setProgress(1.0);
        progressLabel.setText(fileTable.getItems().size() + " / " + fileTable.getItems().size() + " files processed");
    }

    private void startComputation() {
        if (fileTable.getItems().isEmpty()) {
            logger.warn("No files to process");
            completed.set(true);
            return;
        }

        computing.set(true);
        startButton.setDisable(true);
        startButton.setVisible(false);
        cancelButton.setVisible(true);
        etaLabel.setVisible(true);
        etaLabel.setManaged(true);
        etaLabel.setText("Estimated time: calculating...");
        computationStartTime = System.currentTimeMillis();

        statusLabel.setText("Computing checksums...");
        logger.info("Starting checksum computation for {} files", model.getFiles().size());

        checksumService = ServiceFactory.getInstance().createChecksumService(model.getFiles());

        // Monitor current file
        checksumService.currentFileNameProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                if (newVal != null && !newVal.isEmpty()) {
                    FileChecksumRow row = fileRowMap.get(newVal);
                    if (row != null) {
                        row.setStatus(FileChecksumStatus.PROCESSING);
                    }
                }
            });
        });

        // Monitor progress
        checksumService.progressProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> overallProgress.setProgress(newVal.doubleValue()));
        });

        checksumService.filesProcessedProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                int processed = newVal.intValue();
                int total = checksumService.getTotalFiles();
                progressLabel.setText(processed + " / " + total + " files processed");

                // Calculate ETA
                if (processed > 0 && processed < total) {
                    long elapsed = System.currentTimeMillis() - computationStartTime;
                    long estimatedTotal = (elapsed * total) / processed;
                    long remaining = estimatedTotal - elapsed;
                    etaLabel.setText("Estimated time remaining: " + formatDuration(remaining));
                } else if (processed >= total) {
                    long elapsed = System.currentTimeMillis() - computationStartTime;
                    etaLabel.setText("Completed in " + formatDuration(elapsed));
                }
            });
        });

        // Handle success
        checksumService.setOnSucceeded(e -> {
            Map<DataFile, String> checksums = checksumService.getValue();
            logger.info("Checksums computed successfully for {} files", checksums.size());

            Platform.runLater(() -> {
                // Update table rows with checksums
                for (Map.Entry<DataFile, String> entry : checksums.entrySet()) {
                    String fileName = entry.getKey().getFileName();
                    FileChecksumRow row = fileRowMap.get(fileName);
                    if (row != null) {
                        row.setChecksum(entry.getValue());
                        row.setCompleted(true);
                    }
                }

                // Store in model
                model.setChecksums(checksums);

                // Write checksum.txt file
                writeChecksumFile(checksums);

                // Update UI
                statusLabel.setText("Everything done! All checksums computed successfully.");
                statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");
                cancelButton.setVisible(false);
                computing.set(false);
                completed.set(true);

                logger.info("Checksum computation completed successfully");

                // Don't auto-advance - let user review the checksums
                // User will click "Continue to Upload" button when ready
            });
        });

        // Handle failure
        checksumService.setOnFailed(e -> {
            Throwable ex = e.getSource().getException();
            logger.error("Checksum computation failed", ex);

            Platform.runLater(() -> {
                statusLabel.setText("Error: Checksum computation failed");
                statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545;");
                computing.set(false);
                startButton.setDisable(false);
                startButton.setVisible(true);
                cancelButton.setVisible(false);

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Checksum Error");
                alert.setHeaderText("Failed to compute checksums");
                alert.setContentText(ex != null ? ex.getMessage() : "Unknown error");
                alert.showAndWait();
            });
        });

        // Handle cancellation
        checksumService.setOnCancelled(e -> {
            logger.info("Checksum computation cancelled");

            Platform.runLater(() -> {
                statusLabel.setText("Checksum computation cancelled");
                statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545;");
                computing.set(false);
                startButton.setDisable(false);
                startButton.setVisible(true);
                cancelButton.setVisible(false);

                // Reset all rows to pending
                for (FileChecksumRow row : fileTable.getItems()) {
                    if (row.getStatus() != FileChecksumStatus.COMPLETED) {
                        row.setStatus(FileChecksumStatus.PENDING);
                    }
                }
            });
        });

        // Start the service
        checksumService.start();
    }

    private void cancelComputation() {
        if (checksumService != null && checksumService.isRunning()) {
            checksumService.cancel();
        }
    }

    private void writeChecksumFile(Map<DataFile, String> checksums) {
        try {
            // Write checksum.txt to the current working directory
            File workingDir = new File(System.getProperty("user.dir"));
            File checksumFile = ChecksumService.writeChecksumFile(checksums, workingDir);

            logger.info("Checksum file written: {}", checksumFile.getAbsolutePath());

            // Add checksum.txt to the file list if not already present
            boolean alreadyExists = model.getFiles().stream()
                .anyMatch(f -> "checksum.txt".equals(f.getFileName()));

            if (!alreadyExists) {
                DataFile checksumDataFile = new DataFile();
                checksumDataFile.setFile(checksumFile);
                checksumDataFile.setFileType(uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType.OTHER);
                model.addFile(checksumDataFile);
                logger.info("Added checksum.txt to submission file list");
            }
        } catch (IOException ex) {
            logger.warn("Could not write checksum file", ex);
        }
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
            return String.format("%ds", Math.max(1, seconds));
        }
    }

    @Override
    public boolean showBackButton() {
        return !computing.get();
    }

    @Override
    public String getNextButtonText() {
        return "Next";
    }

    /**
     * Enum for file checksum status
     */
    public enum FileChecksumStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    /**
     * Table row model for file checksum display
     */
    public static class FileChecksumRow {
        private final DataFile dataFile;
        private final StringProperty fileName;
        private final StringProperty fileSize;
        private final StringProperty checksum;
        private final ObjectProperty<FileChecksumStatus> status;
        private final ObjectProperty<Region> statusIcon;

        public FileChecksumRow(DataFile dataFile) {
            this.dataFile = dataFile;
            this.fileName = new SimpleStringProperty(dataFile.getFileName());
            this.fileSize = new SimpleStringProperty(formatSize(
                dataFile.getFile() != null ? dataFile.getFile().length() : 0
            ));
            this.checksum = new SimpleStringProperty("-");
            this.status = new SimpleObjectProperty<>(FileChecksumStatus.PENDING);
            this.statusIcon = new SimpleObjectProperty<>(createStatusIcon(FileChecksumStatus.PENDING));

            // Update icon when status changes
            this.status.addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> statusIcon.set(createStatusIcon(newVal)));
            });
        }

        private Region createStatusIcon(FileChecksumStatus status) {
            StackPane pane = new StackPane();
            pane.setAlignment(Pos.CENTER);
            pane.setPrefSize(24, 24);

            Circle circle = new Circle(10);

            switch (status) {
                case PENDING:
                    circle.setFill(Color.LIGHTGRAY);
                    circle.setStroke(Color.GRAY);
                    circle.setStrokeWidth(1);
                    break;

                case PROCESSING:
                    circle.setFill(Color.LIGHTYELLOW);
                    circle.setStroke(Color.ORANGE);
                    circle.setStrokeWidth(2);
                    break;

                case COMPLETED:
                    circle.setFill(Color.LIGHTGREEN);
                    circle.setStroke(Color.GREEN);
                    circle.setStrokeWidth(2);

                    // Add checkmark
                    Label checkmark = new Label("✓");
                    checkmark.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 14px;");
                    pane.getChildren().add(checkmark);
                    break;

                case FAILED:
                    circle.setFill(Color.LIGHTCORAL);
                    circle.setStroke(Color.RED);
                    circle.setStrokeWidth(2);

                    // Add X mark
                    Label xmark = new Label("✗");
                    xmark.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");
                    pane.getChildren().add(xmark);
                    break;
            }

            pane.getChildren().add(0, circle);
            return pane;
        }

        private static String formatSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }

        // Getters and setters
        public DataFile getDataFile() {
            return dataFile;
        }

        public String getFileName() {
            return fileName.get();
        }

        public StringProperty fileNameProperty() {
            return fileName;
        }

        public String getFileSize() {
            return fileSize.get();
        }

        public StringProperty fileSizeProperty() {
            return fileSize;
        }

        public String getChecksum() {
            return checksum.get();
        }

        public void setChecksum(String value) {
            this.checksum.set(value);
        }

        public StringProperty checksumProperty() {
            return checksum;
        }

        public FileChecksumStatus getStatus() {
            return status.get();
        }

        public void setStatus(FileChecksumStatus value) {
            this.status.set(value);
        }

        public ObjectProperty<FileChecksumStatus> statusProperty() {
            return status;
        }

        public void setCompleted(boolean completed) {
            setStatus(completed ? FileChecksumStatus.COMPLETED : FileChecksumStatus.PENDING);
        }

        public Region getStatusIcon() {
            return statusIcon.get();
        }

        public ObjectProperty<Region> statusIconProperty() {
            return statusIcon;
        }
    }
}
