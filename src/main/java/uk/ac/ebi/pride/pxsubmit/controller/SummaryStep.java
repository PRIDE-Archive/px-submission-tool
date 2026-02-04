package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.util.FileTypeDetector;
import uk.ac.ebi.pride.pxsubmit.view.component.ValidationFeedback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Step for reviewing submission summary before upload.
 * Displays all collected information for verification.
 */
public class SummaryStep extends AbstractWizardStep {

    private VBox contentBox;

    public SummaryStep(SubmissionModel model) {
        super("summary",
              "Review Submission",
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
        // Rebuild summary each time we enter
        buildSummary();
    }

    private void buildSummary() {
        contentBox.getChildren().clear();

        // Training mode warning (at top)
        if (model.isTrainingMode()) {
            Label warningLabel = new Label(
                "\u26A0 TRAINING MODE: This submission will not be processed. " +
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
        addSection("Submission Type", model.getSubmissionType() != null ?
            model.getSubmissionType().toString() : "Not specified");

        // Project Information
        VBox projectSection = createSectionBox("Project Information");
        addField(projectSection, "Title", model.getProjectTitle());
        addField(projectSection, "Description", model.getProjectDescription());
        addField(projectSection, "Keywords", model.getKeywords());
        contentBox.getChildren().add(projectSection);

        // Protocols
        VBox protocolSection = createSectionBox("Protocols");
        addField(protocolSection, "Sample Processing", model.getSampleProcessingProtocol());
        addField(protocolSection, "Data Processing", model.getDataProcessingProtocol());
        contentBox.getChildren().add(protocolSection);

        // Metadata
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

        // Lab Head
        if (model.getLabHeadName() != null && !model.getLabHeadName().isEmpty()) {
            VBox labHeadSection = createSectionBox("Lab Head");
            addField(labHeadSection, "Name", model.getLabHeadName());
            addField(labHeadSection, "Email", model.getLabHeadEmail());
            addField(labHeadSection, "Affiliation", model.getLabHeadAffiliation());
            contentBox.getChildren().add(labHeadSection);
        }

        // Files
        VBox filesSection = createSectionBox("Files");
        addFileSummary(filesSection);
        contentBox.getChildren().add(filesSection);

        // Upload Method
        if (model.getUploadMethod() != null) {
            addSection("Upload Method", model.getUploadMethod().getMethod());
        }
    }

    private ValidationFeedback createValidationSummary() {
        ValidationFeedback feedback = new ValidationFeedback();

        // Check files
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
        return "Submit";
    }

    @Override
    public boolean validate() {
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
}
