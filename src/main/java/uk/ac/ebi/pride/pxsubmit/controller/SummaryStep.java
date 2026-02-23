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
import uk.ac.ebi.pride.pxsubmit.view.component.ValidationFeedback;

import org.slf4j.LoggerFactory;

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

        // Validation Summary
        ValidationFeedback validation = createValidationSummary();
        contentBox.getChildren().add(validation);

        // Submission type
        String typeDisplay = model.isResubmissionMode() ? "Resubmission" :
            (model.getSubmissionType() != null ? model.getSubmissionType().toString() : "Not specified");
        addSection("Submission Type", typeDisplay);

        // Project Information, Protocols, Metadata, Lab Head — skip for resubmission
        // (metadata is already on the server for the existing project)
        if (!model.isResubmissionMode()) {
            VBox projectSection = createSectionBox("Project Information");
            addField(projectSection, "Title", model.getProjectTitle());
            addField(projectSection, "Description", model.getProjectDescription());
            addField(projectSection, "Keywords", model.getKeywords());
            contentBox.getChildren().add(projectSection);

            VBox protocolSection = createSectionBox("Protocols");
            addField(protocolSection, "Sample Processing", model.getSampleProcessingProtocol());
            addField(protocolSection, "Data Processing", model.getDataProcessingProtocol());
            contentBox.getChildren().add(protocolSection);

            VBox metadataSection = createSectionBox("Metadata");
            addListField(metadataSection, "Species",
                model.getSpecies().stream().map(CvParam::getName).collect(Collectors.toList()));
            addListField(metadataSection, "Instruments",
                model.getInstruments().stream().map(CvParam::getName).collect(Collectors.toList()));
            addListField(metadataSection, "Modifications",
                model.getModifications().stream().map(CvParam::getName).collect(Collectors.toList()));
            addListField(metadataSection, "Quantification",
                model.getQuantifications().stream().map(CvParam::getName).collect(Collectors.toList()));
            contentBox.getChildren().add(metadataSection);

            if (model.getLabHeadName() != null && !model.getLabHeadName().isEmpty()) {
                VBox labHeadSection = createSectionBox("Lab Head");
                addField(labHeadSection, "Name", model.getLabHeadName());
                addField(labHeadSection, "Email", model.getLabHeadEmail());
                addField(labHeadSection, "Affiliation", model.getLabHeadAffiliation());
                contentBox.getChildren().add(labHeadSection);
            }
        }

        // Resubmission file changes (only in resubmission mode)
        if (model.isResubmissionMode()) {
            VBox resubFilesSection = createSectionBox("Resubmission File Changes");
            addResubmissionFileSummary(resubFilesSection);
            contentBox.getChildren().add(resubFilesSection);
        }

        // Files
        VBox filesSection = createSectionBox("Files");
        addFileSummary(filesSection);
        contentBox.getChildren().add(filesSection);

        // Export submission.px section
        VBox exportSection = createSectionBox("Export Submission");

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
        fileChooser.setInitialFileName("submission.px");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PX Submission Files", "*.px")
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
        VBox box = new VBox(8);
        box.setStyle(
            "-fx-background-color: #f8f9fa; " +
            "-fx-border-color: #e9ecef; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 15;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        box.getChildren().add(titleLabel);

        return box;
    }

    private void addSection(String title, String value) {
        VBox box = createSectionBox(title);
        Label valueLabel = new Label(value != null ? value : "-");
        valueLabel.setWrapText(true);
        box.getChildren().add(valueLabel);
        contentBox.getChildren().add(box);
    }

    private void addField(VBox container, String label, String value) {
        HBox row = new HBox(10);

        Label labelNode = new Label(label + ":");
        labelNode.setStyle("-fx-font-weight: bold; -fx-min-width: 150;");
        labelNode.setMinWidth(150);

        Label valueNode = new Label(truncate(value, 500));
        valueNode.setWrapText(true);
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

        // Total size
        long totalSize = model.getFiles().stream()
            .filter(f -> f.getFile() != null)
            .mapToLong(f -> f.getFile().length())
            .sum();

        addField(container, "Total Files", String.valueOf(total));
        addField(container, "Total Size", formatSize(totalSize));

        if (rawCount > 0) addField(container, "RAW Files", String.valueOf(rawCount));
        if (resultCount > 0) addField(container, "STANDARD Files", String.valueOf(resultCount));
        if (searchCount > 0) addField(container, "ANALYSIS Files", String.valueOf(searchCount));
        if (peakCount > 0) addField(container, "Peak Lists", String.valueOf(peakCount));
        if (otherCount > 0) addField(container, "Other Files", String.valueOf(otherCount));
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

        // Append resubmission file change summary as COM lines
        if (model.isResubmissionMode()) {
            appendResubmissionSummary(file, model);
        }

        // Append tool version and metadata
        appendToolMetadata(file);
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
