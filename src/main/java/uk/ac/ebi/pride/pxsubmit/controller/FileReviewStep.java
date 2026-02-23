package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.util.FileTypeDetector;
import uk.ac.ebi.pride.pxsubmit.view.component.FileClassificationPanel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Step for reviewing and adjusting file classifications.
 * Shows files organized by category in tabs with validation status.
 *
 * Tabs (following PRIDE Guidelines terminology):
 * - RAW Files: Instrument raw data
 * - ANALYSIS Files: Tool outputs (MaxQuant, DIA-NN, FragPipe, etc.)
 * - STANDARD File Formats: mzIdentML, mzTab
 * - Database & Libraries: FASTA, spectral libraries
 * - Other Files: Unclassified and supplementary
 */
public class FileReviewStep extends AbstractWizardStep {

    private TabPane tabPane;
    private FileClassificationPanel summaryPanel;
    private Label validationStatusLabel;

    // Tables for each category
    private TableView<DataFile> rawTable;
    private TableView<DataFile> analysisTable;
    private TableView<DataFile> standardTable;
    private TableView<DataFile> databaseTable;
    private TableView<DataFile> otherTable;

    // Observable lists for each category
    private final ObservableList<DataFile> rawFiles = FXCollections.observableArrayList();
    private final ObservableList<DataFile> analysisFiles = FXCollections.observableArrayList();
    private final ObservableList<DataFile> standardFiles = FXCollections.observableArrayList();
    private final ObservableList<DataFile> databaseFiles = FXCollections.observableArrayList();
    private final ObservableList<DataFile> otherFiles = FXCollections.observableArrayList();

    public FileReviewStep(SubmissionModel model) {
        super("file-review",
              "Review Files",
              "Review and adjust file classifications before submission",
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

        // Top: Summary panel
        VBox topBox = new VBox(10);

        summaryPanel = new FileClassificationPanel();
        summaryPanel.setShowDetails(true);
        summaryPanel.setOnTypeSelected((type, files) -> selectTabForType(type));

        validationStatusLabel = new Label();
        validationStatusLabel.setStyle("-fx-font-weight: bold;");

        topBox.getChildren().addAll(summaryPanel, validationStatusLabel);
        root.setTop(topBox);
        BorderPane.setMargin(topBox, new Insets(0, 0, 10, 0));

        // Center: Tab pane with file tables
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Create tabs (following PRIDE Guidelines terminology)
        Tab rawTab = createTab("RAW", createRawFilesPane(), rawFiles);
        Tab analysisTab = createTab("ANALYSIS", createAnalysisPane(), analysisFiles);
        Tab standardTab = createTab("STANDARD", createStandardPane(), standardFiles);
        Tab databaseTab = createTab("Reference Database", createDatabasePane(), databaseFiles);
        Tab otherTab = createTab("Other", createOtherPane(), otherFiles);

        tabPane.getTabs().addAll(rawTab, analysisTab, standardTab, databaseTab, otherTab);
        root.setCenter(tabPane);

        return root;
    }

    private Tab createTab(String name, Parent content, ObservableList<DataFile> files) {
        Tab tab = new Tab();

        // Tab with count badge
        HBox header = new HBox(5);
        header.setAlignment(Pos.CENTER);
        Label nameLabel = new Label(name);
        Label countLabel = new Label("(0)");
        countLabel.setStyle("-fx-text-fill: #666;");

        // Update count when files change
        files.addListener((ListChangeListener.Change<? extends DataFile> c) -> {
            countLabel.setText("(" + files.size() + ")");
        });

        header.getChildren().addAll(nameLabel, countLabel);
        tab.setGraphic(header);
        tab.setContent(content);

        return tab;
    }

    private VBox createRawFilesPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));

        Label infoLabel = new Label(
            "RAW files are your instrument data files. These are mandatory for all submissions.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #666;");

        rawTable = createFileTable(rawFiles);
        VBox.setVgrow(rawTable, Priority.ALWAYS);

        pane.getChildren().addAll(infoLabel, rawTable);
        return pane;
    }

    private VBox createAnalysisPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));

        // Tool detection info
        VBox toolBox = new VBox(5);
        toolBox.setStyle("-fx-background-color: #e7f3ff; -fx-padding: 10; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label toolTitleLabel = new Label("Detected Analysis Tool");
        toolTitleLabel.setStyle("-fx-font-weight: bold;");

        Label toolNameLabel = new Label("Analyzing files...");
        toolNameLabel.setId("toolNameLabel");

        Label toolFilesLabel = new Label("");
        toolFilesLabel.setId("toolFilesLabel");
        toolFilesLabel.setWrapText(true);
        toolFilesLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        toolBox.getChildren().addAll(toolTitleLabel, toolNameLabel, toolFilesLabel);

        Label infoLabel = new Label(
            "ANALYSIS files are the native outputs from search engines and analysis tools " +
            "(MaxQuant, DIA-NN, FragPipe, Spectronaut, etc.). These contain the complete results of your computational analysis.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #666;");

        analysisTable = createFileTable(analysisFiles);
        VBox.setVgrow(analysisTable, Priority.ALWAYS);

        pane.getChildren().addAll(toolBox, infoLabel, analysisTable);
        return pane;
    }

    private VBox createStandardPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));

        // Info box
        VBox infoBox = new VBox(5);
        infoBox.setStyle("-fx-background-color: #f0e6ff; -fx-padding: 10; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label infoTitle = new Label("STANDARD File Formats");
        infoTitle.setStyle("-fx-font-weight: bold;");

        Label infoText = new Label(
            "STANDARD file formats (mzIdentML, mzTab) are community-standard formats developed by the " +
            "Proteomics Standards Initiative (PSI) that enable automated validation and enhanced interoperability.");
        infoText.setWrapText(true);
        infoText.setStyle("-fx-text-fill: #666;");

        infoBox.getChildren().addAll(infoTitle, infoText);

        standardTable = createFileTable(standardFiles);
        VBox.setVgrow(standardTable, Priority.ALWAYS);

        // ProteomeXchange notification (shown when standard files present)
        VBox pxNotice = new VBox(5);
        pxNotice.setStyle("-fx-background-color: #d4edda; -fx-padding: 10; -fx-border-radius: 4; -fx-background-radius: 4;");
        Label pxIcon = new Label("\u2714");
        pxIcon.setStyle("-fx-text-fill: #155724; -fx-font-size: 14px;");
        Label pxText = new Label("Your submission will be registered as a ProteomeXchange COMPLETE dataset, " +
            "enabling broader data discovery and reuse.");
        pxText.setWrapText(true);
        pxText.setStyle("-fx-text-fill: #155724;");
        HBox pxBox = new HBox(8, pxIcon, pxText);
        pxBox.setAlignment(Pos.CENTER_LEFT);
        pxNotice.getChildren().add(pxBox);
        pxNotice.visibleProperty().bind(Bindings.isNotEmpty(standardFiles));
        pxNotice.managedProperty().bind(Bindings.isNotEmpty(standardFiles));

        // Empty state note
        Label emptyLabel = new Label("No standard files added - this is optional. Your analysis outputs are sufficient for submission.");
        emptyLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        emptyLabel.setWrapText(true);
        emptyLabel.visibleProperty().bind(Bindings.isEmpty(standardFiles));
        emptyLabel.managedProperty().bind(Bindings.isEmpty(standardFiles));

        pane.getChildren().addAll(infoBox, standardTable, pxNotice, emptyLabel);
        return pane;
    }

    private VBox createDatabasePane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));

        Label infoLabel = new Label(
            "Include the FASTA database used for your search. Spectral libraries (.blib, .sptxt) can also be added here.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #666;");

        databaseTable = createFileTable(databaseFiles);
        VBox.setVgrow(databaseTable, Priority.ALWAYS);

        // Warning for missing FASTA
        HBox warningBox = new HBox(8);
        warningBox.setPadding(new Insets(8));
        warningBox.setStyle("-fx-background-color: #fff3cd; -fx-border-radius: 4; -fx-background-radius: 4;");
        warningBox.setAlignment(Pos.CENTER_LEFT);

        Label warningIcon = new Label("\u26A0");
        warningIcon.setStyle("-fx-text-fill: #856404;");
        Label warningText = new Label("No FASTA database found. Please ensure you've added your sequence database.");
        warningText.setStyle("-fx-text-fill: #856404;");

        warningBox.getChildren().addAll(warningIcon, warningText);

        // Show warning only when no FASTA files
        warningBox.visibleProperty().bind(Bindings.createBooleanBinding(() ->
            databaseFiles.stream().noneMatch(f -> FileTypeDetector.isFastaFile(f.getFile())),
            databaseFiles
        ));
        warningBox.managedProperty().bind(warningBox.visibleProperty());

        pane.getChildren().addAll(infoLabel, databaseTable, warningBox);
        return pane;
    }

    private VBox createOtherPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));

        Label infoLabel = new Label(
            "Files that don't fit other categories. You can change the file type using the dropdown if needed.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #666;");

        otherTable = createFileTable(otherFiles);
        VBox.setVgrow(otherTable, Priority.ALWAYS);

        pane.getChildren().addAll(infoLabel, otherTable);
        return pane;
    }

    @SuppressWarnings("unchecked")
    private TableView<DataFile> createFileTable(ObservableList<DataFile> items) {
        TableView<DataFile> table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Name column
        TableColumn<DataFile, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(data -> {
            DataFile df = data.getValue();
            return new javafx.beans.property.SimpleStringProperty(df.getFileName());
        });
        nameCol.setPrefWidth(250);

        // Size column
        TableColumn<DataFile, Long> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(data -> {
            DataFile df = data.getValue();
            long size = df.getFile() != null ? df.getFile().length() : 0;
            return new javafx.beans.property.SimpleLongProperty(size).asObject();
        });
        sizeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Long size, boolean empty) {
                super.updateItem(size, empty);
                if (empty || size == null) {
                    setText(null);
                } else {
                    setText(formatSize(size));
                }
            }
        });
        sizeCol.setPrefWidth(80);

        // Type column (editable)
        TableColumn<DataFile, ProjectFileType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> {
            DataFile df = data.getValue();
            return new javafx.beans.property.SimpleObjectProperty<>(df.getFileType());
        });
        typeCol.setCellFactory(ComboBoxTableCell.forTableColumn(ProjectFileType.values()));
        typeCol.setOnEditCommit(event -> {
            DataFile df = event.getRowValue();
            ProjectFileType newType = event.getNewValue();
            if (newType != null && newType != df.getFileType()) {
                df.setFileType(newType);
                reclassifyFiles();
            }
        });
        typeCol.setPrefWidth(150);
        typeCol.setEditable(true);

        // Path column
        TableColumn<DataFile, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(data -> {
            DataFile df = data.getValue();
            return new javafx.beans.property.SimpleStringProperty(df.getFilePath());
        });
        pathCol.setPrefWidth(300);

        table.getColumns().addAll(nameCol, sizeCol, typeCol, pathCol);
        table.setEditable(true);

        // Empty placeholder
        table.setPlaceholder(new Label("No files in this category"));

        return table;
    }

    private void selectTabForType(ProjectFileType type) {
        int tabIndex = switch (type) {
            case RAW -> 0;
            case SEARCH -> 1;
            case RESULT -> 2;
            case OTHER -> {
                // Check if it's database/library
                yield 4; // Default to Other tab
            }
            default -> 4;
        };
        tabPane.getSelectionModel().select(tabIndex);
    }

    @Override
    protected void initializeStep() {
        // Listen for file changes
        model.getFiles().addListener((ListChangeListener.Change<? extends DataFile> c) -> {
            reclassifyFiles();
        });

        // Valid when at least RAW files are present
        valid.bind(Bindings.createBooleanBinding(() ->
            !rawFiles.isEmpty(),
            rawFiles
        ));
    }

    @Override
    protected void onStepEntering() {
        reclassifyFiles();
        updateToolDetection();
        updateValidationStatus();
    }

    /**
     * Reclassify files into categories
     */
    private void reclassifyFiles() {
        rawFiles.clear();
        analysisFiles.clear();
        standardFiles.clear();
        databaseFiles.clear();
        otherFiles.clear();

        for (DataFile df : model.getFiles()) {
            ProjectFileType type = df.getFileType();

            // Check for special types first
            if (FileTypeDetector.isFastaFile(df.getFile()) ||
                FileTypeDetector.isSpectralLibrary(df.getFile())) {
                databaseFiles.add(df);
            } else {
                switch (type) {
                    case RAW -> rawFiles.add(df);
                    case SEARCH -> analysisFiles.add(df);
                    case RESULT -> standardFiles.add(df);
                    case PEAK -> otherFiles.add(df); // Peak lists go to other
                    case EXPERIMENTAL_DESIGN -> otherFiles.add(df);
                    default -> otherFiles.add(df);
                }
            }
        }

        // Update summary panel
        summaryPanel.setFiles(model.getFiles());
        updateValidationStatus();
    }

    /**
     * Update tool detection info
     */
    private void updateToolDetection() {
        FileTypeDetector.ToolDetectionResult tool = FileTypeDetector.detectTool(model.getFiles());

        // Find the labels in the analysis pane
        Tab analysisTab = tabPane.getTabs().get(1);
        VBox pane = (VBox) analysisTab.getContent();

        for (javafx.scene.Node node : pane.getChildren()) {
            if (node instanceof VBox toolBox) {
                for (javafx.scene.Node child : toolBox.getChildren()) {
                    if (child instanceof Label label) {
                        if ("toolNameLabel".equals(label.getId())) {
                            if (tool.isConfident()) {
                                label.setText(tool.tool().getDisplayName() +
                                    String.format(" (%.0f%% confidence)", tool.confidence() * 100));
                                label.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                            } else {
                                label.setText("No specific tool detected");
                                label.setStyle("-fx-text-fill: #666;");
                            }
                        } else if ("toolFilesLabel".equals(label.getId())) {
                            if (tool.isConfident()) {
                                StringBuilder sb = new StringBuilder();
                                if (!tool.foundRequiredFiles().isEmpty()) {
                                    sb.append("Found: ").append(String.join(", ", tool.foundRequiredFiles()));
                                }
                                if (!tool.missingRequiredFiles().isEmpty()) {
                                    if (sb.length() > 0) sb.append(" | ");
                                    sb.append("Missing: ").append(String.join(", ", tool.missingRequiredFiles()));
                                }
                                label.setText(sb.toString());
                            } else {
                                label.setText("Add analysis output files to enable tool detection");
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * Update validation status
     */
    private void updateValidationStatus() {
        boolean hasRaw = !rawFiles.isEmpty();
        boolean hasAnalysis = !analysisFiles.isEmpty();
        boolean hasStandard = !standardFiles.isEmpty();
        boolean hasFasta = databaseFiles.stream()
            .anyMatch(f -> FileTypeDetector.isFastaFile(f.getFile()));

        if (!hasRaw) {
            validationStatusLabel.setText("\u26A0 Missing RAW files - required for submission");
            validationStatusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545;");
        } else if (!hasAnalysis && !hasStandard) {
            validationStatusLabel.setText("\u26A0 No analysis files - add your search engine outputs");
            validationStatusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffc107;");
        } else if (!hasFasta) {
            validationStatusLabel.setText("\u26A0 FASTA database recommended for complete documentation");
            validationStatusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffc107;");
        } else if (hasStandard) {
            validationStatusLabel.setText("\u2714 Ready to submit - ProteomeXchange COMPLETE");
            validationStatusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");
        } else {
            validationStatusLabel.setText("\u2714 Ready to submit - all required files present");
            validationStatusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");
        }
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    @Override
    public boolean validate() {
        if (rawFiles.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Missing Files");
            alert.setHeaderText("RAW files required");
            alert.setContentText("Please add at least one RAW file to your submission.");
            alert.showAndWait();
            return false;
        }
        return true;
    }
}
