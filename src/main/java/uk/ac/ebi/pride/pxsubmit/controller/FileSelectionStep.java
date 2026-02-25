package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import uk.ac.ebi.pride.pxsubmit.service.ServiceFactory;
import uk.ac.ebi.pride.pxsubmit.service.ValidationService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public FileSelectionStep(SubmissionModel model) {
        super("file-selection",
              "Select Files",
              "Add the files you want to include in your submission",
              model);
    }

    @Override
    public boolean canSkip() {
        // Skip this step during resubmission - files are managed in FileResubmissionStep
        return model.isResubmissionMode();
    }

    @Override
    protected Parent createContent() {
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

                    if (dataFile.getFileType() == ProjectFileType.EXPERIMENTAL_DESIGN) {
                        validateSdrfFile(file);
                    }
                }
            }
        }

        if (addedCount > 0) {
            logger.info("Added {} file(s) to submission", addedCount);
        }

        updateSummary();
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

        ValidationService validationService = ServiceFactory.getInstance().createValidationService(
            model.getFiles(), model.getSubmissionType());

        validationProgress.setVisible(true);
        validationProgress.progressProperty().bind(validationService.progressProperty());
        validationStatus.textProperty().bind(validationService.messageProperty());

        validationService.setOnSucceeded(e -> {
            ValidationService.ValidationResult result = validationService.getValue();
            Platform.runLater(() -> {
                validationProgress.setVisible(false);
                validationProgress.progressProperty().unbind();
                validationStatus.textProperty().unbind();

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

        int total = model.getFiles().size();
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

        boolean hasSdrf = model.getFiles().stream()
            .anyMatch(f -> f.getFileType() == ProjectFileType.EXPERIMENTAL_DESIGN);
        if (hasSdrf) {
            validationFeedback.addInfo("SDRF/experimental design file included - metadata will be auto-populated");
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

    private static final String SDRF_VALIDATOR_URL =
            "https://www.ebi.ac.uk/pride/services/sdrf-validator/validate";

    /**
     * Validate an SDRF file using the PRIDE SDRF Validator REST API.
     * Sends the file as multipart/form-data and displays results as non-blocking feedback.
     * Runs asynchronously to avoid blocking the UI.
     */
    private void validateSdrfFile(File file) {
        validationFeedback.addInfo("SDRF: Validating " + file.getName() + "...");

        Thread.startVirtualThread(() -> {
            try {
                String boundary = "----SdrfBoundary" + System.currentTimeMillis();
                byte[] fileBytes = Files.readAllBytes(file.toPath());

                // Build multipart body
                byte[] body = buildMultipartBody(boundary, file.getName(), fileBytes);

                HttpClient client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SDRF_VALIDATOR_URL + "?template=default&skip_ontology=false&use_ols_cache_only=true"))
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
                    errorMessages.add(err.path("msg").asText());
                }
            }
            JsonNode warnings = root.path("warnings");
            if (warnings.isArray()) {
                for (JsonNode warn : warnings) {
                    warningMessages.add(warn.path("msg").asText());
                }
            }

            // Update inline feedback
            if (valid) {
                validationFeedback.addInfo("SDRF validated: " + fileName);
            } else {
                validationFeedback.addWarning("SDRF validation failed: " + fileName + " (" + errorCount + " error(s))");
            }

            // Show popup dialog with full results
            showSdrfValidationPopup(fileName, valid, errorMessages, warningMessages);

        } catch (Exception e) {
            logger.warn("Failed to parse SDRF validation response", e);
            validationFeedback.addWarning("SDRF: Could not parse validation result for " + fileName);
        }
    }

    private void showSdrfValidationPopup(String fileName, boolean valid,
                                          List<String> errors, List<String> warnings) {
        Alert alert = new Alert(valid ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle("SDRF Validation Result");
        alert.setHeaderText(valid ? "Validation Passed" : "Validation Failed");
        alert.setResizable(true);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setPrefWidth(500);

        // File name
        Label fileLabel = new Label("File: " + fileName);
        fileLabel.setStyle("-fx-font-weight: bold;");
        content.getChildren().add(fileLabel);

        // Status banner
        Label statusLabel = new Label(valid ? "\u2714 SDRF is valid" : "\u2718 SDRF has errors");
        statusLabel.setStyle(valid
            ? "-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-padding: 8 12; -fx-background-radius: 4; -fx-font-weight: bold;"
            : "-fx-background-color: #f8d7da; -fx-text-fill: #721c24; -fx-padding: 8 12; -fx-background-radius: 4; -fx-font-weight: bold;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(statusLabel);

        // Errors
        if (!errors.isEmpty()) {
            Label errTitle = new Label("Errors (" + errors.size() + "):");
            errTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545;");
            content.getChildren().add(errTitle);

            TextArea errArea = new TextArea();
            errArea.setEditable(false);
            errArea.setWrapText(true);
            errArea.setPrefRowCount(Math.min(errors.size(), 8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < errors.size(); i++) {
                sb.append(i + 1).append(". ").append(errors.get(i)).append("\n");
            }
            errArea.setText(sb.toString());
            errArea.setStyle("-fx-control-inner-background: #fff5f5;");
            content.getChildren().add(errArea);
        }

        // Warnings
        if (!warnings.isEmpty()) {
            Label warnTitle = new Label("Warnings (" + warnings.size() + "):");
            warnTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #856404;");
            content.getChildren().add(warnTitle);

            TextArea warnArea = new TextArea();
            warnArea.setEditable(false);
            warnArea.setWrapText(true);
            warnArea.setPrefRowCount(Math.min(warnings.size(), 6));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < warnings.size(); i++) {
                sb.append(i + 1).append(". ").append(warnings.get(i)).append("\n");
            }
            warnArea.setText(sb.toString());
            warnArea.setStyle("-fx-control-inner-background: #fffbf0;");
            content.getChildren().add(warnArea);
        }

        // No issues
        if (errors.isEmpty() && warnings.isEmpty()) {
            Label noIssues = new Label("No errors or warnings found.");
            noIssues.setStyle("-fx-text-fill: #28a745;");
            content.getChildren().add(noIssues);
        }

        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
