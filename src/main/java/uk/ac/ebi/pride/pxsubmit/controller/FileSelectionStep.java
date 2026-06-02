package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.PrideCommonsFileValidationService;
import uk.ac.ebi.pride.pxsubmit.service.SdrfParserService;
import uk.ac.ebi.pride.pxsubmit.service.SdrfValidationOptions;
import uk.ac.ebi.pride.pxsubmit.service.SdrfValidatorApi;
import uk.ac.ebi.pride.pxsubmit.service.ServiceFactory;
import uk.ac.ebi.pride.pxsubmit.service.ValidationService;
import uk.ac.ebi.pride.submissions.commons.exceptions.SubValidationException;
import uk.ac.ebi.pride.pxsubmit.util.FileTypeDetector;
import uk.ac.ebi.pride.pxsubmit.view.component.FileClassificationPanel;
import uk.ac.ebi.pride.pxsubmit.view.component.FileTableView;
import uk.ac.ebi.pride.pxsubmit.view.component.ValidationFeedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Step for selecting and categorizing submission files.
 * Features drag-drop support and automatic file type detection.
 */
public class FileSelectionStep extends AbstractWizardStep {

    private FileTableView fileTable;
    private FileClassificationPanel classificationPanel;
    private ValidationFeedback validationFeedback;
    private Label summaryLabel;
    private Button addFilesButton;
    private Button addFolderButton;
    private Button removeSelectedButton;
    private ProgressBar validationProgress;
    private Label validationStatus;

    private volatile CompletableFuture<List<String>> sdrfTemplatesLoadFuture;
    private List<String> sdrfTemplateNames = new ArrayList<>();
    /** Signature of the SDRF file set currently tracked. */
    private String sdrfPopupSignature;
    /** Signature last known to have passed SDRF validation (cleared when SDRF files change). */
    private String sdrfValidatedSignature;
    /** Absolute file path → PRIDE commons validation passed (table status column). */
    private final Map<String, Boolean> prideValidationByPath = new HashMap<>();
    /** Absolute file path → SDRF validation passed (shown in file table status column). */
    private final Map<String, Boolean> sdrfValidationByPath = new HashMap<>();

    private final PrideCommonsFileValidationService prideFileValidationService =
            ServiceFactory.getInstance().createPrideCommonsFileValidationService();

    public FileSelectionStep(SubmissionModel model) {
        super("file-selection",
              "Select Files",
              "Add the files you want to include in your submission",
              model);
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent;");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top: Instructions and buttons
        VBox topBox = new VBox(5);
        topBox.setPadding(new Insets(0, 0, 5, 0));

        Label instructionLabel = new Label(
            "Add files using the buttons below or drag and drop files into the table. " +
            "File types will be automatically detected but you can change them manually.");
        instructionLabel.setWrapText(true);
        instructionLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        // Button bar
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        addFilesButton = new Button("Select Files");
        addFilesButton.setOnAction(e -> addFiles());

        addFolderButton = new Button("Select Files from Folder");
        addFolderButton.setOnAction(e -> addFolder());

        removeSelectedButton = new Button("Remove Selected");
        removeSelectedButton.setOnAction(e -> removeSelected());
        removeSelectedButton.setDisable(true);

        buttonBar.getChildren().addAll(
            addFilesButton, addFolderButton, removeSelectedButton
        );

        // File classification panel
        classificationPanel = new FileClassificationPanel();
        classificationPanel.setShowDetails(true);
        classificationPanel.setShowWarnings(true);
        classificationPanel.setOnTypeSelected((type, files) -> {
            // Filter table to show only files of this type
            fileTable.filterByType(type);
        });

        topBox.getChildren().addAll(instructionLabel, buttonBar, classificationPanel);
        root.setTop(topBox);

        // Create file table first so search can bind to it
        fileTable = new FileTableView();
        fileTable.setDataFiles(model.getFiles());
        fileTable.setOnFilesDropped(this::addFilesFromDrop);
        fileTable.setOnFileRemoved(this::removeFile);
        fileTable.setOnFileTypeChanged(this::onFileTypeChanged);
        fileTable.setChecksumLookup(df -> model.getChecksum(df));
        fileTable.setPrideValidationLookup(this::lookupPrideValidationForTable);
        fileTable.setSdrfValidationLookup(this::lookupSdrfValidationForTable);

        // Enable remove button when selection changes
        fileTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            removeSelectedButton.setDisable(fileTable.getSelectionModel().isEmpty());
        });

        // Search field right above the table, aligned right
        HBox searchBar = new HBox(8);
        searchBar.setAlignment(Pos.CENTER_RIGHT);
        searchBar.setPadding(new Insets(0, 0, 5, 0));

        Label searchIcon = new Label("\u2315");
        searchIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");

        TextField searchField = fileTable.createSearchField();
        searchField.setPrefWidth(250);

        searchBar.getChildren().addAll(searchIcon, searchField);

        VBox centerBox = new VBox(0);
        centerBox.getChildren().addAll(searchBar, fileTable);
        VBox.setVgrow(fileTable, Priority.ALWAYS);
        root.setCenter(centerBox);

        // Bottom: Summary, pagination, validation feedback, and validation status
        VBox bottomBox = new VBox(5);
        bottomBox.setPadding(new Insets(5, 0, 0, 0));

        // Pagination controls (auto-hidden when < 1000 files)
        javafx.scene.layout.HBox paginationControls = fileTable.createPaginationControls();
        bottomBox.getChildren().add(paginationControls);

        summaryLabel = new Label("No files added");
        summaryLabel.setStyle("-fx-font-weight: bold;");

        // Inline validation feedback
        validationFeedback = new ValidationFeedback();

        // Validation progress
        HBox validationBox = new HBox(10);
        validationBox.setAlignment(Pos.CENTER_LEFT);

        validationProgress = new ProgressBar(0);
        validationProgress.setPrefWidth(200);
        validationProgress.setVisible(false);

        validationStatus = new Label();
        validationStatus.setStyle("-fx-text-fill: #666;");

        validationBox.getChildren().addAll(validationProgress, validationStatus);

        bottomBox.getChildren().addAll(summaryLabel, validationFeedback, validationBox);
        root.setBottom(bottomBox);

        scrollPane.setContent(root);
        return scrollPane;
    }

    @Override
    protected void initializeStep() {
        // Update summary when files change
        model.getFiles().addListener((javafx.collections.ListChangeListener.Change<? extends DataFile> c) -> {
            updateSummary();
        });

        ensureSdrfTemplatesLoaded();
        preloadPrideValidationRules();
        updateSummary();
    }

    @Override
    protected void onStepEntering() {
        ensureSdrfTemplatesLoaded();
        preloadPrideValidationRules();
        updateSummary();
    }

    @Override
    public boolean validate() {
        classificationPanel.setFiles(model.getFiles());
        if (model.getFiles().isEmpty() || !classificationPanel.hasAllMandatoryFiles()) {
            return false;
        }

        if (!runPrideCommonsFileValidation()) {
            updateSummary();
            return false;
        }

        List<File> sdrfFiles = getSdrfFiles();
        if (sdrfFiles.isEmpty()) {
            updateSummary();
            return true;
        }

        ensureSdrfSignatureSynced();
        String signature = buildSdrfPopupSignature(sdrfFiles);

        if (signature.equals(sdrfValidatedSignature) && allSdrfFilesPassedValidation()) {
            return true;
        }

        showSdrfValidationDialog(sdrfFiles);
        return false;
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
     * Open folder chooser to add all files from a folder (recursively)
     */
    private void addFolder() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Folder to Add");

        File directory = dirChooser.showDialog(null);
        if (directory != null && directory.isDirectory()) {
            // Scan folder recursively in background
            scanFolderRecursively(directory);
        }
    }

    /**
     * Recursively scan a folder and add all files
     */
    private void scanFolderRecursively(File directory) {
        // Create a simple progress stage
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("Scanning Folder");
        progressStage.setResizable(false);

        VBox progressBox = new VBox(15);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(20));
        progressBox.setStyle("-fx-background-color: white;");

        Label messageLabel = new Label("Scanning folder recursively...");
        messageLabel.setStyle("-fx-font-size: 14px;");

        Label folderLabel = new Label(directory.getName());
        folderLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);

        progressBox.getChildren().addAll(messageLabel, folderLabel, progressIndicator);

        Scene scene = new Scene(progressBox, 300, 150);
        progressStage.setScene(scene);
        progressStage.show();

        // Scan in background thread
        Thread scanThread = new Thread(() -> {
            try {
                List<File> foundFiles = new ArrayList<>();

                // Recursively walk the directory tree
                try (Stream<Path> paths = Files.walk(directory.toPath())) {
                    foundFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> !isHiddenOrSystemFile(path))
                        .map(Path::toFile)
                        .collect(Collectors.toList());
                } catch (IOException e) {
                    logger.error("Error scanning folder: " + directory.getAbsolutePath(), e);
                    Platform.runLater(() -> {
                        progressStage.close();
                        showError("Folder Scan Error",
                            "Failed to scan folder: " + e.getMessage());
                    });
                    return;
                }

                final List<File> filesToAdd = foundFiles;
                final int totalFiles = filesToAdd.size();

                Platform.runLater(() -> {
                    // Close progress window
                    progressStage.close();

                    if (totalFiles == 0) {
                        showInfo("No Files Found",
                            "No files were found in the selected folder.");
                        return;
                    }

                    // Check for existing files
                    List<File> newFiles = filesToAdd.stream()
                        .filter(file -> model.getFiles().stream()
                            .noneMatch(df -> df.getFile() != null &&
                                df.getFile().getAbsolutePath().equals(file.getAbsolutePath())))
                        .collect(Collectors.toList());

                    int duplicateCount = totalFiles - newFiles.size();

                    // Add files directly without confirmation
                    addFilesToModel(newFiles);
                    logger.info("Added {} files from folder {}", newFiles.size(), directory.getName());
                });

            } catch (Exception e) {
                logger.error("Error during folder scan", e);
                Platform.runLater(() -> {
                    progressStage.close();
                    showError("Scan Error", "Unexpected error: " + e.getMessage());
                });
            }
        });

        scanThread.setDaemon(true);
        scanThread.setName("Folder-Scanner");
        scanThread.start();
    }

    /**
     * Check if a file is hidden or a system file
     */
    private boolean isHiddenOrSystemFile(Path path) {
        try {
            String fileName = path.getFileName().toString();
            // Skip hidden files (starting with .)
            if (fileName.startsWith(".")) {
                return true;
            }
            // Skip common system/temp files
            if (fileName.equals("Thumbs.db") || fileName.equals("Desktop.ini") ||
                fileName.equals(".DS_Store") || fileName.startsWith("~$")) {
                return true;
            }
            // Check if hidden attribute is set (Windows)
            if (Files.isHidden(path)) {
                return true;
            }
            return false;
        } catch (IOException e) {
            return false; // If we can't check, assume it's not hidden
        }
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            logger.warn("Could not open URL: {}", url, e);
            showInfo("Open in browser", "Please open this link in your browser:\n" + url);
        }
    }

    /**
     * Show error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show info dialog
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show warning dialog
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Handle files dropped via drag-and-drop
     */
    private void addFilesFromDrop(List<File> files) {
        // Check if any directories were dropped
        List<File> directories = files.stream()
            .filter(File::isDirectory)
            .collect(Collectors.toList());

        List<File> regularFiles = files.stream()
            .filter(File::isFile)
            .collect(Collectors.toList());

        // Add regular files immediately
        if (!regularFiles.isEmpty()) {
            addFilesToModel(regularFiles);
        }

        // Handle directories with confirmation
        if (!directories.isEmpty()) {
            if (directories.size() == 1) {
                // Single directory - scan recursively with confirmation
                scanFolderRecursively(directories.get(0));
            } else {
                // Multiple directories - ask user
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Add Folders");
                alert.setHeaderText("Scan " + directories.size() + " folder(s) recursively?");
                alert.setContentText("This will add all files from these folders and their subfolders.");

                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        for (File dir : directories) {
                            scanFolderRecursively(dir);
                        }
                    }
                });
            }
        }
    }

    /**
     * Add files to the model (for regular files only, not directories)
     */
    private void addFilesToModel(List<File> files) {
        int addedCount = 0;
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
                    logger.debug("Added file: {} (type: {})", file.getName(), dataFile.getFileType());
                    addedCount++;
                }
            }
        }

        if (addedCount > 0) {
            logger.info("Added {} file(s) to submission", addedCount);
        }

        updateSummary();
        syncValidationStatusWithCurrentFiles();
    }

    /**
     * Detect file type using FileTypeDetector
     */
    private ProjectFileType detectFileType(File file) {
        try {
            return FileTypeDetector.detectFileType(file);
        } catch (Exception e) {
            // Handle files without extensions or other detection errors
            logger.warn("Could not detect file type for: {} - {}", file.getName(), e.getMessage());
            System.err.println("Warning: Could not detect file type for: " + file.getName() + " - " + e.getMessage());

            // For files without extensions, try to infer from name
            String name = file.getName().toLowerCase();
            if (name.equals("readme") || name.equals("license") || name.equals("changelog")) {
                return ProjectFileType.OTHER;
            }

            // Default to OTHER for unknown files
            return ProjectFileType.OTHER;
        }
    }

    /**
     * Remove selected files
     */
    private void removeSelected() {
        // Copy the list since getSelectedFiles() returns a live view
        // that changes as items are removed from the model
        List<DataFile> selected = new ArrayList<>(fileTable.getSelectedFiles());
        if (selected.isEmpty()) return;
        for (DataFile file : selected) {
            model.removeFile(file);
        }
        fileTable.getSelectionModel().clearSelection();
        updateSummary();
        syncValidationStatusWithCurrentFiles();
    }

    /**
     * Remove a specific file
     */
    private void removeFile(DataFile file) {
        model.removeFile(file);
        updateSummary();
        syncValidationStatusWithCurrentFiles();
    }

    /**
     * Handle file type change
     */
    private void onFileTypeChanged(DataFile file) {
        logger.debug("File type changed for {}: {}", file.getFileName(), file.getFileType());
        updateSummary();
        syncValidationStatusWithCurrentFiles();
    }

    /**
     * Validate all files
     */
    private void validateFiles() {
        if (model.getFiles().isEmpty()) {
            validationStatus.setText("No files to validate");
            return;
        }

        ValidationService validationService = ServiceFactory.getInstance().createValidationService(
            model.getFiles(), model.getSubmissionType());

        validationProgress.setVisible(true);
        validationProgress.progressProperty().bind(validationService.progressProperty());
        validationStatus.textProperty().bind(validationService.messageProperty());
        if (wizardController != null) wizardController.showGlobalProgress();

        validationService.setOnSucceeded(e -> {
            ValidationService.ValidationResult result = validationService.getValue();
            Platform.runLater(() -> {
                validationProgress.setVisible(false);
                validationProgress.progressProperty().unbind();
                validationStatus.textProperty().unbind();
                if (wizardController != null) wizardController.hideGlobalProgress();

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
                if (wizardController != null) wizardController.hideGlobalProgress();
                validationStatus.setText("Validation error: " + e.getSource().getException().getMessage());
                validationStatus.setStyle("-fx-text-fill: #dc3545;");
            });
        });

        validationService.start();
    }

    /**
     * Update the summary label, classification panel, and validation feedback
     */
    private void updateSummary() {
        // Update classification panel
        classificationPanel.setFiles(model.getFiles());

        // Clear and update validation feedback
        validationFeedback.clear();

        boolean hasSdrf = !getSdrfFiles().isEmpty();
        int total = model.getFiles().size();
        boolean sdrfValidationPassed = allSdrfFilesPassedValidation();

        setValid(classificationPanel.hasAllMandatoryFiles());

        if (total == 0) {
            summaryLabel.setText("No files added - drag and drop files or use the buttons above");
            summaryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
            validationFeedback.addInfo("Add your RAW files (mandatory) and analysis outputs to proceed");
            return;
        }

        // Count file types
        long rawCount = model.getFiles().stream()
            .filter(f -> f.getFileType() == ProjectFileType.RAW).count();
        long analysisCount = model.getFiles().stream()
            .filter(f -> f.getFileType() == ProjectFileType.SEARCH).count();
        long resultCount = model.getFiles().stream()
            .filter(f -> f.getFileType() == ProjectFileType.RESULT).count();
        boolean hasFasta = model.getFiles().stream()
            .anyMatch(f -> FileTypeDetector.isFastaFile(f.getFile()));

        // File requirement validation
        if (rawCount == 0) {
            validationFeedback.addError("RAW files are required - add your instrument output files (.raw, .wiff, .d, .mzML)");
        }

        if (analysisCount == 0 && resultCount == 0) {
            validationFeedback.addWarning("No analysis files detected - consider adding search engine outputs");
        }

        if (!hasFasta) {
            validationFeedback.addInfo("Recommended: Add a FASTA database for sequence validation");
        }

        if (hasSdrf) {
            validationFeedback.addInfo("SDRF/experimental design file included - metadata will be auto-populated");
            if (!sdrfValidationPassed && hasAnyInvalidSdrfValidation()) {
                validationFeedback.addError(
                        "SDRF validation failed for one or more files - fix errors and re-validate before proceeding");
            } else if (!sdrfValidationPassed) {
                validationFeedback.addInfo("Click Next to open SDRF validation");
            } else {
                validationFeedback.addInfo("SDRF validation passed - you can proceed to the next step");
            }
        }

        // Success messages
        if (rawCount > 0) {
            validationFeedback.addInfo(rawCount + " RAW file(s) detected");
        }

        // Show validation status based on mandatory files
        if (classificationPanel.hasAllMandatoryFiles()) {
            summaryLabel.setText(total + " files ready for submission");
            summaryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");
            if (!validationFeedback.hasErrors()) {
                validationFeedback.addInfo("All mandatory files present - you can proceed to the next step");
            }
        } else {
            summaryLabel.setText(total + " files added - missing mandatory files");
            summaryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffc107;");
        }
    }

    /** Preloads templates from the validator API in the background. */
    private void preloadPrideValidationRules() {
        Thread.startVirtualThread(() -> {
            try {
                prideFileValidationService.preloadConfig();
            } catch (SubValidationException e) {
                logger.warn("Could not preload PRIDE submission validation rules: {}", e.getMessage());
            }
        });
    }

    /**
     * Runs pride-submissions-commons validation when the user clicks Next (modal progress dialog).
     */
    private boolean runPrideCommonsFileValidation() {
        AtomicBoolean passed = new AtomicBoolean(false);
        AtomicReference<String> failureMessage = new AtomicReference<>();

        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("Validating Files");
        progressStage.setResizable(false);

        VBox progressBox = new VBox(15);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(20));
        progressBox.setStyle("-fx-background-color: white;");

        Label messageLabel = new Label("Checking files against PRIDE submission rules...");
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 13px;");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);

        progressBox.getChildren().addAll(messageLabel, progressIndicator);
        progressStage.setScene(new Scene(progressBox, 360, 150));

        Thread.startVirtualThread(() -> {
            try {
                PrideCommonsFileValidationService.ValidationResult result =
                        prideFileValidationService.validate(new ArrayList<>(model.getFiles()));
                Platform.runLater(() -> {
                    applyPrideValidationResult(result);
                    passed.set(result.valid());
                    if (!result.valid()) {
                        failureMessage.set(result.formattedDetails());
                    }
                    progressStage.close();
                });
            } catch (SubValidationException e) {
                logger.error("PRIDE commons file validation failed", e);
                Platform.runLater(() -> {
                    validationFeedback.clear();
                    validationFeedback.addError("Could not load PRIDE validation rules: " + e.getMessage());
                    validationFeedback.addInfo("Check your network connection and try again.");
                    failureMessage.set(e.getMessage());
                    progressStage.close();
                });
            } catch (Exception e) {
                logger.error("Unexpected error during PRIDE file validation", e);
                Platform.runLater(() -> {
                    validationFeedback.addError("Validation error: " + e.getMessage());
                    failureMessage.set(e.getMessage());
                    progressStage.close();
                });
            }
        });

        progressStage.showAndWait();

        if (!passed.get() && failureMessage.get() != null) {
            showError("File Validation Failed", failureMessage.get());
        }
        return passed.get();
    }

    private void applyPrideValidationResult(PrideCommonsFileValidationService.ValidationResult result) {
        applyPrideValidationToTable(result.fileValidByPath());
        validationFeedback.clear();
        if (result.valid()) {
            for (String warning : result.warnings()) {
                validationFeedback.addWarning(warning);
            }
            validationStatus.setText("");
            validationStatus.setStyle("-fx-text-fill: #666;");
        } else {
            for (String error : result.errors()) {
                validationFeedback.addError(error);
            }
            for (String warning : result.warnings()) {
                validationFeedback.addWarning(warning);
            }
            if (result.errors().isEmpty() && result.summaryMessage() != null) {
                validationFeedback.addError(result.summaryMessage());
            }
            validationStatus.setText("PRIDE validation failed");
            validationStatus.setStyle("-fx-text-fill: #dc3545;");
        }
    }

    private void ensureSdrfTemplatesLoaded() {
        startSdrfTemplatesLoad();
    }

    private CompletableFuture<List<String>> startSdrfTemplatesLoad() {
        if (!sdrfTemplateNames.isEmpty()) {
            return CompletableFuture.completedFuture(List.copyOf(sdrfTemplateNames));
        }
        CompletableFuture<List<String>> inFlight = sdrfTemplatesLoadFuture;
        if (inFlight != null) {
            return inFlight;
        }
        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(this::fetchSdrfTemplatesFromApi);
        sdrfTemplatesLoadFuture = future;
        future.whenComplete((names, error) -> {
            if (error != null) {
                logger.warn("SDRF templates load failed", error);
                sdrfTemplatesLoadFuture = null;
            }
        });
        return future;
    }

    /** Fetches template names from the SDRF validator /templates API (no local filtering). */
    private List<String> fetchSdrfTemplatesFromApi() {
        try {
            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SdrfValidatorApi.TEMPLATES_URL))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.warn("SDRF templates API returned status {}", response.statusCode());
                Platform.runLater(() -> validationFeedback.addWarning(
                        "SDRF: Could not load validator templates (HTTP " + response.statusCode() + ")"));
                return List.of();
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            JsonNode templatesNode = root.path("templates");
            List<String> names = new ArrayList<>();
            if (templatesNode.isArray()) {
                for (JsonNode t : templatesNode) {
                    String name = t.path("name").asText(null);
                    if (name != null && !name.isBlank()) {
                        names.add(name);
                    }
                }
            }
            Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
            sdrfTemplateNames = names;
            logger.info("Loaded {} SDRF validator templates from API", names.size());
            return List.copyOf(names);
        } catch (Exception e) {
            logger.warn("Failed to fetch SDRF validator templates", e);
            Platform.runLater(() ->
                    validationFeedback.addWarning("SDRF: Could not load validator templates - " + e.getMessage()));
            return List.of();
        }
    }

    /**
     * Validate an SDRF file using the PRIDE SDRF Validator REST API.
     * Sends the file as multipart/form-data and displays results as non-blocking feedback.
     * Runs asynchronously to avoid blocking the UI.
     */
    private void validateSdrfFile(File file, SdrfValidationOptions options) {
        validationFeedback.addInfo("SDRF: Validating " + file.getName() + "...");

        Thread.startVirtualThread(() -> {
            try {
                String boundary = "----SdrfBoundary" + System.currentTimeMillis();
                byte[] fileBytes = Files.readAllBytes(file.toPath());

                byte[] body = buildMultipartBody(boundary, file.getName(), fileBytes);
                String validateUri = SdrfValidatorApi.buildValidateUri(options);

                HttpClient client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(validateUri))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> parseSdrfValidationResponse(file.getName(), response));

            } catch (Exception e) {
                logger.warn("SDRF validation API call failed for: {}", file.getName(), e);
                Platform.runLater(() ->
                    validationFeedback.addWarning("SDRF: Could not reach validator service - " + e.getMessage()));
            }
        });
    }

    private ValidationOutcome validateSdrfFileSync(File file, SdrfValidationOptions options) {
        try {
            String boundary = "----SdrfBoundary" + System.currentTimeMillis();
            byte[] fileBytes = Files.readAllBytes(file.toPath());

            byte[] body = buildMultipartBody(boundary, file.getName(), fileBytes);
            String validateUri = SdrfValidatorApi.buildValidateUri(options);

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(validateUri))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return parseSdrfValidationOutcome(file.getName(), response);
        } catch (Exception e) {
            logger.warn("SDRF validation API call failed for: {}", file.getName(), e);
            return new ValidationOutcome(
                    false,
                    List.of(file.getName() + ": Could not reach validator service - " + e.getMessage()),
                    List.of()
            );
        }
    }

    private ValidationOutcome parseSdrfValidationOutcome(String fileName, HttpResponse<String> response) {
        try {
            if (response.statusCode() != 200) {
                return new ValidationOutcome(
                        false,
                        List.of(fileName + ": Validator returned status " + response.statusCode()),
                        List.of()
                );
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            boolean valid = root.path("valid").asBoolean(false);

            List<String> errorMessages = new ArrayList<>();
            List<String> warningMessages = new ArrayList<>();

            JsonNode errors = root.path("errors");
            if (errors.isArray()) {
                for (JsonNode err : errors) {
                    errorMessages.add(fileName + ": " + formatValidationEntry(err));
                }
            }
            JsonNode warnings = root.path("warnings");
            if (warnings.isArray()) {
                for (JsonNode warn : warnings) {
                    warningMessages.add(fileName + ": " + formatValidationEntry(warn));
                }
            }

            return new ValidationOutcome(valid, errorMessages, warningMessages);
        } catch (Exception e) {
            logger.warn("Failed to parse SDRF validation response", e);
            return new ValidationOutcome(
                    false,
                    List.of(fileName + ": Could not parse validation result"),
                    List.of()
            );
        }
    }

    private void showSdrfValidationDialog(List<File> sdrfFiles) {
        List<String> templateNames;
        try {
            templateNames = startSdrfTemplatesLoad().get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.warn("Timed out loading SDRF templates from API");
            templateNames = List.of();
        } catch (Exception e) {
            logger.warn("Failed waiting for SDRF templates", e);
            templateNames = List.of();
        }
        if (templateNames.isEmpty() && !sdrfTemplateNames.isEmpty()) {
            templateNames = List.copyOf(sdrfTemplateNames);
        }
        final List<String> selectableTemplates = templateNames.isEmpty()
                ? List.of("ms-proteomics")
                : templateNames;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("SDRF Validation");
        dialog.setHeaderText("Select SDRF template(s) and run validation");
        ButtonType validateButtonType = new ButtonType("Validate", ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().setAll(validateButtonType, ButtonType.CLOSE);
        dialog.setResizable(true);

        Button validateButton = (Button) dialog.getDialogPane().lookupButton(validateButtonType);
        // Consume APPLY so the dialog does not close; still run validation here (consume runs before setOnAction).

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label fileLabel = new Label("Detected SDRF file(s): " +
                sdrfFiles.stream().map(File::getName).collect(Collectors.joining(", ")));
        fileLabel.setWrapText(true);

        Label templateHint = new Label(
                "Templates are loaded from the PRIDE SDRF validator API. Combine compatible ones "
                        + "(e.g. ms-proteomics + human + dia-acquisition).");
        templateHint.setWrapText(true);
        templateHint.setStyle("-fx-text-fill: #555;");

        Label sdrfWebHint = new Label(
                "For more detail (all templates, options, and full validation output), use the online tool:");
        sdrfWebHint.setWrapText(true);
        sdrfWebHint.setStyle("-fx-text-fill: #555;");

        Hyperlink sdrfValidatorLink = new Hyperlink("PRIDE SDRF Validator");
        sdrfValidatorLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/services/sdrf-validator"));
        sdrfValidatorLink.setTooltip(new Tooltip("https://www.ebi.ac.uk/pride/services/sdrf-validator"));

        final String templatePlaceholder = "Select template";
        MenuButton templateMenu = new MenuButton(templatePlaceholder);
        templateMenu.setMinWidth(280);
        templateMenu.setMaxWidth(Double.MAX_VALUE);
        List<CheckMenuItem> templateCheckItems = new ArrayList<>();
        Label templateSelectionDetail = new Label();
        templateSelectionDetail.setWrapText(true);
        templateSelectionDetail.setStyle("-fx-text-fill: #555;");
        Runnable refreshTemplateSelectionUi = () -> {
            List<String> sel = templateCheckItems.stream()
                    .filter(CheckMenuItem::isSelected)
                    .map(CheckMenuItem::getText)
                    .toList();
            if (sel.isEmpty()) {
                templateMenu.setText(templatePlaceholder);
                templateSelectionDetail.setText("No template selected.");
            } else {
                templateMenu.setText(String.join(", ", sel));
                templateSelectionDetail.setText(sel.size() + " template" + (sel.size() == 1 ? "" : "s") + " selected.");
            }
        };
        MenuItem templateMenuHeader = new MenuItem(templatePlaceholder);
        templateMenuHeader.setDisable(true);
        templateMenu.getItems().add(templateMenuHeader);
        templateMenu.getItems().add(new SeparatorMenuItem());
        for (String name : selectableTemplates) {
            CheckMenuItem item = new CheckMenuItem(name);
            item.selectedProperty().addListener((obs, was, now) -> refreshTemplateSelectionUi.run());
            templateCheckItems.add(item);
            templateMenu.getItems().add(item);
        }
        refreshTemplateSelectionUi.run();

        HBox templateRow = new HBox(8);
        templateRow.setAlignment(Pos.CENTER_LEFT);
        Label templateFieldLabel = new Label("Templates:");
        templateFieldLabel.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(templateMenu, Priority.ALWAYS);
        templateRow.getChildren().addAll(templateFieldLabel, templateMenu);

        Label optionsTitle = new Label("Validation options:");
        optionsTitle.setStyle("-fx-font-weight: bold;");

        CheckBox skipOntologyCheck = new CheckBox("Skip ontology term validation");
        skipOntologyCheck.setSelected(false);
        skipOntologyCheck.setTooltip(new Tooltip(
                "When enabled, ontology terms are not checked (skip_ontology=true). Faster but less strict."));

        CheckBox useOlsCacheOnlyCheck = new CheckBox("Use only OLS cache for ontology validation");
        useOlsCacheOnlyCheck.setSelected(true);
        useOlsCacheOnlyCheck.setTooltip(new Tooltip(
                "When enabled, ontology lookup uses the local OLS cache only (use_ols_cache_only=true). "
                        + "Faster and works offline; disable for live OLS lookups."));

        skipOntologyCheck.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                useOlsCacheOnlyCheck.setDisable(true);
            } else {
                useOlsCacheOnlyCheck.setDisable(false);
            }
        });

        VBox optionsBox = new VBox(6, optionsTitle, skipOntologyCheck, useOlsCacheOnlyCheck);

        Label status = new Label("Choose template(s) and click Validate.");
        status.setStyle("-fx-text-fill: #555;");

        Label resultSummary = new Label("No validation result yet.");
        resultSummary.setWrapText(true);
        resultSummary.setStyle("-fx-text-fill: #555;");

        Label issuesTitle = new Label("Errors and warnings:");
        issuesTitle.setStyle("-fx-font-weight: bold;");

        TextArea issuesArea = new TextArea();
        issuesArea.setEditable(false);
        issuesArea.setWrapText(true);
        issuesArea.setPrefRowCount(12);
        issuesArea.setPromptText("No errors or warnings");
        issuesArea.setStyle("-fx-control-inner-background: #fafafa;");

        Runnable runValidation = () -> {
            List<String> selectedTemplates = templateCheckItems.stream()
                    .filter(CheckMenuItem::isSelected)
                    .map(CheckMenuItem::getText)
                    .toList();
            if (selectedTemplates.isEmpty()) {
                status.setText("Please select at least one template.");
                status.setStyle("-fx-text-fill: #dc3545;");
                return;
            }
            status.setText("Validating SDRF file(s) with: " + String.join(", ", selectedTemplates) + "...");
            status.setStyle("-fx-text-fill: #555;");
            validateButton.setDisable(true);
            issuesArea.clear();
            resultSummary.setText("Validation in progress...");
            resultSummary.setStyle("-fx-text-fill: #555;");

            SdrfValidationOptions validationOptions = new SdrfValidationOptions(
                    List.copyOf(selectedTemplates),
                    skipOntologyCheck.isSelected(),
                    useOlsCacheOnlyCheck.isSelected()
            );
            final List<String> templatesToValidate = selectedTemplates;
            final String signature = buildSdrfPopupSignature(sdrfFiles);
            Thread.startVirtualThread(() -> {
                SdrfBatchResult batchResult = executeSdrfValidation(sdrfFiles, validationOptions);
                final boolean finalAllValid = batchResult.allPassed();
                final Map<String, Boolean> resultsForTable = batchResult.resultsByPath();
                final List<String> allErrors = batchResult.errors();
                final List<String> allWarnings = batchResult.warnings();

                Platform.runLater(() -> {
                    applySdrfValidationResultsToTable(resultsForTable);
                    if (finalAllValid) {
                        markSdrfValidationSucceeded(signature, resultsForTable);
                    }
                    validateButton.setDisable(false);
                    String optionsSummary = validationOptions.skipOntology()
                            ? ", skip ontology"
                            : (validationOptions.useOlsCacheOnly() ? ", OLS cache only" : ", live OLS");
                    status.setText("Validation completed (" + templatesToValidate.size() + " template"
                            + (templatesToValidate.size() == 1 ? "" : "s") + ": "
                            + String.join(", ", templatesToValidate) + optionsSummary + ").");
                    status.setStyle("-fx-text-fill: #555;");

                    issuesArea.setText(formatValidationIssues(allErrors, allWarnings));

                    if (finalAllValid && allErrors.isEmpty() && allWarnings.isEmpty()) {
                        resultSummary.setText("SDRF validation passed. No errors or warnings.");
                        resultSummary.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else if (allErrors.isEmpty()) {
                        resultSummary.setText("SDRF validation passed with warnings.");
                        resultSummary.setStyle("-fx-text-fill: #856404; -fx-font-weight: bold;");
                    } else {
                        resultSummary.setText("SDRF validation failed. Please review issues below.");
                        resultSummary.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    }
                    updateSummary();
                });
            });
        };

        validateButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            runValidation.run();
        });

        content.getChildren().addAll(
                fileLabel,
                templateHint,
                sdrfWebHint,
                sdrfValidatorLink,
                templateRow,
                templateSelectionDetail,
                optionsBox,
                status,
                resultSummary,
                issuesTitle,
                issuesArea
        );
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(560);
        dialog.showAndWait();
    }

    private String formatValidationIssues(List<String> errors, List<String> warnings) {
        StringBuilder text = new StringBuilder();
        if (errors != null && !errors.isEmpty()) {
            text.append("Errors:\n").append(joinLines(errors));
        }
        if (warnings != null && !warnings.isEmpty()) {
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            text.append("Warnings:\n").append(joinLines(warnings));
        }
        return text.toString();
    }

    private String joinLines(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (String message : messages) {
            joiner.add(message);
        }
        return joiner.toString();
    }

    private String formatValidationEntry(JsonNode node) {
        String message = node.path("message").asText("");
        if (message.isBlank()) {
            message = node.path("msg").asText("Unknown validation issue");
        }

        JsonNode rowNode = node.path("row");
        if (rowNode.isInt()) {
            int row = rowNode.asInt(-1);
            if (row >= 0) {
                return message + " (row " + row + ")";
            }
        }
        return message;
    }

    private static class ValidationOutcome {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        private ValidationOutcome(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }
    }

    private record SdrfBatchResult(
            boolean allPassed,
            Map<String, Boolean> resultsByPath,
            List<String> errors,
            List<String> warnings
    ) {
    }

    private List<File> getSdrfFiles() {
        List<File> sdrfFiles = new ArrayList<>();
        for (DataFile df : model.getFiles()) {
            File file = df.getFile();
            if (file == null || !file.isFile()) {
                continue;
            }
            if (df.getFileType() == ProjectFileType.EXPERIMENTAL_DESIGN || SdrfParserService.isSdrfFile(file)) {
                sdrfFiles.add(file);
            }
        }
        return sdrfFiles;
    }

    private String buildSdrfPopupSignature(List<File> sdrfFiles) {
        List<String> ids = sdrfFiles.stream()
                .map(f -> f.getAbsolutePath() + ":" + f.lastModified() + ":" + f.length())
                .sorted()
                .toList();
        return String.join("|", ids);
    }

    private void openSdrfValidationDialog() {
        List<File> sdrfFiles = getSdrfFiles();
        if (!sdrfFiles.isEmpty()) {
            showSdrfValidationDialog(sdrfFiles);
        }
    }

    /**
     * Drops validation status for removed files only; keeps status for files still in the list.
     */
    private void syncValidationStatusWithCurrentFiles() {
        Set<String> currentPaths = new HashSet<>();
        for (DataFile dataFile : model.getFiles()) {
            if (dataFile.getFile() != null) {
                currentPaths.add(dataFile.getFile().getAbsolutePath());
            }
        }
        prideValidationByPath.keySet().removeIf(path -> !currentPaths.contains(path));
        sdrfValidationByPath.keySet().removeIf(path -> !currentPaths.contains(path));
        ensureSdrfSignatureSynced();
        fileTable.refreshValidationStatus();
    }

    /**
     * Clears saved SDRF validation status when the SDRF file set changes (new upload, remove, or edit).
     */
    private void ensureSdrfSignatureSynced() {
        List<File> sdrfFiles = getSdrfFiles();
        String newSignature = sdrfFiles.isEmpty() ? null : buildSdrfPopupSignature(sdrfFiles);
        if (Objects.equals(newSignature, sdrfPopupSignature)) {
            return;
        }
        sdrfPopupSignature = newSignature;
        sdrfValidatedSignature = null;
        sdrfValidationByPath.clear();
    }

    private void applyPrideValidationToTable(Map<String, Boolean> resultsByPath) {
        prideValidationByPath.clear();
        if (resultsByPath != null) {
            prideValidationByPath.putAll(resultsByPath);
        }
        fileTable.refreshValidationStatus();
    }

    private Boolean lookupPrideValidationForTable(DataFile dataFile) {
        if (dataFile == null || dataFile.getFile() == null) {
            return null;
        }
        return prideValidationByPath.get(dataFile.getFile().getAbsolutePath());
    }

    private SdrfBatchResult executeSdrfValidation(List<File> sdrfFiles, SdrfValidationOptions options) {
        Map<String, Boolean> resultsByPath = new HashMap<>();
        List<String> allErrors = new ArrayList<>();
        List<String> allWarnings = new ArrayList<>();
        boolean allPassed = true;

        for (File file : sdrfFiles) {
            ValidationOutcome outcome = validateSdrfFileSync(file, options);
            boolean filePassed = outcome.valid && outcome.errors.isEmpty();
            resultsByPath.put(file.getAbsolutePath(), filePassed);
            if (!filePassed) {
                allPassed = false;
            }
            allErrors.addAll(outcome.errors);
            allWarnings.addAll(outcome.warnings);
        }
        return new SdrfBatchResult(allPassed, Map.copyOf(resultsByPath), allErrors, allWarnings);
    }

    private void markSdrfValidationSucceeded(String signature, Map<String, Boolean> resultsByPath) {
        sdrfPopupSignature = signature;
        sdrfValidatedSignature = signature;
        applySdrfValidationResultsToTable(resultsByPath);
    }

    /** All SDRF files must have passed validation (or there are no SDRF files). */
    private boolean allSdrfFilesPassedValidation() {
        List<File> sdrfFiles = getSdrfFiles();
        if (sdrfFiles.isEmpty()) {
            return true;
        }
        for (File file : sdrfFiles) {
            if (!Boolean.TRUE.equals(sdrfValidationByPath.get(file.getAbsolutePath()))) {
                return false;
            }
        }
        return true;
    }

    /** True when at least one SDRF file failed validation (red status in the table). */
    private boolean hasAnyInvalidSdrfValidation() {
        for (File file : getSdrfFiles()) {
            if (Boolean.FALSE.equals(sdrfValidationByPath.get(file.getAbsolutePath()))) {
                return true;
            }
        }
        return false;
    }

    private Boolean lookupSdrfValidationForTable(DataFile dataFile) {
        if (dataFile == null || dataFile.getFile() == null) {
            return null;
        }
        if (dataFile.getFileType() != ProjectFileType.EXPERIMENTAL_DESIGN
                && !SdrfParserService.isSdrfFile(dataFile.getFile())) {
            return null;
        }
        return sdrfValidationByPath.get(dataFile.getFile().getAbsolutePath());
    }

    private void applySdrfValidationResultsToTable(Map<String, Boolean> resultsByPath) {
        sdrfValidationByPath.clear();
        sdrfValidationByPath.putAll(resultsByPath);
        fileTable.refreshValidationStatus();
    }

    private byte[] buildMultipartBody(String boundary, String fileName, byte[] fileBytes) {
        String prefix = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: text/tab-separated-values\r\n\r\n";
        String suffix = "\r\n--" + boundary + "--\r\n";

        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[prefixBytes.length + fileBytes.length + suffixBytes.length];
        System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
        System.arraycopy(fileBytes, 0, body, prefixBytes.length, fileBytes.length);
        System.arraycopy(suffixBytes, 0, body, prefixBytes.length + fileBytes.length, suffixBytes.length);
        return body;
    }

    private void parseSdrfValidationResponse(String fileName, HttpResponse<String> response) {
        try {
            if (response.statusCode() != 200) {
                validationFeedback.addWarning("SDRF: Validator returned status " + response.statusCode() + " for " + fileName);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            boolean valid = root.path("valid").asBoolean(false);
            int errorCount = root.path("error_count").asInt(0);
            int warningCount = root.path("warning_count").asInt(0);

            // Collect errors and warnings for popup
            List<String> errorMessages = new ArrayList<>();
            List<String> warningMessages = new ArrayList<>();

            JsonNode errors = root.path("errors");
            if (errors.isArray()) {
                for (JsonNode err : errors) {
                    errorMessages.add(formatValidationEntry(err));
                }
            }
            JsonNode warnings = root.path("warnings");
            if (warnings.isArray()) {
                for (JsonNode warn : warnings) {
                    warningMessages.add(formatValidationEntry(warn));
                }
            }

            // Update inline feedback
            if (valid) {
                validationFeedback.addInfo("SDRF validated: " + fileName);
            } else {
                validationFeedback.addWarning("SDRF validation failed: " + fileName + " (" + errorCount + " error(s))");
            }

        } catch (Exception e) {
            logger.warn("Failed to parse SDRF validation response", e);
            validationFeedback.addWarning("SDRF: Could not parse validation result for " + fileName);
        }
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
