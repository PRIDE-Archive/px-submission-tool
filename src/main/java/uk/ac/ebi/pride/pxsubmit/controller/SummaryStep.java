package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.io.SubmissionFileWriter;
import uk.ac.ebi.pride.data.model.Resubmission;
import uk.ac.ebi.pride.data.model.ResubmissionFileChangeState;
import uk.ac.ebi.pride.pxsubmit.config.AppConfig;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.util.FileTypeDetector;
import uk.ac.ebi.pride.pxsubmit.view.component.FileClassificationPanel;
import uk.ac.ebi.pride.pxsubmit.view.component.ValidationFeedback;

import org.slf4j.LoggerFactory;

import javafx.scene.control.cell.PropertyValueFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Step for reviewing submission summary before upload.
 * Displays all collected information for verification.
 */
public class SummaryStep extends AbstractWizardStep {

    private VBox contentBox;
    private Label exportStatusLabel;

    public SummaryStep(SubmissionModel model) {
        super("summary",
              "Summary",
              "Please review your submission details before uploading",
              model);
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20));

        scrollPane.setContent(contentBox);
        return scrollPane;
    }

    @Override
    protected void initializeStep() {
        valid.set(true); // Summary is always valid
    }

    @Override
    protected void onStepEntering() {
        // Sync model properties to the Submission object before building summary.
        // This is critical for resubmission mode where ProjectMetadataStep and
        // SampleMetadataStep are skipped (they normally call syncMetadataToSubmission).
        model.syncMetadataToSubmission();

        // Rebuild summary each time we enter
        buildSummary();
    }

    private void buildSummary() {
        contentBox.getChildren().clear();

        // Resubmission info banner
        if (model.isResubmissionMode()) {
            String accession = model.getSubmission().getProjectMetaData().getResubmissionPxAccession();
            Label resubLabel = new Label(
                "Resubmission to: " + (accession != null ? accession : "Unknown"));
            resubLabel.setStyle(
                "-fx-background-color: #d1ecf1; " +
                "-fx-text-fill: #0c5460; " +
                "-fx-padding: 12; " +
                "-fx-background-radius: 5; " +
                "-fx-font-weight: bold;");
            resubLabel.setWrapText(true);
            resubLabel.setMaxWidth(Double.MAX_VALUE);
            contentBox.getChildren().add(resubLabel);
        }

        // Test mode warning (at top)
        if (model.isTrainingMode()) {
            Label warningLabel = new Label(
                "\u26A0 TEST MODE: This submission will not be processed. " +
                "No files will be uploaded to the production server.");
            warningLabel.setStyle(
                "-fx-background-color: #fff3cd; " +
                "-fx-text-fill: #856404; " +
                "-fx-padding: 12; " +
                "-fx-background-radius: 5; " +
                "-fx-font-weight: bold;");
            warningLabel.setWrapText(true);
            warningLabel.setMaxWidth(Double.MAX_VALUE);
            contentBox.getChildren().add(warningLabel);
        }

        // Crosslinking warning banner
        if (!model.isResubmissionMode() && isCrosslinkDataset()) {
            contentBox.getChildren().add(createCrosslinkBanner());
        }

        // Validation Summary
        ValidationFeedback validation = createValidationSummary();
        contentBox.getChildren().add(validation);

        // Submission type
        String typeDisplay = model.isResubmissionMode() ? "Resubmission" :
            (model.getSubmissionType() != null ? model.getSubmissionType().toString() : "Not specified");
        addSection("Submission Type", typeDisplay);

        // Project Information, Metadata, Lab Head — skip for resubmission
        // (metadata is already on the server for the existing project)
        if (!model.isResubmissionMode()) {
            VBox projectSection = createSectionBox("Project Information", "\uD83D\uDCCB");
            addField(projectSection, "Title", model.getProjectTitle());
            addField(projectSection, "Description", model.getProjectDescription());
            addField(projectSection, "Keywords", model.getKeywords());
            addListField(projectSection, "Experiment Type",
                model.getExperimentMethods().stream().map(CvParam::getName).collect(Collectors.toList()));
            addListField(projectSection, "Software",
                model.getSoftware().stream().map(CvParam::getName).collect(Collectors.toList()));
            addField(projectSection, "Sample Processing", model.getSampleProcessingProtocol());
            addField(projectSection, "Data Processing", model.getDataProcessingProtocol());
            contentBox.getChildren().add(projectSection);

            VBox metadataSection = createSectionBox("Metadata", "\uD83E\uDDEC");
            addListField(metadataSection, "Species",
                model.getSpecies().stream().map(CvParam::getName).collect(Collectors.toList()));
            addListField(metadataSection, "Instruments",
                model.getInstruments().stream().map(CvParam::getName).collect(Collectors.toList()));
            addListField(metadataSection, "Modifications",
                model.getModifications().stream().map(CvParam::getName).collect(Collectors.toList()));
            addListField(metadataSection, "Quantification",
                model.getQuantifications().stream().map(CvParam::getName).collect(Collectors.toList()));
            contentBox.getChildren().add(metadataSection);

            VBox labHeadSection = createSectionBox("Lab Head", "\uD83D\uDC64");
            addField(labHeadSection, "Name", model.getLabHeadName());
            addField(labHeadSection, "Email", model.getLabHeadEmail());
            addField(labHeadSection, "Affiliation", model.getLabHeadAffiliation());
            addField(labHeadSection, "Country", model.getLabHeadCountry());
            if (model.getLabHeadOrcid() != null && !model.getLabHeadOrcid().isEmpty()) {
                addField(labHeadSection, "ORCID iD", model.getLabHeadOrcid());
            }
            contentBox.getChildren().add(labHeadSection);

            // Project References
            var meta = model.getSubmission().getProjectMetaData();
            if (meta != null) {
                VBox referencesSection = createSectionBox("References & Links", "\uD83D\uDD17");
                addField(referencesSection, "PubMed IDs",
                    meta.hasPubmedIds() ? String.join(", ", meta.getPubmedIds()) : null);
                addField(referencesSection, "Omics Dataset Links",
                    meta.hasOtherOmicsLink() ? meta.getOtherOmicsLink() : null);
                if (!meta.getProjectTags().isEmpty()) {
                    addField(referencesSection, "Project Tags", String.join(", ", meta.getProjectTags()));
                }
                contentBox.getChildren().add(referencesSection);
            }
        }

        // Resubmission file changes (only in resubmission mode)
        if (model.isResubmissionMode()) {
            VBox resubFilesSection = createSectionBox("Resubmission File Changes", "\uD83D\uDD04");
            addResubmissionFileSummary(resubFilesSection);
            contentBox.getChildren().add(resubFilesSection);
        }

        // Files
        VBox filesSection = createSectionBox("Files", "\uD83D\uDCC1");
        addFileSummary(filesSection);
        contentBox.getChildren().add(filesSection);

        // Export submission.px section
        VBox exportSection = createSectionBox("Export Submission", "\uD83D\uDCBE");

        Label autoSaveNote = new Label("Note: Your submission.px file will be automatically saved before upload.");
        autoSaveNote.setWrapText(true);
        autoSaveNote.setStyle("-fx-text-fill: #28a745; -fx-font-style: italic;");

        Label exportInstructions = new Label("You can also export a copy to a custom location:");
        exportInstructions.setWrapText(true);
        exportInstructions.setStyle("-fx-text-fill: #666;");

        exportStatusLabel = new Label();
        exportStatusLabel.setWrapText(true);
        exportStatusLabel.setVisible(false);
        exportStatusLabel.setManaged(false);

        Button exportButton = new Button("Export submission.px");
        exportButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white;");
        exportButton.setOnAction(e -> exportSubmissionFile());

        HBox exportButtonBox = new HBox(10);
        exportButtonBox.setAlignment(Pos.CENTER_LEFT);
        exportButtonBox.setPadding(new Insets(5, 0, 0, 0));
        exportButtonBox.getChildren().addAll(exportButton, exportStatusLabel);

        exportSection.getChildren().addAll(autoSaveNote, exportInstructions, exportButtonBox);
        contentBox.getChildren().add(exportSection);
    }

    private void exportSubmissionFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Submission File");
        fileChooser.setInitialFileName("submission");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PX Submission Files (*.px)", "*.px")
        );

        File file = fileChooser.showSaveDialog(contentBox.getScene().getWindow());
        if (file != null) {
            try {
                writeSubmissionPxFile(model, file);
                exportStatusLabel.setText("Exported to: " + file.getAbsolutePath());
                exportStatusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 11px;");
                exportStatusLabel.setVisible(true);
                exportStatusLabel.setManaged(true);
                logger.info("Submission file exported to: {}", file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Failed to export submission file", ex);
                exportStatusLabel.setText("Export failed: " + ex.getMessage());
                exportStatusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px;");
                exportStatusLabel.setVisible(true);
                exportStatusLabel.setManaged(true);
            }
        }
    }

    private ValidationFeedback createValidationSummary() {
        ValidationFeedback feedback = new ValidationFeedback();

        if (model.isResubmissionMode()) {
            // Resubmission: validate file changes only (metadata is already on server)
            Resubmission resub = model.getResubmission();
            java.util.Map<DataFile, ResubmissionFileChangeState> resubMap = resub.getResubmission();

            boolean hasChanges = resubMap.values().stream()
                .anyMatch(state -> state != ResubmissionFileChangeState.NONE);

            if (!hasChanges) {
                feedback.addWarning("No file changes specified - consider adding, modifying, or deleting files");
            }

            long addCount = resubMap.values().stream()
                .filter(s -> s == ResubmissionFileChangeState.ADD).count();
            if (addCount > 0) {
                feedback.addInfo(addCount + " new file(s) will be uploaded");
            }

            if (!feedback.hasErrors() && !feedback.hasWarnings()) {
                feedback.setSuccess("Resubmission ready - file changes will be applied");
            }
        } else {
            // Normal submission: full validation
            long rawCount = model.getFiles().stream()
                .filter(f -> f.getFileType() == ProjectFileType.RAW).count();
            long analysisCount = model.getFiles().stream()
                .filter(f -> f.getFileType() == ProjectFileType.SEARCH).count();
            long standardCount = model.getFiles().stream()
                .filter(f -> f.getFileType() == ProjectFileType.RESULT).count();
            boolean hasFasta = model.getFiles().stream()
                .anyMatch(f -> FileTypeDetector.isFastaFile(f.getFile()));

            // File validation
            if (rawCount == 0) {
                feedback.addError("No RAW files - at least one RAW file is required");
            }
            if (analysisCount == 0 && standardCount == 0) {
                feedback.addWarning("No analysis or standard result files included");
            }
            if (!hasFasta) {
                feedback.addInfo("Recommended: Add a FASTA database file for sequence validation");
            }

            // Check for SDRF
            boolean hasSdrf = model.getFiles().stream()
                .anyMatch(f -> f.getFileType() == ProjectFileType.EXPERIMENTAL_DESIGN ||
                              (f.getFile() != null && f.getFile().getName().toLowerCase().contains("sdrf")));
            if (!hasSdrf) {
                feedback.addInfo("Recommended: Add an SDRF file for sample metadata");
            }

            // Metadata validation
            if (model.getSpecies().isEmpty()) {
                feedback.addError("No species/organism specified");
            }
            if (model.getInstruments().isEmpty()) {
                feedback.addError("No instrument specified");
            }
            if (model.getProjectTitle() == null || model.getProjectTitle().trim().isEmpty()) {
                feedback.addError("Project title is missing");
            }
            if (model.getProjectDescription() == null || model.getProjectDescription().trim().isEmpty()) {
                feedback.addError("Project description is missing");
            }
            if (model.getLabHeadName() == null || model.getLabHeadName().trim().isEmpty()) {
                feedback.addError("Lab head information is missing");
            }

            // Success message if all OK
            if (!feedback.hasErrors() && !feedback.hasWarnings()) {
                feedback.setSuccess("All validation checks passed - ready for submission!");
            } else if (!feedback.hasErrors()) {
                feedback.addInfo("Submission can proceed with warnings");
            }
        }

        return feedback;
    }

    private VBox createSectionBox(String title) {
        return createSectionBox(title, null);
    }

    private VBox createSectionBox(String title, String iconText) {
        VBox box = new VBox(10);
        box.setStyle(
            "-fx-background-color: #f8f9fa; " +
            "-fx-border-color: #e0e0e0; " +
            "-fx-border-radius: 10; " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 18;");

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        if (iconText != null) {
            Label icon = new Label(iconText);
            icon.setStyle("-fx-font-size: 16px;");
            headerRow.getChildren().add(icon);
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #333;");
        headerRow.getChildren().add(titleLabel);

        // Subtle separator line under header
        Region separator = new Region();
        separator.setMaxHeight(1);
        separator.setStyle("-fx-background-color: #dee2e6;");
        VBox.setMargin(separator, new Insets(2, 0, 4, 0));

        box.getChildren().addAll(headerRow, separator);

        return box;
    }

    private void addSection(String title, String value) {
        VBox box = createSectionBox(title);
        Label valueLabel = new Label(value != null ? value : "-");
        valueLabel.setWrapText(true);
        valueLabel.setStyle("-fx-font-size: 13px;");
        box.getChildren().add(valueLabel);
        contentBox.getChildren().add(box);
    }

    private void addField(VBox container, String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-min-width: 160;");
        labelNode.setMinWidth(160);

        Label valueNode = new Label(truncate(value, 500));
        valueNode.setWrapText(true);
        valueNode.setStyle("-fx-text-fill: #222;");
        HBox.setHgrow(valueNode, Priority.ALWAYS);

        row.getChildren().addAll(labelNode, valueNode);
        container.getChildren().add(row);
    }

    private void addListField(VBox container, String label, java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            addField(container, label, "-");
            return;
        }
        addField(container, label, String.join(", ", values));
    }

    private void addFileSummary(VBox container) {
        int total = model.getFiles().size();
        if (total == 0) {
            addField(container, "Total Files", "0");
            return;
        }

        // File classification panel (category filter)
        FileClassificationPanel classPanel = new FileClassificationPanel();
        classPanel.setShowDetails(false);
        classPanel.setShowWarnings(false);
        classPanel.setFiles(model.getFiles());
        container.getChildren().add(classPanel);

        // Search field
        HBox searchBar = new HBox(8);
        searchBar.setAlignment(Pos.CENTER_RIGHT);

        Label searchIcon = new Label("\u2315");
        searchIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search files...");
        searchField.setPrefWidth(250);

        searchBar.getChildren().addAll(searchIcon, searchField);
        container.getChildren().add(searchBar);

        // File table
        TableView<DataFile> fileTable = new TableView<>();
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        fileTable.setMaxHeight(250);
        fileTable.setStyle("-fx-font-size: 12px;");

        TableColumn<DataFile, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(param ->
            new javafx.beans.property.SimpleStringProperty(param.getValue().getFileName()));
        nameCol.setPrefWidth(300);

        TableColumn<DataFile, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(param -> {
            ProjectFileType ft = param.getValue().getFileType();
            return new javafx.beans.property.SimpleStringProperty(
                ft != null ? FileTypeDetector.getDisplayName(ft) : "Other");
        });
        typeCol.setPrefWidth(150);

        TableColumn<DataFile, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(param -> {
            File f = param.getValue().getFile();
            return new javafx.beans.property.SimpleStringProperty(
                f != null ? formatSize(f.length()) : "-");
        });
        sizeCol.setPrefWidth(100);

        fileTable.getColumns().addAll(nameCol, typeCol, sizeCol);

        List<DataFile> allFiles = new ArrayList<>(model.getFiles());
        fileTable.getItems().addAll(allFiles);

        // Wire category filter
        final ProjectFileType[] activeFilter = {null};
        classPanel.setOnTypeSelected((type, files) -> {
            activeFilter[0] = type;
            filterSummaryFileTable(fileTable, allFiles, activeFilter[0], searchField.getText());
        });

        // Wire search filter
        searchField.textProperty().addListener((obs, oldVal, newVal) ->
            filterSummaryFileTable(fileTable, allFiles, activeFilter[0], newVal));

        container.getChildren().add(fileTable);
    }

    private void filterSummaryFileTable(TableView<DataFile> table, List<DataFile> allFiles,
                                         ProjectFileType typeFilter, String searchText) {
        String searchLower = (searchText != null) ? searchText.toLowerCase().trim() : "";
        table.getItems().clear();
        for (DataFile df : allFiles) {
            boolean matchesType = typeFilter == null || df.getFileType() == typeFilter;
            boolean matchesSearch = searchLower.isEmpty()
                    || df.getFileName().toLowerCase().contains(searchLower);
            if (matchesType && matchesSearch) {
                table.getItems().add(df);
            }
        }
    }

    private Label createChip(String text, String color) {
        Label chip = new Label(text);
        chip.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-padding: 4 10 4 10; " +
            "-fx-background-radius: 12; " +
            "-fx-font-size: 11px; " +
            "-fx-font-weight: bold;", color));
        return chip;
    }

    private void addResubmissionFileSummary(VBox container) {
        Resubmission resub = model.getResubmission();
        java.util.Map<DataFile, ResubmissionFileChangeState> resubMap = resub.getResubmission();

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

        addField(container, "New Files", String.valueOf(addCount));
        addField(container, "Modified Files", String.valueOf(modifyCount));
        addField(container, "Deleted Files", String.valueOf(deleteCount));
        addField(container, "Unchanged Files", String.valueOf(unchangedCount));

        // List individual file changes
        if (!resubMap.isEmpty()) {
            VBox fileList = new VBox(2);
            fileList.setPadding(new Insets(5, 0, 0, 10));

            for (java.util.Map.Entry<DataFile, ResubmissionFileChangeState> entry : resubMap.entrySet()) {
                ResubmissionFileChangeState state = entry.getValue();
                if (state == ResubmissionFileChangeState.NONE) continue;

                DataFile df = entry.getKey();
                String icon = switch (state) {
                    case ADD -> "+";
                    case MODIFY -> "~";
                    case DELETE -> "-";
                    default -> " ";
                };
                String color = switch (state) {
                    case ADD -> "#28a745";
                    case MODIFY -> "#cc7a00";
                    case DELETE -> "#dc3545";
                    default -> "#666";
                };

                Label fileLabel = new Label(icon + " " + df.getFileName() + " (" + state.name() + ")");
                fileLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-family: monospace;");
                fileList.getChildren().add(fileLabel);
            }

            if (!fileList.getChildren().isEmpty()) {
                container.getChildren().add(fileList);
            }
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "-";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    @Override
    public String getNextButtonText() {
        return "Next";
    }

    @Override
    public boolean validate() {
        // Skip recommended file checks for resubmission (existing project already has them)
        if (model.isResubmissionMode()) {
            return true;
        }

        // Check for missing recommended files
        List<String> missingRecommended = getMissingRecommendedFiles();

        if (!missingRecommended.isEmpty()) {
            // Show confirmation dialog
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Missing Recommended Files");
            alert.setHeaderText("Some recommended files are missing");

            StringBuilder content = new StringBuilder();
            content.append("The following recommended files were not found:\n\n");
            for (String item : missingRecommended) {
                content.append("• ").append(item).append("\n");
            }
            content.append("\nThese files are recommended for better data reuse and reproducibility.\n");
            content.append("Do you want to proceed without them?");

            alert.setContentText(content.toString());

            ButtonType proceedButton = new ButtonType("Proceed Anyway");
            ButtonType goBackButton = new ButtonType("Go Back", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(proceedButton, goBackButton);

            var result = alert.showAndWait();
            if (result.isEmpty() || result.get() == goBackButton) {
                return false; // User chose to go back
            }
        }

        // Checksum computation is now handled in the next step (ChecksumComputationStep)
        return true;
    }

    /**
     * Get list of missing recommended files
     */
    private List<String> getMissingRecommendedFiles() {
        List<String> missing = new ArrayList<>();

        // Check for FASTA database
        boolean hasFasta = model.getFiles().stream()
            .anyMatch(f -> FileTypeDetector.isFastaFile(f.getFile()));
        if (!hasFasta) {
            missing.add("FASTA database file - helps with protein sequence validation");
        }

        // Check for SDRF (experimental design)
        boolean hasSdrf = model.getFiles().stream()
            .anyMatch(f -> f.getFileType() == ProjectFileType.EXPERIMENTAL_DESIGN ||
                          (f.getFile() != null && f.getFile().getName().toLowerCase().contains("sdrf")));
        if (!hasSdrf) {
            missing.add("SDRF file - provides sample metadata for better data discovery");
        }

        return missing;
    }

    // ==================== Crosslinking Detection ====================

    private static final String CROSSLINK_ACCESSION = "PRIDE:0000430";

    private boolean isCrosslinkDataset() {
        // Check experiment type accession
        boolean hasCrosslinkType = model.getExperimentMethods().stream()
            .anyMatch(t -> CROSSLINK_ACCESSION.equals(t.getAccession()));
        if (hasCrosslinkType) return true;

        // Check text fields
        String[] fields = {
            model.getProjectTitle(),
            model.getKeywords(),
            model.getProjectDescription(),
            model.getSampleProcessingProtocol(),
            model.getDataProcessingProtocol()
        };
        for (String text : fields) {
            if (text != null) {
                String lower = text.toLowerCase();
                if (lower.contains("crosslink") || lower.contains("cross-link")) {
                    return true;
                }
            }
        }
        return false;
    }

    private VBox createCrosslinkBanner() {
        VBox banner = new VBox(8);
        banner.setPadding(new Insets(14));
        banner.setStyle(
            "-fx-background-color: #fff8e1; " +
            "-fx-border-color: #f9a825; " +
            "-fx-border-width: 0 0 0 4; " +
            "-fx-border-radius: 0 6 6 0; " +
            "-fx-background-radius: 0 6 6 0;");

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("\u26A0");
        icon.setStyle("-fx-font-size: 18px; -fx-text-fill: #f9a825;");

        Label title = new Label("Crosslinking Dataset Detected");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #6d4c00;");

        headerRow.getChildren().addAll(icon, title);

        Label message = new Label(
            "Please ensure your files and metadata meet the requirements " +
            "for the PRIDE Crosslinking Resource.");
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #6d4c00; -fx-font-size: 12px;");

        HBox linksRow = new HBox(16);
        linksRow.setPadding(new Insets(4, 0, 0, 0));

        Hyperlink guidelinesLink = new Hyperlink("Submission Guidelines");
        guidelinesLink.setStyle("-fx-text-fill: #0066cc; -fx-font-size: 12px;");
        guidelinesLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/markdownpage/crosslinking"));

        Hyperlink resourceLink = new Hyperlink("PRIDE Crosslinking Resource");
        resourceLink.setStyle("-fx-text-fill: #0066cc; -fx-font-size: 12px;");
        resourceLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/archive/crosslinking"));

        linksRow.getChildren().addAll(guidelinesLink, resourceLink);

        banner.getChildren().addAll(headerRow, message, linksRow);
        return banner;
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            LoggerFactory.getLogger(SummaryStep.class).error("Failed to open URL: {}", url, e);
        }
    }

    // ==================== Submission.px Writing ====================

    /**
     * Write submission.px file with resubmission data and tool metadata appended.
     * Matches the format used by the old Swing tool on master branch.
     */
    public static void writeSubmissionPxFile(SubmissionModel model, File file) throws Exception {
        // For resubmission, clear comments before writing (matches old tool behavior)
        if (model.isResubmissionMode()) {
            model.getSubmission().setComments(new java.util.ArrayList<>());
        }

        SubmissionFileWriter.write(model.getSubmission(), file);

        // Append lab head ORCID and country (not supported by Contact model, so we add as COM lines)
        appendLabHeadOrcid(file, model);
        appendLabHeadCountry(file, model);

        // Append resubmission file change summary as COM lines
        if (model.isResubmissionMode()) {
            appendResubmissionSummary(file, model);
        }

        // Append tool version and metadata
        appendToolMetadata(file);
    }

    /**
     * Append lab head ORCID to submission.px as a COM line.
     * The Contact model doesn't support ORCID, so we write it as metadata.
     */
    private static void appendLabHeadOrcid(File file, SubmissionModel model) {
        String orcid = model.getLabHeadOrcid();
        if (orcid != null && !orcid.trim().isEmpty()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                bw.write("COM\tlab_head_orcid\t" + orcid.trim());
                bw.newLine();
            } catch (IOException e) {
                LoggerFactory.getLogger(SummaryStep.class)
                    .error("Failed to append lab head ORCID to submission.px", e);
            }
        }
    }

    /**
     * Append lab head country to submission.px as a COM line.
     */
    private static void appendLabHeadCountry(File file, SubmissionModel model) {
        String country = model.getLabHeadCountry();
        if (country != null && !country.trim().isEmpty()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                bw.write("COM\tlab_head_country\t" + country.trim());
                bw.newLine();
            } catch (IOException e) {
                LoggerFactory.getLogger(SummaryStep.class)
                    .error("Failed to append lab head country to submission.px", e);
            }
        }
    }

    /**
     * Append resubmission file change states to submission.px.
     * Format: COM\tResubmission\tfilename\tfileType\tfileSize\tchangeState
     */
    private static void appendResubmissionSummary(File file, SubmissionModel model) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            Map<DataFile, ResubmissionFileChangeState> resubMap =
                model.getResubmission().getResubmission();
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
            LoggerFactory.getLogger(SummaryStep.class)
                .error("Failed to append resubmission summary to submission.px", e);
        }
    }

    /**
     * Append tool version and OS metadata to submission.px.
     */
    private static void appendToolMetadata(File file) {
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
            LoggerFactory.getLogger(SummaryStep.class)
                .error("Failed to append tool metadata to submission.px", e);
        }
    }
}
