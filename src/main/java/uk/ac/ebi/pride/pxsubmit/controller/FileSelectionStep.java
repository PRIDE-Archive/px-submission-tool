package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.ValidationService;
import uk.ac.ebi.pride.pxsubmit.view.component.FileTableView;

import java.io.File;
import java.util.List;

/**
 * Step for selecting and categorizing submission files.
 * Features drag-drop support and automatic file type detection.
 */
public class FileSelectionStep extends AbstractWizardStep {

    private FileTableView fileTable;
    private Label summaryLabel;
    private Button addFilesButton;
    private Button addFolderButton;
    private Button removeSelectedButton;
    private Button validateButton;
    private ProgressBar validationProgress;
    private Label validationStatus;

    public FileSelectionStep(SubmissionModel model) {
        super("file-selection",
              "Select Files",
              "Add the files you want to include in your submission",
              model);
    }

    @Override
    protected Parent createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top: Instructions and buttons
        VBox topBox = new VBox(10);
        topBox.setPadding(new Insets(0, 0, 10, 0));

        Label instructionLabel = new Label(
            "Add files using the buttons below or drag and drop files into the table. " +
            "File types will be automatically detected but you can change them manually.");
        instructionLabel.setWrapText(true);
        instructionLabel.setStyle("-fx-text-fill: #666;");

        // Button bar
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        addFilesButton = new Button("Add Files...");
        addFilesButton.setOnAction(e -> addFiles());

        addFolderButton = new Button("Add Folder...");
        addFolderButton.setOnAction(e -> addFolder());

        removeSelectedButton = new Button("Remove Selected");
        removeSelectedButton.setOnAction(e -> removeSelected());
        removeSelectedButton.setDisable(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        validateButton = new Button("Validate Files");
        validateButton.setOnAction(e -> validateFiles());

        buttonBar.getChildren().addAll(
            addFilesButton, addFolderButton, removeSelectedButton,
            spacer, validateButton
        );

        topBox.getChildren().addAll(instructionLabel, buttonBar);
        root.setTop(topBox);

        // Center: File table
        fileTable = new FileTableView();
        fileTable.setDataFiles(model.getFiles());
        fileTable.setOnFilesDropped(this::addFilesFromDrop);
        fileTable.setOnFileRemoved(this::removeFile);
        fileTable.setOnFileTypeChanged(this::onFileTypeChanged);

        // Enable remove button when selection changes
        fileTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            removeSelectedButton.setDisable(fileTable.getSelectionModel().isEmpty());
        });

        root.setCenter(fileTable);

        // Bottom: Summary and validation status
        VBox bottomBox = new VBox(10);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));

        summaryLabel = new Label("No files added");
        summaryLabel.setStyle("-fx-font-weight: bold;");

        // Validation progress
        HBox validationBox = new HBox(10);
        validationBox.setAlignment(Pos.CENTER_LEFT);

        validationProgress = new ProgressBar(0);
        validationProgress.setPrefWidth(200);
        validationProgress.setVisible(false);

        validationStatus = new Label();
        validationStatus.setStyle("-fx-text-fill: #666;");

        validationBox.getChildren().addAll(validationProgress, validationStatus);

        bottomBox.getChildren().addAll(summaryLabel, validationBox);
        root.setBottom(bottomBox);

        return root;
    }

    @Override
    protected void initializeStep() {
        // Update summary when files change
        model.getFiles().addListener((javafx.collections.ListChangeListener.Change<? extends DataFile> c) -> {
            updateSummary();
        });

        // Valid when at least one file is added
        valid.bind(Bindings.isNotEmpty(model.getFiles()));

        updateSummary();
    }

    @Override
    protected void onStepEntering() {
        updateSummary();
    }

    /**
     * Open file chooser to add files
     */
    private void addFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Add");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("Raw Files", "*.raw", "*.RAW", "*.wiff", "*.d", "*.mzML", "*.mzXML"),
            new FileChooser.ExtensionFilter("Result Files", "*.mzid", "*.mzIdentML", "*.mzTab", "*.xml"),
            new FileChooser.ExtensionFilter("Search Files", "*.dat", "*.msf", "*.pepXML")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(null);
        if (files != null && !files.isEmpty()) {
            addFilesToModel(files);
        }
    }

    /**
     * Open folder chooser to add all files from a folder
     */
    private void addFolder() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Folder to Add");

        File directory = dirChooser.showDialog(null);
        if (directory != null && directory.isDirectory()) {
            File[] files = directory.listFiles(File::isFile);
            if (files != null && files.length > 0) {
                addFilesToModel(List.of(files));
            }
        }
    }

    /**
     * Handle files dropped via drag-and-drop
     */
    private void addFilesFromDrop(List<File> files) {
        addFilesToModel(files);
    }

    /**
     * Add files to the model
     */
    private void addFilesToModel(List<File> files) {
        for (File file : files) {
            if (file.isFile()) {
                // Check if file already exists
                boolean exists = model.getFiles().stream()
                    .anyMatch(df -> df.getFile() != null &&
                                   df.getFile().getAbsolutePath().equals(file.getAbsolutePath()));

                if (!exists) {
                    DataFile dataFile = new DataFile();
                    dataFile.setFile(file);
                    dataFile.setFileType(detectFileType(file));
                    model.addFile(dataFile);
                    logger.debug("Added file: {}", file.getName());
                }
            } else if (file.isDirectory()) {
                // Recursively add files from directory
                File[] subFiles = file.listFiles(File::isFile);
                if (subFiles != null) {
                    addFilesToModel(List.of(subFiles));
                }
            }
        }
        updateSummary();
    }

    /**
     * Detect file type based on extension and format
     */
    private ProjectFileType detectFileType(File file) {
        try {
            MassSpecFileFormat format = MassSpecFileFormat.checkFormat(file);

            if (format != null) {
                return switch (format) {
                    case PRIDE, MZIDENTML, MZTAB -> ProjectFileType.RESULT;
                    case MZML, INDEXED_MZML -> ProjectFileType.RAW;
                    default -> guessTypeFromExtension(file);
                };
            }
        } catch (java.io.IOException e) {
            logger.debug("Could not determine file format for {}: {}", file.getName(), e.getMessage());
        }

        return guessTypeFromExtension(file);
    }

    private ProjectFileType guessTypeFromExtension(File file) {
        String name = file.getName().toLowerCase();

        // Raw files
        if (name.endsWith(".raw") || name.endsWith(".wiff") || name.endsWith(".d") ||
            name.endsWith(".baf") || name.endsWith(".tdf")) {
            return ProjectFileType.RAW;
        }

        // Result files
        if (name.endsWith(".mzid") || name.endsWith(".mzidentml") ||
            name.endsWith(".mztab") || name.endsWith(".pride.xml")) {
            return ProjectFileType.RESULT;
        }

        // Search files
        if (name.endsWith(".dat") || name.endsWith(".msf") || name.endsWith(".pep.xml") ||
            name.endsWith(".pepxml") || name.endsWith(".t.xml")) {
            return ProjectFileType.SEARCH;
        }

        // Peak files
        if (name.endsWith(".mgf") || name.endsWith(".dta") || name.endsWith(".pkl") ||
            name.endsWith(".ms2") || name.endsWith(".apl")) {
            return ProjectFileType.PEAK;
        }

        // SDRF/experimental design
        if (name.equals("sdrf.tsv") || name.endsWith(".sdrf.tsv")) {
            return ProjectFileType.EXPERIMENTAL_DESIGN;
        }

        // Affinity files
        if (name.endsWith(".adat") || name.endsWith(".npx") || name.endsWith(".parquet")) {
            return ProjectFileType.RAW;
        }

        // Default to OTHER
        return ProjectFileType.OTHER;
    }

    /**
     * Remove selected files
     */
    private void removeSelected() {
        List<DataFile> selected = fileTable.getSelectedFiles();
        for (DataFile file : selected) {
            model.removeFile(file);
        }
        updateSummary();
    }

    /**
     * Remove a specific file
     */
    private void removeFile(DataFile file) {
        model.removeFile(file);
        updateSummary();
    }

    /**
     * Handle file type change
     */
    private void onFileTypeChanged(DataFile file) {
        logger.debug("File type changed for {}: {}", file.getFileName(), file.getFileType());
    }

    /**
     * Validate all files
     */
    private void validateFiles() {
        if (model.getFiles().isEmpty()) {
            validationStatus.setText("No files to validate");
            return;
        }

        ValidationService validationService = new ValidationService(
            model.getFiles(), model.getSubmissionType());

        validationProgress.setVisible(true);
        validationProgress.progressProperty().bind(validationService.progressProperty());
        validationStatus.textProperty().bind(validationService.messageProperty());

        validateButton.setDisable(true);

        validationService.setOnSucceeded(e -> {
            ValidationService.ValidationResult result = validationService.getValue();
            Platform.runLater(() -> {
                validationProgress.setVisible(false);
                validationProgress.progressProperty().unbind();
                validationStatus.textProperty().unbind();
                validateButton.setDisable(false);

                if (result.isValid()) {
                    validationStatus.setText("Validation passed!");
                    validationStatus.setStyle("-fx-text-fill: #28a745;");
                } else {
                    validationStatus.setText("Validation failed: " + result.getErrors().size() + " error(s)");
                    validationStatus.setStyle("-fx-text-fill: #dc3545;");

                    // Show errors in dialog
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Validation Errors");
                    alert.setHeaderText("The following errors were found:");
                    alert.setContentText(result.getErrorSummary());
                    alert.showAndWait();
                }

                if (result.hasWarnings()) {
                    logger.warn("Validation warnings: {}", result.getWarningSummary());
                }
            });
        });

        validationService.setOnFailed(e -> {
            Platform.runLater(() -> {
                validationProgress.setVisible(false);
                validationProgress.progressProperty().unbind();
                validationStatus.textProperty().unbind();
                validateButton.setDisable(false);
                validationStatus.setText("Validation error: " + e.getSource().getException().getMessage());
                validationStatus.setStyle("-fx-text-fill: #dc3545;");
            });
        });

        validationService.start();
    }

    /**
     * Update the summary label
     */
    private void updateSummary() {
        int total = model.getFiles().size();
        if (total == 0) {
            summaryLabel.setText("No files added");
            return;
        }

        // Count by type
        long rawCount = model.getFiles().stream()
            .filter(f -> f.getFileType() == ProjectFileType.RAW).count();
        long resultCount = model.getFiles().stream()
            .filter(f -> f.getFileType() == ProjectFileType.RESULT).count();
        long searchCount = model.getFiles().stream()
            .filter(f -> f.getFileType() == ProjectFileType.SEARCH).count();
        long peakCount = model.getFiles().stream()
            .filter(f -> f.getFileType() == ProjectFileType.PEAK).count();
        long otherCount = total - rawCount - resultCount - searchCount - peakCount;

        // Calculate total size
        long totalSize = model.getFiles().stream()
            .filter(f -> f.getFile() != null)
            .mapToLong(f -> f.getFile().length())
            .sum();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d files (%s) - ", total, formatSize(totalSize)));

        if (rawCount > 0) sb.append(rawCount).append(" raw, ");
        if (resultCount > 0) sb.append(resultCount).append(" result, ");
        if (searchCount > 0) sb.append(searchCount).append(" search, ");
        if (peakCount > 0) sb.append(peakCount).append(" peak, ");
        if (otherCount > 0) sb.append(otherCount).append(" other");

        String summary = sb.toString();
        if (summary.endsWith(", ")) {
            summary = summary.substring(0, summary.length() - 2);
        }

        summaryLabel.setText(summary);
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
