package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
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
import uk.ac.ebi.pride.pxsubmit.service.SdrfParserService;
import uk.ac.ebi.pride.pxsubmit.service.SdrfValidationService;
import uk.ac.ebi.pride.pxsubmit.service.ServiceFactory;
import uk.ac.ebi.pride.pxsubmit.service.WebSubIncomingFileValidationService;
import uk.ac.ebi.pride.pxsubmit.util.FileNameValidator;
import uk.ac.ebi.pride.pxsubmit.view.component.FileClassificationPanel;
import uk.ac.ebi.pride.pxsubmit.view.component.FileTableView;
import uk.ac.ebi.pride.pxsubmit.view.component.ValidationFeedback;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Step for selecting and categorizing submission files.
 * Features drag-drop support and API-backed file type validation.
 */
public class FileSelectionStep extends AbstractWizardStep {

    private static final int MAX_FOLDER_FILE_COUNT = 1000;
    private static final int MAX_INVALID_FILE_NAMES_TO_SHOW = 10;

    private FileTableView fileTable;
    private FileClassificationPanel classificationPanel;
    private ValidationFeedback validationFeedback;
    private Label summaryLabel;
    private Button addFilesButton;
    private Button addFolderButton;
    private Button removeSelectedButton;
    private ProgressBar validationProgress;
    private Label validationStatus;
    private final BooleanProperty validationActionVisible = new SimpleBooleanProperty(false);
    private final BooleanProperty validationActionEnabled = new SimpleBooleanProperty(false);

    /** Absolute file path → incoming-file validation passed (table status column). */
    private final Map<String, Boolean> prideValidationByPath = new HashMap<>();
    /** Absolute file path → file type/category returned by incoming-file validation. */
    private final Map<String, String> responseFileTypeByPath = new HashMap<>();
    private boolean incomingFileValidationPassed;

    private final WebSubIncomingFileValidationService webSubIncomingFileValidationService =
            ServiceFactory.getInstance().createWebSubIncomingFileValidationService();
    private final SdrfValidationService sdrfValidationService =
            ServiceFactory.getInstance().createSdrfValidationService();

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
            "Click Validate Files to check them and apply the file types returned by the service.");
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
        classificationPanel.setShowWarnings(false);
        classificationPanel.setVisible(false);
        classificationPanel.setManaged(false);
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
        fileTable.setChecksumLookup(df -> model.getChecksum(df));
        fileTable.setPrideValidationLookup(this::lookupPrideValidationForTable);
        fileTable.setSdrfValidationLookup(this::lookupSdrfValidationForTable);
        fileTable.setResponseFileTypeLookup(this::lookupResponseFileTypeForTable);

        // Enable remove button when selection changes
        fileTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateFileActionButtons();
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
            syncValidationStatusWithCurrentFiles();
            updateFileActionButtons();
            if (wizardController != null) {
                wizardController.refreshStepIndicator();
            }
        });

        ensureSdrfTemplatesLoaded();
        updateSummary();
        updateFileActionButtons();
    }

    @Override
    protected void onStepEntering() {
        ensureSdrfTemplatesLoaded();
        updateSummary();
    }

    @Override
    public boolean validate() {
        List<File> invalidNamedFiles = getFilesWithInvalidSubmissionNames();
        if (!invalidNamedFiles.isEmpty()) {
            updateSummary();
            showInvalidFileNamesWarning("Invalid File Names", invalidNamedFiles);
            return false;
        }

        if (model.getFiles().isEmpty()) {
            return false;
        }

        if (!hasCurrentSuccessfulIncomingFileValidation()) {
            validationFeedback.clear();
            validationFeedback.addInfo("Validate files before continuing.");
            return false;
        }

        updateSummary();
        return true;
    }

    public BooleanProperty validationActionEnabledProperty() {
        return validationActionEnabled;
    }

    public BooleanProperty validationActionVisibleProperty() {
        return validationActionVisible;
    }

    public boolean validateUploadedFiles() {
        List<File> invalidNamedFiles = getFilesWithInvalidSubmissionNames();
        if (!invalidNamedFiles.isEmpty()) {
            updateSummary();
            showInvalidFileNamesWarning("Invalid File Names", invalidNamedFiles);
            return false;
        }

        if (model.getFiles().isEmpty()) {
            validationFeedback.clear();
            validationFeedback.addError("Add files before validation.");
            return false;
        }

        if (!requiresIncomingFileValidation()) {
            updateSummary();
            return true;
        }

        return runWebSubIncomingFileValidation();
    }

    /** Refreshes file table SDRF status icons after validation on the SDRF step. */
    public void refreshSdrfValidationDisplay() {
        if (fileTable != null) {
            fileTable.refreshValidationStatus();
        }
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
     * Open folder chooser to add files from the selected folder (top level only, not subfolders).
     */
    private void addFolder() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Folder to Add");

        File directory = dirChooser.showDialog(null);
        if (directory != null && directory.isDirectory()) {
            scanFolder(directory);
        }
    }

    /**
     * Scan files in the selected folder (not subfolders), show progress with cancel, then confirm.
     */
    private void scanFolder(File directory) {
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("Scanning Folder");
        progressStage.setResizable(false);

        VBox progressBox = new VBox(12);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(20));
        progressBox.setStyle("-fx-background-color: white;");

        Label messageLabel = new Label("Scanning folder...");
        messageLabel.setStyle("-fx-font-size: 14px;");

        Label folderLabel = new Label(directory.getAbsolutePath());
        folderLabel.setWrapText(true);
        folderLabel.setMaxWidth(420);
        folderLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Label statusLabel = new Label("Starting scan...");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(420);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setPrefWidth(360);

        Button cancelButton = new Button("Cancel");
        cancelButton.setDefaultButton(true);

        HBox buttonBox = new HBox(cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPrefWidth(360);

        progressBox.getChildren().addAll(
            messageLabel, folderLabel, statusLabel, progressBar, buttonBox
        );

        Scene scene = new Scene(progressBox, 460, 200);
        progressStage.setScene(scene);

        setFolderScanControlsDisabled(true);

        Task<List<File>> scanTask = new Task<>() {
            @Override
            protected List<File> call() throws Exception {
                List<File> foundFiles = new ArrayList<>();
                try (Stream<Path> paths = Files.list(directory.toPath())) {
                    for (Path path : (Iterable<Path>) paths::iterator) {
                        if (isCancelled()) {
                            break;
                        }
                        if (Files.isRegularFile(path) && !isHiddenOrSystemFile(path)) {
                            foundFiles.add(path.toFile());
                            int count = foundFiles.size();
                            if (count == 1 || count % 25 == 0) {
                                updateMessage("Found " + count + " file(s)...");
                            }
                            if (count > MAX_FOLDER_FILE_COUNT) {
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    if (!isCancelled()) {
                        throw e;
                    }
                }
                return foundFiles;
            }
        };

        statusLabel.textProperty().bind(scanTask.messageProperty());

        Runnable finishScan = () -> {
            statusLabel.textProperty().unbind();
            setFolderScanControlsDisabled(false);
        };

        cancelButton.setOnAction(e -> {
            scanTask.cancel();
            cancelButton.setDisable(true);
            statusLabel.textProperty().unbind();
            statusLabel.setText("Cancelling scan...");
        });

        progressStage.setOnCloseRequest(e -> {
            scanTask.cancel();
            cancelButton.setDisable(true);
        });

        scanTask.setOnSucceeded(e -> {
            finishScan.run();
            progressStage.close();
            List<File> foundFiles = scanTask.getValue();
            if (foundFiles != null && foundFiles.size() > MAX_FOLDER_FILE_COUNT) {
                showWarning("Too Many Files",
                    "This folder contains more than " + MAX_FOLDER_FILE_COUNT + " files.\n\n" +
                    "You cannot select more than " + MAX_FOLDER_FILE_COUNT + " files from a folder at once. " +
                    "Subfolders are not included — only files directly in the selected folder are counted.\n\n" +
                    "Please choose a smaller folder or add files individually.");
                return;
            }
            confirmAndAddFilesFromFolder(directory, foundFiles);
        });

        scanTask.setOnCancelled(e -> {
            finishScan.run();
            progressStage.close();
            logger.info("Folder scan cancelled by user: {}", directory.getAbsolutePath());
        });

        scanTask.setOnFailed(e -> {
            finishScan.run();
            progressStage.close();
            if (!scanTask.isCancelled()) {
                Throwable ex = scanTask.getException();
                logger.error("Error scanning folder: {}", directory.getAbsolutePath(), ex);
                showError("Folder Scan Error",
                    "Failed to scan folder: " + (ex != null ? ex.getMessage() : "Unknown error"));
            }
        });

        Thread scanThread = new Thread(scanTask, "Folder-Scanner");
        scanThread.setDaemon(true);
        scanThread.start();
        progressStage.show();
    }

    /**
     * Ask the user to confirm adding scanned files (so a wrong folder can be rejected).
     */
    private void confirmAndAddFilesFromFolder(File directory, List<File> foundFiles) {
        if (foundFiles == null || foundFiles.isEmpty()) {
            showInfo("No Files Found",
                "No files were found in the selected folder:\n" + directory.getAbsolutePath());
            return;
        }

        List<File> invalidNamedFiles = FileNameValidator.findInvalidSubmissionFileNames(foundFiles);
        List<File> validNamedFiles = foundFiles.stream()
            .filter(FileNameValidator::isValidSubmissionFileName)
            .collect(Collectors.toList());

        if (validNamedFiles.isEmpty()) {
            showInvalidFileNamesWarning("Invalid File Names", invalidNamedFiles);
            return;
        }

        if (!invalidNamedFiles.isEmpty()) {
            showInvalidFileNamesWarning("Some Files Were Skipped", invalidNamedFiles);
        }

        List<File> newFiles = validNamedFiles.stream()
            .filter(file -> model.getFiles().stream()
                .noneMatch(df -> df.getFile() != null &&
                    df.getFile().getAbsolutePath().equals(file.getAbsolutePath())))
            .collect(Collectors.toList());

        int duplicateCount = validNamedFiles.size() - newFiles.size();

        StringBuilder content = new StringBuilder();
        content.append("Folder:\n").append(directory.getAbsolutePath()).append("\n\n");
        content.append("Found ").append(validNamedFiles.size()).append(" valid file(s)");
        if (duplicateCount > 0) {
            content.append(" (").append(duplicateCount).append(" already in your submission)");
        }
        if (!invalidNamedFiles.isEmpty()) {
            content.append(".\nSkipped ").append(invalidNamedFiles.size())
                .append(" file(s) with invalid names");
        }
        content.append(".\n\nOnly files directly in this folder are included (not subfolders).\n\nAdd ");
        content.append(newFiles.isEmpty() ? "these files" : newFiles.size() + " new file(s)");
        content.append(" to your submission?");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Add Files from Folder");
        alert.setHeaderText("Review folder contents");
        alert.setContentText(content.toString());
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("Add Files");
        ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");

        Optional<ButtonType> response = alert.showAndWait();
        if (response.isEmpty() || response.get() != ButtonType.OK) {
            logger.info("User declined to add files from folder: {}", directory.getAbsolutePath());
            return;
        }

        if (newFiles.isEmpty()) {
            showInfo("No New Files",
                "All files from this folder are already in your submission.");
            return;
        }

        addFilesToModel(newFiles);
        logger.info("Added {} files from folder {}", newFiles.size(), directory.getName());
    }

    private void setFolderScanControlsDisabled(boolean disabled) {
        if (addFilesButton != null) {
            addFilesButton.setDisable(disabled);
        }
        if (addFolderButton != null) {
            addFolderButton.setDisable(disabled);
        }
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
                scanFolder(directories.get(0));
            } else {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Add Folders");
                alert.setHeaderText("Add files from " + directories.size() + " folder(s)?");
                alert.setContentText(
                    "This will add files directly in each folder (not from subfolders). " +
                    "Each folder is limited to " + MAX_FOLDER_FILE_COUNT + " files.");

                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        for (File dir : directories) {
                            scanFolder(dir);
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
        List<File> validNamedFiles = filterFilesWithValidSubmissionNames(files);
        int addedCount = 0;
        for (File file : validNamedFiles) {
            if (file.isFile()) {
                // Check if file already exists
                boolean exists = model.getFiles().stream()
                    .anyMatch(df -> df.getFile() != null &&
                                   df.getFile().getAbsolutePath().equals(file.getAbsolutePath()));

                if (!exists) {
                    DataFile dataFile = new DataFile();
                    dataFile.setFile(file);
                    if (SdrfParserService.isSdrfFile(file)) {
                        dataFile.setFileType(ProjectFileType.EXPERIMENTAL_DESIGN);
                    }
                    model.addFile(dataFile);
                    logger.debug("Added file: {} (type: {})", file.getName(), dataFile.getFileType());
                    addedCount++;
                }
            }
        }

        if (addedCount > 0) {
            logger.info("Added {} file(s) to submission", addedCount);
            syncValidationStatusWithCurrentFiles();
        }

        updateSummary();
        updateFileActionButtons();
    }

    /**
     * Keep files whose names can be uploaded safely and warn for skipped files.
     */
    private List<File> filterFilesWithValidSubmissionNames(List<File> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<File> invalidNamedFiles = FileNameValidator.findInvalidSubmissionFileNames(files);
        if (!invalidNamedFiles.isEmpty()) {
            showInvalidFileNamesWarning("Some Files Were Skipped", invalidNamedFiles);
        }

        return files.stream()
            .filter(FileNameValidator::isValidSubmissionFileName)
            .collect(Collectors.toList());
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
        syncValidationStatusWithCurrentFiles();
        updateSummary();
        updateFileActionButtons();
    }

    /**
     * Remove a specific file
     */
    private void removeFile(DataFile file) {
        model.removeFile(file);
        syncValidationStatusWithCurrentFiles();
        updateSummary();
        updateFileActionButtons();
    }

    /**
     * Update the summary label, classification panel, and validation feedback
     */
    private void updateSummary() {
        refreshFileClassificationVisibility();

        validationFeedback.clear();

        List<File> invalidNamedFiles = getFilesWithInvalidSubmissionNames();
        boolean hasInvalidFileNames = !invalidNamedFiles.isEmpty();
        int total = model.getFiles().size();
        boolean requiresIncomingValidation = requiresIncomingFileValidation();
        boolean hasValidatedCurrentFiles = hasCurrentSuccessfulIncomingFileValidation();

        if (total == 0) {
            summaryLabel.setText("No files added - drag and drop files or use the buttons above");
            summaryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
            validationFeedback.addInfo("Add files to continue");
            refreshFileStepState();
            return;
        }

        if (hasInvalidFileNames) {
            summaryLabel.setText(total + " files added - invalid file names");
            summaryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545;");
            validationFeedback.addError(createInvalidFileNameSummary(invalidNamedFiles));
            refreshFileStepState();
            return;
        }

        if (!requiresIncomingValidation) {
            summaryLabel.setText(total + " SDRF file(s) added");
            summaryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");
            validationFeedback.addInfo("SDRF files do not need incoming file validation - you can proceed");
        } else if (hasValidatedCurrentFiles) {
            summaryLabel.setText(total + " files validated");
            summaryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");
            validationFeedback.addInfo("File validation passed - you can proceed to the next step");
        } else {
            summaryLabel.setText(total + " files added - validation required");
            summaryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
            validationFeedback.addInfo("Click Validate Files before continuing");
        }

        refreshFileStepState();
    }

    /**
     * Runs web-submissions incoming-file validation from the footer Validate Files button.
     */
    private boolean runWebSubIncomingFileValidation() {
        List<DataFile> filesToValidate = getFilesRequiringIncomingValidation();
        if (filesToValidate.isEmpty()) {
            updateSummary();
            return true;
        }

        if (model.isTrainingMode()) {
            applyPrideValidationToTable(buildFilesValidMap(filesToValidate), buildCurrentFileTypeMap(filesToValidate), true);
            return true;
        }

        String username = model.getUserName();
        String password = model.getPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            String message = "Missing login credentials. Please log in again before validating files.";
            validationFeedback.clear();
            validationFeedback.addError(message);
            showError("File Validation Failed", message);
            return false;
        }

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

        Label messageLabel = new Label("Checking files with the web submissions validation service...");
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 13px;");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);

        progressBox.getChildren().addAll(messageLabel, progressIndicator);
        progressStage.setScene(new Scene(progressBox, 360, 150));

        Thread.startVirtualThread(() -> {
            try {
                Platform.runLater(() ->
                    messageLabel.setText("Authenticating with the web submissions validation service..."));
                String token = webSubIncomingFileValidationService.authenticate(username, password);
                Platform.runLater(() ->
                    messageLabel.setText("Checking files with the web submissions validation service..."));
                WebSubIncomingFileValidationService.ValidationResult result =
                        webSubIncomingFileValidationService.validate(new ArrayList<>(filesToValidate), token);
                Platform.runLater(() -> {
                    applyWebSubIncomingFileValidationResult(result);
                    passed.set(result.valid());
                    if (!result.valid()) {
                        failureMessage.set(result.formattedDetails());
                    }
                    progressStage.close();
                });
            } catch (WebSubIncomingFileValidationService.ValidationException e) {
                logger.error("Web submissions file validation failed", e);
                Platform.runLater(() -> {
                    validationFeedback.clear();
                    validationFeedback.addError("File validation failed: " + e.getMessage());
                    validationFeedback.addInfo("Check your network connection and try again.");
                    failureMessage.set(e.getMessage());
                    progressStage.close();
                });
            } catch (Exception e) {
                logger.error("Unexpected error during web submissions file validation", e);
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

    private void applyWebSubIncomingFileValidationResult(WebSubIncomingFileValidationService.ValidationResult result) {
        applyPrideValidationToTable(result.fileValidByPath(), result.fileTypeByPath(), result.valid());
        validationFeedback.clear();
        if (result.valid()) {
            if (result.summaryMessage() != null && !result.summaryMessage().isBlank()) {
                validationFeedback.addInfo(result.summaryMessage());
            } else {
                validationFeedback.addInfo("File validation passed.");
            }
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
            validationStatus.setText("");
            validationStatus.setStyle("-fx-text-fill: #666;");
        }
    }

    private Map<String, Boolean> buildFilesValidMap(List<DataFile> files) {
        Map<String, Boolean> results = new HashMap<>();
        for (DataFile file : files) {
            if (file.getFile() != null) {
                results.put(file.getFile().getAbsolutePath(), true);
            }
        }
        return results;
    }

    private Map<String, String> buildCurrentFileTypeMap(List<DataFile> files) {
        Map<String, String> results = new HashMap<>();
        for (DataFile file : files) {
            if (file.getFile() != null && file.getFileType() != null) {
                results.put(file.getFile().getAbsolutePath(), file.getFileType().name());
            }
        }
        return results;
    }

    private void ensureSdrfTemplatesLoaded() {
        sdrfValidationService.loadTemplatesAsync();
    }

    private List<File> getSdrfFiles() {
        return sdrfValidationService.findSdrfFiles(model.getFiles());
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
        responseFileTypeByPath.keySet().removeIf(path -> !currentPaths.contains(path));
        model.getSdrfValidation().syncFileSet(getSdrfFiles());
        fileTable.refreshValidationStatus();
    }

    private void applyPrideValidationToTable(Map<String, Boolean> resultsByPath, Map<String, String> responseFileTypesByPath,
                                             boolean validationPassed) {
        prideValidationByPath.clear();
        incomingFileValidationPassed = validationPassed;
        if (resultsByPath != null) {
            prideValidationByPath.putAll(resultsByPath);
        }
        responseFileTypeByPath.clear();
        if (responseFileTypesByPath != null) {
            responseFileTypeByPath.putAll(responseFileTypesByPath);
            applyResponseFileTypesToModel(responseFileTypesByPath);
        }
        refreshFileClassificationVisibility();
        refreshFileStepState();
        fileTable.refreshValidationStatus();
    }

    private void refreshFileClassificationVisibility() {
        List<DataFile> categorizedFiles = model.getFiles().stream()
            .filter(dataFile -> dataFile.getFile() != null
                && dataFile.getFileType() != null
                && responseFileTypeByPath.containsKey(dataFile.getFile().getAbsolutePath()))
            .collect(Collectors.toList());
        boolean showClassification = !categorizedFiles.isEmpty();
        classificationPanel.setVisible(showClassification);
        classificationPanel.setManaged(showClassification);
        if (showClassification) {
            classificationPanel.setFiles(categorizedFiles);
        } else {
            classificationPanel.setFiles(List.of());
            fileTable.filterByType(null);
        }
    }

    private void refreshFileStepState() {
        boolean hasFiles = !model.getFiles().isEmpty();
        boolean hasInvalidFileNames = !getFilesWithInvalidSubmissionNames().isEmpty();
        boolean requiresIncomingValidation = hasFiles && requiresIncomingFileValidation();
        validationActionVisible.set(requiresIncomingValidation);
        validationActionEnabled.set(requiresIncomingValidation && !hasInvalidFileNames);
        setValid(hasFiles && !hasInvalidFileNames && hasCurrentSuccessfulIncomingFileValidation());
    }

    private void applyResponseFileTypesToModel(Map<String, String> responseFileTypesByPath) {
        for (DataFile dataFile : model.getFiles()) {
            if (dataFile.getFile() == null) {
                continue;
            }
            String responseFileType = responseFileTypesByPath.get(dataFile.getFile().getAbsolutePath());
            ProjectFileType projectFileType = toProjectFileType(responseFileType);
            if (projectFileType != null) {
                dataFile.setFileType(projectFileType);
            }
        }
    }

    private ProjectFileType toProjectFileType(String responseFileType) {
        if (responseFileType == null || responseFileType.isBlank()) {
            return null;
        }
        String normalized = responseFileType.trim()
            .toUpperCase(java.util.Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');
        return switch (normalized) {
            case "STANDARD", "STANDARD_FILE", "STANDARD_FILE_FORMAT", "STANDARD_FILE_FORMATS" -> ProjectFileType.RESULT;
            case "ANALYSIS", "ANALYSIS_FILE", "ANALYSIS_FILES", "SEARCH_ENGINE_OUTPUT" -> ProjectFileType.SEARCH;
            case "SDRF", "EXPERIMENTAL_DESIGN", "SAMPLE_METADATA" -> ProjectFileType.EXPERIMENTAL_DESIGN;
            default -> {
                try {
                    yield ProjectFileType.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }

    private Boolean lookupPrideValidationForTable(DataFile dataFile) {
        if (dataFile == null || dataFile.getFile() == null) {
            return null;
        }
        return prideValidationByPath.get(dataFile.getFile().getAbsolutePath());
    }

    private String lookupResponseFileTypeForTable(DataFile dataFile) {
        if (dataFile == null || dataFile.getFile() == null) {
            return null;
        }
        return responseFileTypeByPath.get(dataFile.getFile().getAbsolutePath());
    }

    private Boolean lookupSdrfValidationForTable(DataFile dataFile) {
        if (dataFile == null || dataFile.getFile() == null) {
            return null;
        }
        if (dataFile.getFileType() != ProjectFileType.EXPERIMENTAL_DESIGN
                && !SdrfParserService.isSdrfFile(dataFile.getFile())) {
            return null;
        }
        return model.getSdrfValidation().lookup(dataFile.getFile().getAbsolutePath());
    }

    private boolean hasCurrentSuccessfulIncomingFileValidation() {
        List<DataFile> filesToValidate = getFilesRequiringIncomingValidation();
        if (filesToValidate.isEmpty()) {
            return !model.getFiles().isEmpty();
        }
        if (!incomingFileValidationPassed) {
            return false;
        }
        for (DataFile dataFile : filesToValidate) {
            if (dataFile.getFile() == null) {
                return false;
            }
            if (!Boolean.TRUE.equals(prideValidationByPath.get(dataFile.getFile().getAbsolutePath()))) {
                return false;
            }
        }
        return true;
    }

    private boolean requiresIncomingFileValidation() {
        return !getFilesRequiringIncomingValidation().isEmpty();
    }

    private List<DataFile> getFilesRequiringIncomingValidation() {
        return model.getFiles().stream()
            .filter(dataFile -> dataFile.getFile() != null)
            .filter(dataFile -> !isSdrfDataFile(dataFile))
            .collect(Collectors.toList());
    }

    private boolean isSdrfDataFile(DataFile dataFile) {
        if (dataFile == null || dataFile.getFile() == null) {
            return false;
        }
        return dataFile.getFileType() == ProjectFileType.EXPERIMENTAL_DESIGN
            || SdrfParserService.isSdrfFile(dataFile.getFile());
    }

    private void updateFileActionButtons() {
        if (removeSelectedButton != null && fileTable != null) {
            removeSelectedButton.setDisable(fileTable.getSelectionModel().isEmpty());
        }
        refreshFileStepState();
    }

    private List<File> getFilesWithInvalidSubmissionNames() {
        return model.getFiles().stream()
            .map(DataFile::getFile)
            .filter(file -> file != null && !FileNameValidator.isValidSubmissionFileName(file))
            .collect(Collectors.toList());
    }

    private String createInvalidFileNameSummary(List<File> invalidFiles) {
        return invalidFiles.size() + " file(s) must be renamed before continuing. " +
            FileNameValidator.SUBMISSION_FILE_NAME_RULE + " " +
            "Invalid: " + formatInvalidFileNames(invalidFiles, 3);
    }

    private void showInvalidFileNamesWarning(String title, List<File> invalidFiles) {
        if (invalidFiles == null || invalidFiles.isEmpty()) {
            return;
        }

        String message = "These file names cannot be added:\n\n" +
            formatInvalidFileNames(invalidFiles, MAX_INVALID_FILE_NAMES_TO_SHOW) +
            "\n\n" + FileNameValidator.SUBMISSION_FILE_NAME_RULE +
            "\n\nPlease rename the file(s) and select them again.";
        showWarning(title, message);
    }

    private String formatInvalidFileNames(List<File> invalidFiles, int limit) {
        String text = invalidFiles.stream()
            .limit(limit)
            .map(file -> "- " + file.getName())
            .collect(Collectors.joining("\n"));

        int remaining = invalidFiles.size() - Math.min(invalidFiles.size(), limit);
        if (remaining > 0) {
            text += "\n- ... and " + remaining + " more";
        }
        return text;
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
