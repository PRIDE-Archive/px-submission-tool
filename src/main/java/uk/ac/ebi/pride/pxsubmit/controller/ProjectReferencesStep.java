package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Step for entering project references:
 * - Project Tags (from cv/projecttag.cv)
 * - PubMed IDs
 * - Reanalysis PX Accessions
 * - Links to other omics datasets
 *
 * All fields are optional.
 */
public class ProjectReferencesStep extends AbstractWizardStep {

    private static final Pattern PUBMED_PATTERN = Pattern.compile("^\\d[\\d, ]+\\d$");
    private static final Pattern PX_ACCESSION_PATTERN = Pattern.compile("^(PXD\\d{6}[, ]*)+$");

    private TextField pubmedField;
    private TextField reanalysisField;
    private TextField omicsLinkField;
    private VBox tagCheckboxContainer;
    private final List<CheckBox> tagCheckboxes = new ArrayList<>();
    private Label pubmedValidationLabel;
    private Label reanalysisValidationLabel;

    public ProjectReferencesStep(SubmissionModel model) {
        super("project-references",
              "Project References",
              "Add publications, project tags, and related dataset links",
              model);
    }

    @Override
    public boolean canSkip() {
        return model.isResubmissionMode();
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Info box
        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(12));
        infoBox.setStyle(
            "-fx-background-color: #e7f3ff; " +
            "-fx-border-color: #0066cc; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;");

        Label infoTitle = new Label("Project References (Optional)");
        infoTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #0066cc;");

        Label infoText = new Label(
            "Add any publications, project affiliations, or links to related datasets. " +
            "All fields on this page are optional - skip if not applicable.");
        infoText.setWrapText(true);
        infoText.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        infoBox.getChildren().addAll(infoTitle, infoText);

        // PubMed IDs section
        VBox pubmedSection = createPubmedSection();

        // Reanalysis section
        VBox reanalysisSection = createReanalysisSection();

        // Other omics link section
        VBox omicsSection = createOmicsLinkSection();

        // Project Tags section
        VBox tagsSection = createProjectTagsSection();

        content.getChildren().addAll(
            infoBox,
            pubmedSection,
            new Separator(),
            reanalysisSection,
            new Separator(),
            omicsSection,
            new Separator(),
            tagsSection
        );

        scrollPane.setContent(content);
        return scrollPane;
    }

    private VBox createPubmedSection() {
        VBox section = new VBox(8);

        Label titleLabel = new Label("PubMed ID(s)");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label descLabel = new Label(
            "If your dataset is associated with a publication, enter the PubMed ID(s) separated by commas.");
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        pubmedField = new TextField();
        pubmedField.setPromptText("e.g. 12345678, 23456789");
        pubmedField.setPrefWidth(400);

        pubmedValidationLabel = new Label();
        pubmedValidationLabel.setStyle("-fx-font-size: 11px;");
        pubmedValidationLabel.setVisible(false);
        pubmedValidationLabel.setManaged(false);

        section.getChildren().addAll(titleLabel, descLabel, pubmedField, pubmedValidationLabel);
        return section;
    }

    private VBox createReanalysisSection() {
        VBox section = new VBox(8);

        Label titleLabel = new Label("Reanalysis ProteomeXchange Accession(s)");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label descLabel = new Label(
            "Only applicable if your results are based on the reprocessing of previously submitted PX dataset(s). " +
            "Enter PX accessions separated by commas.");
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        reanalysisField = new TextField();
        reanalysisField.setPromptText("e.g. PXD012345, PXD067890");
        reanalysisField.setPrefWidth(400);

        reanalysisValidationLabel = new Label();
        reanalysisValidationLabel.setStyle("-fx-font-size: 11px;");
        reanalysisValidationLabel.setVisible(false);
        reanalysisValidationLabel.setManaged(false);

        section.getChildren().addAll(titleLabel, descLabel, reanalysisField, reanalysisValidationLabel);
        return section;
    }

    private VBox createOmicsLinkSection() {
        VBox section = new VBox(8);

        Label titleLabel = new Label("Links to Other 'Omics' Datasets");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label descLabel = new Label(
            "Only applicable if proteomics results can be linked to other biological data " +
            "submitted to other resources (e.g. ArrayExpress, GEO).");
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        omicsLinkField = new TextField();
        omicsLinkField.setPromptText("e.g. https://www.ebi.ac.uk/arrayexpress/experiments/E-MTAB-1234");
        omicsLinkField.setPrefWidth(500);

        section.getChildren().addAll(titleLabel, descLabel, omicsLinkField);
        return section;
    }

    private VBox createProjectTagsSection() {
        VBox section = new VBox(8);

        Label titleLabel = new Label("Project Tags");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label descLabel = new Label(
            "Select any applicable project affiliations for your dataset.");
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        tagCheckboxContainer = new VBox(4);
        tagCheckboxContainer.setPadding(new Insets(5, 0, 0, 10));

        // Load project tags from CV file
        List<String> tags = loadProjectTags();
        for (String tag : tags) {
            CheckBox cb = new CheckBox(tag);
            cb.setStyle("-fx-font-size: 12px;");
            tagCheckboxes.add(cb);
            tagCheckboxContainer.getChildren().add(cb);
        }

        // Wrap in a scroll pane if many tags
        ScrollPane tagScroll = new ScrollPane(tagCheckboxContainer);
        tagScroll.setFitToWidth(true);
        tagScroll.setPrefHeight(200);
        tagScroll.setMaxHeight(200);
        tagScroll.setStyle("-fx-background-color: transparent; -fx-border-color: #e0e0e0; " +
            "-fx-border-radius: 4; -fx-background-radius: 4;");

        section.getChildren().addAll(titleLabel, descLabel, tagScroll);
        return section;
    }

    private List<String> loadProjectTags() {
        List<String> tags = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/cv/projecttag.cv")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            tags.add(line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load project tags from CV file", e);
        }
        return tags;
    }

    @Override
    protected void initializeStep() {
        // Always valid - all fields are optional
        valid.set(true);

        // Live PubMed validation
        pubmedField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                if (PUBMED_PATTERN.matcher(newVal.trim()).matches()) {
                    pubmedField.setStyle("");
                    pubmedValidationLabel.setText("\u2714 Valid PubMed ID(s)");
                    pubmedValidationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #28a745;");
                    pubmedValidationLabel.setVisible(true);
                    pubmedValidationLabel.setManaged(true);
                } else {
                    pubmedField.setStyle("-fx-border-color: #dc3545;");
                    pubmedValidationLabel.setText("PubMed IDs should be numeric, separated by commas");
                    pubmedValidationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc3545;");
                    pubmedValidationLabel.setVisible(true);
                    pubmedValidationLabel.setManaged(true);
                }
            } else {
                pubmedField.setStyle("");
                pubmedValidationLabel.setVisible(false);
                pubmedValidationLabel.setManaged(false);
            }
        });

        // Live reanalysis validation
        reanalysisField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                if (PX_ACCESSION_PATTERN.matcher(newVal.trim()).matches()) {
                    reanalysisField.setStyle("");
                    reanalysisValidationLabel.setText("\u2714 Valid PX accession(s)");
                    reanalysisValidationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #28a745;");
                    reanalysisValidationLabel.setVisible(true);
                    reanalysisValidationLabel.setManaged(true);
                } else {
                    reanalysisField.setStyle("-fx-border-color: #dc3545;");
                    reanalysisValidationLabel.setText("Use format: PXD000000 (separated by commas)");
                    reanalysisValidationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc3545;");
                    reanalysisValidationLabel.setVisible(true);
                    reanalysisValidationLabel.setManaged(true);
                }
            } else {
                reanalysisField.setStyle("");
                reanalysisValidationLabel.setVisible(false);
                reanalysisValidationLabel.setManaged(false);
            }
        });
    }

    @Override
    protected void onStepEntering() {
        // Load existing values from model's ProjectMetaData
        var meta = model.getSubmission().getProjectMetaData();
        if (meta != null) {
            // PubMed IDs
            if (meta.hasPubmedIds()) {
                pubmedField.setText(String.join(", ", meta.getPubmedIds()));
            }

            // Reanalysis accessions
            if (meta.hasReanalysisPxAccessions()) {
                reanalysisField.setText(String.join(", ", meta.getReanalysisAccessions()));
            }

            // Other omics link
            if (meta.hasOtherOmicsLink()) {
                omicsLinkField.setText(meta.getOtherOmicsLink());
            }

            // Project tags
            Set<String> selectedTags = meta.getProjectTags();
            if (selectedTags != null) {
                for (CheckBox cb : tagCheckboxes) {
                    cb.setSelected(selectedTags.contains(cb.getText()));
                }
            }
        }

        // Test mode: pre-fill example data
        if (model.isTrainingMode()) {
            if (pubmedField.getText() == null || pubmedField.getText().isEmpty()) {
                pubmedField.setText("12345678");
            }
        }
    }

    @Override
    protected void onStepLeaving() {
        saveToModel();
    }

    private void saveToModel() {
        var meta = model.getSubmission().getProjectMetaData();
        if (meta == null) return;

        // PubMed IDs
        meta.clearPubmedIds();
        String pubmedText = pubmedField.getText();
        if (pubmedText != null && !pubmedText.trim().isEmpty()) {
            String[] ids = pubmedText.trim().split("[,\\s]+");
            List<String> validIds = Arrays.stream(ids)
                .map(String::trim)
                .filter(s -> !s.isEmpty() && s.matches("\\d+"))
                .toList();
            if (!validIds.isEmpty()) {
                meta.addPubmedIds(validIds.toArray(new String[0]));
            }
        }

        // Reanalysis accessions
        meta.clearReanalysisPxAccessions();
        String reanalysisText = reanalysisField.getText();
        if (reanalysisText != null && !reanalysisText.trim().isEmpty()) {
            String[] accessions = reanalysisText.trim().split("[,\\s]+");
            List<String> validAccessions = Arrays.stream(accessions)
                .map(String::trim)
                .filter(s -> s.matches("PXD\\d{6}"))
                .toList();
            if (!validAccessions.isEmpty()) {
                meta.addReanalysisPxAccessions(validAccessions.toArray(new String[0]));
            }
        }

        // Other omics link
        String omicsLink = omicsLinkField.getText();
        meta.setOtherOmicsLink(omicsLink != null && !omicsLink.trim().isEmpty() ? omicsLink.trim() : null);

        // Project tags
        meta.clearProjectTags();
        List<String> selectedTags = tagCheckboxes.stream()
            .filter(CheckBox::isSelected)
            .map(CheckBox::getText)
            .toList();
        if (!selectedTags.isEmpty()) {
            meta.addProjectTags(selectedTags.toArray(new String[0]));
        }

        logger.info("Saved references: {} PubMed IDs, {} reanalysis accessions, {} project tags",
            meta.getNumberOfPubmedIds(),
            meta.getNumberOfReanalysisPxAccessions(),
            meta.getProjectTags().size());
    }

    @Override
    public boolean validate() {
        // Validate PubMed IDs if entered
        String pubmedText = pubmedField.getText();
        if (pubmedText != null && !pubmedText.trim().isEmpty()) {
            if (!PUBMED_PATTERN.matcher(pubmedText.trim()).matches()) {
                showError("Please enter valid PubMed IDs (numeric, separated by commas)");
                pubmedField.requestFocus();
                return false;
            }
        }

        // Validate reanalysis accessions if entered
        String reanalysisText = reanalysisField.getText();
        if (reanalysisText != null && !reanalysisText.trim().isEmpty()) {
            if (!PX_ACCESSION_PATTERN.matcher(reanalysisText.trim()).matches()) {
                showError("Please enter valid PX accessions in format PXD000000 (separated by commas)");
                reanalysisField.requestFocus();
                return false;
            }
        }

        // Validate omics link length
        String omicsLink = omicsLinkField.getText();
        if (omicsLink != null && omicsLink.length() > 500) {
            showError("Links to other omics datasets must be less than 500 characters");
            omicsLinkField.requestFocus();
            return false;
        }

        // Save to model before leaving
        saveToModel();
        return true;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
