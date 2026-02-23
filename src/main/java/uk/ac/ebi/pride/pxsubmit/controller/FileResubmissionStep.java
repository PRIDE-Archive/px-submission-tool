package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.submission.model.File.ProjectFile;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Resubmission;
import uk.ac.ebi.pride.data.model.ResubmissionFileChangeState;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.ApiService;
import uk.ac.ebi.pride.pxsubmit.util.FileTypeDetector;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Step for managing files during resubmission.
 * Two-panel layout:
 * - Top: New files to add (with Add/Remove buttons)
 * - Bottom: Existing project files fetched from server (with NONE/MODIFY/DELETE actions)
 *
 * This step is skipped for normal (non-resubmission) submissions.
 * Existing server files are tracked only in the Resubmission object's map,
 * NOT in model.getFiles(), because ChecksumComputationStep iterates model.getFiles()
 * and server-only files have no local File to checksum.
 */
public class FileResubmissionStep extends AbstractWizardStep {

    // New files added by user (filtered view of model.getFiles())
    private final ObservableList<DataFile> newFiles = FXCollections.observableArrayList();

    // Existing files fetched from server
    private final ObservableList<DataFile> existingFiles = FXCollections.observableArrayList();

    private TableView<DataFile> newFilesTable;
    private TableView<DataFile> existingFilesTable;
    private Label summaryLabel;
    private ProgressIndicator loadingIndicator;
    private Label loadingLabel;
    private VBox existingFilesContent;

    private boolean existingFilesLoaded = false;
    private ApiService apiService;

    public FileResubmissionStep(SubmissionModel model) {
        super("file-resubmission",
              "Resubmission Files",
              "Manage files for your resubmission - add new files and update existing ones",
              model);
    }

    @Override
    public boolean canSkip() {
        return !model.isResubmissionMode();
    }

    @Override
    protected Parent createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Summary label at top
        summaryLabel = new Label("Loading...");
        summaryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        summaryLabel.setMaxWidth(Double.MAX_VALUE);

        // ==================== Top Panel: New Files ====================
        TitledPane newFilesPane = new TitledPane();
        newFilesPane.setText("New Files to Add");
        newFilesPane.setCollapsible(false);

        VBox newFilesBox = new VBox(8);
        newFilesBox.setPadding(new Insets(8));

        // Button bar
        HBox buttonBar = new HBox(8);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        Button addFilesButton = new Button("Add Files...");
        addFilesButton.setOnAction(e -> addFiles());

        Button addFolderButton = new Button("Add Folder...");
        addFolderButton.setOnAction(e -> addFolder());

        Button removeSelectedButton = new Button("Remove Selected");
        removeSelectedButton.setOnAction(e -> removeSelectedNewFiles());
        removeSelectedButton.setDisable(true);

        Button resetAllButton = new Button("Reset All");
        resetAllButton.setStyle("-fx-text-fill: #dc3545;");
        resetAllButton.setOnAction(e -> resetAll());

        buttonBar.getChildren().addAll(addFilesButton, addFolderButton, removeSelectedButton, resetAllButton);

        // New files table
        newFilesTable = createNewFilesTable();
        newFilesTable.setPrefHeight(150);
        newFilesTable.setMinHeight(100);
        VBox.setVgrow(newFilesTable, Priority.SOMETIMES);

        // Enable remove button when selection exists
        newFilesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
            removeSelectedButton.setDisable(newFilesTable.getSelectionModel().isEmpty()));

        newFilesBox.getChildren().addAll(buttonBar, newFilesTable);
        newFilesPane.setContent(newFilesBox);

        // ==================== Bottom Panel: Existing Files ====================
        TitledPane existingFilesPane = new TitledPane();
        existingFilesPane.setText("Existing Project Files");
        existingFilesPane.setCollapsible(false);

        existingFilesContent = new VBox(8);
        existingFilesContent.setPadding(new Insets(8));

        // Loading indicator
        HBox loadingBox = new HBox(10);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(20));

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(30, 30);

        loadingLabel = new Label("Loading existing project files...");
        loadingLabel.setStyle("-fx-text-fill: #666;");

        loadingBox.getChildren().addAll(loadingIndicator, loadingLabel);

        // Existing files table
        existingFilesTable = createExistingFilesTable();
        existingFilesTable.setPrefHeight(250);
        existingFilesTable.setMinHeight(100);
        VBox.setVgrow(existingFilesTable, Priority.ALWAYS);

        VBox.setVgrow(existingFilesTable, Priority.ALWAYS);
        existingFilesContent.getChildren().addAll(loadingBox);
        existingFilesPane.setContent(existingFilesContent);

        // Make the content VBox grow inside the TitledPane
        VBox.setVgrow(newFilesTable, Priority.ALWAYS);

        // Layout - no ScrollPane so tables resize with window
        VBox.setVgrow(newFilesPane, Priority.SOMETIMES);
        VBox.setVgrow(existingFilesPane, Priority.ALWAYS);
        root.getChildren().addAll(summaryLabel, newFilesPane, existingFilesPane);

        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<DataFile> createNewFilesTable() {
        TableView<DataFile> table = new TableView<>(newFiles);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Name column
        TableColumn<DataFile, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getFileName()));
        nameCol.setPrefWidth(250);

        // Size column
        TableColumn<DataFile, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(data -> {
            DataFile df = data.getValue();
            long size = df.getFile() != null ? df.getFile().length() : df.getFileSize();
            return new SimpleStringProperty(formatSize(size));
        });
        sizeCol.setPrefWidth(80);

        // Type column (editable)
        TableColumn<DataFile, ProjectFileType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data ->
            new SimpleObjectProperty<>(data.getValue().getFileType()));
        typeCol.setCellFactory(ComboBoxTableCell.forTableColumn(ProjectFileType.values()));
        typeCol.setOnEditCommit(event -> {
            DataFile df = event.getRowValue();
            ProjectFileType newType = event.getNewValue();
            if (newType != null) {
                df.setFileType(newType);
            }
        });
        typeCol.setPrefWidth(120);
        typeCol.setEditable(true);

        // Remove button column
        TableColumn<DataFile, Void> removeCol = new TableColumn<>("");
        removeCol.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("X");
            {
                removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc3545; -fx-font-weight: bold; -fx-cursor: hand;");
                removeBtn.setOnAction(e -> {
                    DataFile df = getTableView().getItems().get(getIndex());
                    removeNewFile(df);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });
        removeCol.setPrefWidth(40);
        removeCol.setMaxWidth(40);

        table.getColumns().addAll(nameCol, sizeCol, typeCol, removeCol);
        table.setEditable(true);
        table.setPlaceholder(new Label("No new files added - use the buttons above to add files"));

        return table;
    }

    @SuppressWarnings("unchecked")
    private TableView<DataFile> createExistingFilesTable() {
        TableView<DataFile> table = new TableView<>(existingFiles);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Name column
        TableColumn<DataFile, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getFileName()));
        nameCol.setPrefWidth(250);

        // Size column
        TableColumn<DataFile, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(data -> {
            long size = data.getValue().getFileSize();
            return new SimpleStringProperty(formatSize(size));
        });
        sizeCol.setPrefWidth(80);

        // Type column (read-only)
        TableColumn<DataFile, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getFileType() != null
                ? data.getValue().getFileType().name() : "OTHER"));
        typeCol.setPrefWidth(100);

        // Action column (ComboBox: NONE / MODIFY / DELETE)
        TableColumn<DataFile, ResubmissionFileChangeState> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(data -> {
            DataFile df = data.getValue();
            Resubmission resub = model.getResubmission();
            ResubmissionFileChangeState state = resub.getResubmission().getOrDefault(df, ResubmissionFileChangeState.NONE);
            return new SimpleObjectProperty<>(state);
        });
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<ResubmissionFileChangeState> combo = new ComboBox<>(
                FXCollections.observableArrayList(
                    ResubmissionFileChangeState.NONE,
                    ResubmissionFileChangeState.MODIFY,
                    ResubmissionFileChangeState.DELETE
                )
            );
            {
                combo.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        DataFile df = getTableView().getItems().get(getIndex());
                        ResubmissionFileChangeState newState = combo.getValue();
                        updateExistingFileState(df, newState);
                        updateSummary();
                    }
                });
            }

            @Override
            protected void updateItem(ResubmissionFileChangeState item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    combo.setValue(item);
                    setGraphic(combo);
                }
            }
        });
        actionCol.setPrefWidth(120);

        table.getColumns().addAll(nameCol, sizeCol, typeCol, actionCol);
        table.setPlaceholder(new Label("No existing files found"));

        return table;
    }

    @Override
    protected void initializeStep() {
        // Update newFiles list when model files change
        model.getFiles().addListener((ListChangeListener.Change<? extends DataFile> c) -> {
            refreshNewFilesList();
            updateSummary();
        });

        // Valid when we have at least some change (new files or existing file modifications)
        valid.bind(Bindings.createBooleanBinding(() -> {
            // Always valid - user can proceed even with no changes
            return true;
        }, newFiles, existingFiles));
    }

    @Override
    protected void onStepEntering() {
        refreshNewFilesList();

        if (!existingFilesLoaded) {
            loadExistingFiles();
        }

        updateSummary();
    }

    /**
     * Load existing project files from the server
     */
    private void loadExistingFiles() {
        String accession = model.getSubmission().getProjectMetaData().getResubmissionPxAccession();
        if (accession == null || accession.isEmpty()) {
            showExistingFilesLoaded();
            loadingLabel.setText("No project accession found");
            loadingIndicator.setVisible(false);
            return;
        }

        // Create ApiService if needed
        if (apiService == null) {
            String username = model.getUserName();
            String password = model.getPassword();
            if (username != null && password != null) {
                apiService = new ApiService(username, password);
            } else {
                showExistingFilesLoaded();
                loadingLabel.setText("Not logged in - cannot fetch project files");
                loadingIndicator.setVisible(false);
                return;
            }
        }

        logger.info("Fetching existing files for accession: {}", accession);

        apiService.getProjectFiles(accession)
            .thenAccept(projectFiles -> Platform.runLater(() -> {
                populateExistingFiles(projectFiles);
                existingFilesLoaded = true;
                showExistingFilesLoaded();
                updateSummary();
                logger.info("Loaded {} existing project files", projectFiles.size());
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    logger.error("Failed to load existing files: {}", ex.getMessage(), ex);
                    loadingLabel.setText("Failed to load files: " + ex.getMessage());
                    loadingIndicator.setVisible(false);
                    showExistingFilesLoaded();
                });
                return null;
            });
    }

    /**
     * Convert ProjectFile objects from server into DataFile objects and add to resubmission
     */
    private void populateExistingFiles(List<ProjectFile> projectFiles) {
        existingFiles.clear();
        Resubmission resub = model.getResubmission();

        for (ProjectFile pf : projectFiles) {
            DataFile dataFile = new DataFile();
            dataFile.setFileId(model.nextFileId());
            dataFile.setFileType(pf.getFileType());
            dataFile.setFileSize(pf.getFileSize());

            // Set URL from file path if available
            if (pf.getFilePath() != null && !pf.getFilePath().isEmpty()) {
                try {
                    dataFile.setUrl(new URL(pf.getFilePath()));
                } catch (MalformedURLException e) {
                    // File path is not a URL, just use it as-is via a file reference
                    logger.debug("File path is not a URL: {}", pf.getFilePath());
                }
            }

            // Set file name - use fileName from server, or extract from path
            if (pf.getFileName() != null && !pf.getFileName().isEmpty()) {
                // DataFile doesn't have a setFileName - it derives from File or URL
                // We need to create a File reference with the name
                if (dataFile.getFile() == null && dataFile.getUrl() == null) {
                    // Create a placeholder File so getFileName() works
                    dataFile.setFile(new File(pf.getFileName()));
                }
            }

            existingFiles.add(dataFile);

            // Track in resubmission with NONE state (no change by default)
            resub.addDataFile(dataFile);
            resub.getResubmission().put(dataFile, ResubmissionFileChangeState.NONE);
        }
    }

    /**
     * Switch from loading view to table view for existing files
     */
    private void showExistingFilesLoaded() {
        existingFilesContent.getChildren().clear();
        existingFilesContent.getChildren().add(existingFilesTable);
        VBox.setVgrow(existingFilesTable, Priority.ALWAYS);
    }

    /**
     * Refresh the new files list from model.getFiles()
     * Only includes files that are in ADD state in the resubmission
     */
    private void refreshNewFilesList() {
        Resubmission resub = model.getResubmission();
        Map<DataFile, ResubmissionFileChangeState> resubMap = resub.getResubmission();

        newFiles.clear();
        for (DataFile df : model.getFiles()) {
            ResubmissionFileChangeState state = resubMap.get(df);
            if (state == ResubmissionFileChangeState.ADD) {
                newFiles.add(df);
            }
        }
    }

    /**
     * Update the state of an existing file in the resubmission map.
     * When set to MODIFY, checks if a matching new file (same name) exists.
     */
    private void updateExistingFileState(DataFile dataFile, ResubmissionFileChangeState newState) {
        Resubmission resub = model.getResubmission();
        resub.getResubmission().put(dataFile, newState);

        if (newState == ResubmissionFileChangeState.MODIFY) {
            String existingName = dataFile.getFileName();
            boolean hasMatch = newFiles.stream()
                .anyMatch(nf -> nf.getFileName() != null && nf.getFileName().equals(existingName));
            if (!hasMatch) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Missing Replacement File");
                alert.setHeaderText("No replacement file found for: " + existingName);
                alert.setContentText(
                    "You marked this file as MODIFY but haven't added a new file with the same name.\n\n" +
                    "Please add a file named '" + existingName + "' in the 'New Files' section above.");
                alert.showAndWait();
            }
        }
    }

    // ==================== File Add/Remove Operations ====================

    private void addFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Add");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("Raw Files", "*.raw", "*.RAW", "*.wiff", "*.d", "*.mzML", "*.mzXML"),
            new FileChooser.ExtensionFilter("Result Files", "*.mzid", "*.mzIdentML", "*.mzTab", "*.xml"),
            new FileChooser.ExtensionFilter("Search Files", "*.dat", "*.msf", "*.pepXML")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(
            newFilesTable.getScene() != null ? newFilesTable.getScene().getWindow() : null);
        if (files != null && !files.isEmpty()) {
            addFilesToModel(files);
        }
    }

    private void addFolder() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Folder to Add");

        File directory = dirChooser.showDialog(
            newFilesTable.getScene() != null ? newFilesTable.getScene().getWindow() : null);
        if (directory != null && directory.isDirectory()) {
            File[] files = directory.listFiles(File::isFile);
            if (files != null && files.length > 0) {
                addFilesToModel(List.of(files));
            }
        }
    }

    private void addFilesToModel(List<File> files) {
        for (File file : files) {
            if (!file.isFile()) continue;

            // Check for duplicates
            boolean exists = model.getFiles().stream()
                .anyMatch(df -> df.getFile() != null &&
                    df.getFile().getAbsolutePath().equals(file.getAbsolutePath()));
            if (exists) continue;

            DataFile dataFile = new DataFile();
            dataFile.setFile(file);
            dataFile.setFileType(detectFileType(file));
            // model.addFile() handles resubmission mode (sets ADD state)
            model.addFile(dataFile);
        }
        updateSummary();
    }

    private void removeNewFile(DataFile dataFile) {
        model.removeFile(dataFile);
        updateSummary();
    }

    private void removeSelectedNewFiles() {
        List<DataFile> selected = List.copyOf(newFilesTable.getSelectionModel().getSelectedItems());
        for (DataFile df : selected) {
            model.removeFile(df);
        }
        updateSummary();
    }

    private void resetAll() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset All Changes");
        alert.setHeaderText("Reset all file changes?");
        alert.setContentText("This will remove all new files and reset existing file actions to NONE.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Remove all new files
                List<DataFile> toRemove = List.copyOf(newFiles);
                for (DataFile df : toRemove) {
                    model.removeFile(df);
                }

                // Reset existing files to NONE
                Resubmission resub = model.getResubmission();
                for (DataFile df : existingFiles) {
                    resub.getResubmission().put(df, ResubmissionFileChangeState.NONE);
                }

                // Refresh table to show updated states
                existingFilesTable.refresh();
                updateSummary();
            }
        });
    }

    // ==================== Summary ====================

    private void updateSummary() {
        Resubmission resub = model.getResubmission();
        Map<DataFile, ResubmissionFileChangeState> resubMap = resub.getResubmission();

        int addCount = 0;
        int modifyCount = 0;
        int deleteCount = 0;
        int unchangedCount = 0;

        for (ResubmissionFileChangeState state : resubMap.values()) {
            switch (state) {
                case ADD -> addCount++;
                case MODIFY -> modifyCount++;
                case DELETE -> deleteCount++;
                case NONE -> unchangedCount++;
            }
        }

        // Check for MODIFY files without matching new files
        int missingModifyCount = 0;
        StringBuilder missingNames = new StringBuilder();
        for (Map.Entry<DataFile, ResubmissionFileChangeState> entry : resubMap.entrySet()) {
            if (entry.getValue() == ResubmissionFileChangeState.MODIFY) {
                String name = entry.getKey().getFileName();
                boolean hasMatch = newFiles.stream()
                    .anyMatch(nf -> nf.getFileName() != null && nf.getFileName().equals(name));
                if (!hasMatch) {
                    missingModifyCount++;
                    if (missingNames.length() > 0) missingNames.append(", ");
                    missingNames.append(name);
                }
            }
        }

        String summaryText = String.format(
            "New: %d  |  Modified: %d  |  Deleted: %d  |  Unchanged: %d",
            addCount, modifyCount, deleteCount, unchangedCount);

        if (missingModifyCount > 0) {
            summaryText += String.format("\nMissing replacement file(s): %s", missingNames);
        }
        summaryLabel.setText(summaryText);

        // Color based on changes and warnings
        if (missingModifyCount > 0) {
            summaryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #cc7a00;");
        } else if (addCount > 0 || modifyCount > 0 || deleteCount > 0) {
            summaryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #28a745;");
        } else {
            summaryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #666;");
        }
    }

    @Override
    public boolean validate() {
        Resubmission resub = model.getResubmission();
        Map<DataFile, ResubmissionFileChangeState> resubMap = resub.getResubmission();

        // Check that all MODIFY files have a matching new file with the same name
        List<String> missingReplacements = new java.util.ArrayList<>();
        for (Map.Entry<DataFile, ResubmissionFileChangeState> entry : resubMap.entrySet()) {
            if (entry.getValue() == ResubmissionFileChangeState.MODIFY) {
                String name = entry.getKey().getFileName();
                boolean hasMatch = newFiles.stream()
                    .anyMatch(nf -> nf.getFileName() != null && nf.getFileName().equals(name));
                if (!hasMatch) {
                    missingReplacements.add(name);
                }
            }
        }

        if (!missingReplacements.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Missing Replacement Files");
            alert.setHeaderText("Cannot proceed - replacement files are missing");
            alert.setContentText(
                "The following files are marked as MODIFY but no replacement file " +
                "with the same name has been added:\n\n" +
                String.join("\n", missingReplacements) +
                "\n\nPlease add the replacement files or change the action to NONE.");
            alert.showAndWait();
            return false;
        }

        boolean hasChanges = resubMap.values().stream()
            .anyMatch(state -> state != ResubmissionFileChangeState.NONE);

        if (!hasChanges) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("No Changes");
            alert.setHeaderText("No file changes detected");
            alert.setContentText(
                "You haven't added any new files or modified/deleted existing ones.\n\n" +
                "Do you want to proceed anyway?");

            ButtonType proceed = new ButtonType("Proceed");
            ButtonType goBack = new ButtonType("Go Back", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(proceed, goBack);

            var result = alert.showAndWait();
            return result.isPresent() && result.get() == proceed;
        }

        return true;
    }

    // ==================== Utilities ====================

    private ProjectFileType detectFileType(File file) {
        try {
            return FileTypeDetector.detectFileType(file);
        } catch (Exception e) {
            logger.warn("Could not detect file type for: {}", file.getName());
            return ProjectFileType.OTHER;
        }
    }

    private String formatSize(long size) {
        if (size <= 0) return "-";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
