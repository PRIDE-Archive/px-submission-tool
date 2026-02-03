package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;

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

        // Submission type
        addSection("Submission Type", model.getSubmissionType().toString());

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

        // Training mode warning
        if (model.isTrainingMode()) {
            Label warningLabel = new Label(
                "TRAINING MODE: This submission will not be processed. " +
                "No files will be uploaded to the production server.");
            warningLabel.setStyle(
                "-fx-background-color: #fff3cd; " +
                "-fx-text-fill: #856404; " +
                "-fx-padding: 10; " +
                "-fx-background-radius: 5;");
            warningLabel.setWrapText(true);
            contentBox.getChildren().add(0, warningLabel);
        }
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

        if (rawCount > 0) addField(container, "Raw Files", String.valueOf(rawCount));
        if (resultCount > 0) addField(container, "Result Files", String.valueOf(resultCount));
        if (searchCount > 0) addField(container, "Search Files", String.valueOf(searchCount));
        if (peakCount > 0) addField(container, "Peak Files", String.valueOf(peakCount));
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
}
