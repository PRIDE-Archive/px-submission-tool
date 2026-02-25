package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Step for entering project references:
 * - PubMed IDs
 * - Links to other omics datasets (source + accession, multiple entries)
 *
 * All fields are optional.
 */
public class ProjectReferencesStep extends AbstractWizardStep {

    private static final Pattern PUBMED_PATTERN = Pattern.compile("^\\d[\\d, ]+\\d$");

    /** Omics data sources: display name -> submission.px prefix */
    private static final LinkedHashMap<String, String> OMICS_SOURCES = new LinkedHashMap<>();
    static {
        OMICS_SOURCES.put("PRIDE", "pride.project");
        OMICS_SOURCES.put("GEO", "geo");
        OMICS_SOURCES.put("ArrayExpress", "arrayexpress");
        OMICS_SOURCES.put("SRA", "insdc.sra");
        OMICS_SOURCES.put("BioProject", "bioproject");
        OMICS_SOURCES.put("ENA", "ena");
        OMICS_SOURCES.put("dbGaP", "dbgap");
        OMICS_SOURCES.put("MetaboLights", "metabolights");
        OMICS_SOURCES.put("ProteomeXchange", "px");
        OMICS_SOURCES.put("EGA Study", "ega.study");
        OMICS_SOURCES.put("EGA Dataset", "ega.dataset");
        OMICS_SOURCES.put("PeptideAtlas", "peptideatlas.dataset");
    }

    private TextField pubmedField;
    private Label pubmedValidationLabel;
    private VBox omicsEntriesContainer;
    private final List<OmicsEntryRow> omicsEntries = new ArrayList<>();

    public ProjectReferencesStep(SubmissionModel model) {
        super("project-references",
              "Reference",
              "Add publications and related dataset links",
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
            "Add any publications or links to related datasets. " +
            "All fields on this page are optional - skip if not applicable.");
        infoText.setWrapText(true);
        infoText.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        infoBox.getChildren().addAll(infoTitle, infoText);

        // PubMed IDs section
        VBox pubmedSection = createPubmedSection();

        // Omics links section
        VBox omicsSection = createOmicsLinksSection();

        content.getChildren().addAll(
            infoBox,
            pubmedSection,
            new Separator(),
            omicsSection
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

    private VBox createOmicsLinksSection() {
        VBox section = new VBox(8);

        Label titleLabel = new Label("Links to Other 'Omics' Datasets");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label descLabel = new Label(
            "Link your proteomics results to related datasets in other repositories. " +
            "Select the source and enter the accession/identifier.");
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        // Container for omics entry rows
        omicsEntriesContainer = new VBox(6);

        // Add button
        Button addButton = new Button("+ Add Link");
        addButton.setStyle("-fx-background-color: #0066cc; -fx-text-fill: white; " +
            "-fx-background-radius: 4; -fx-cursor: hand;");
        addButton.setOnAction(e -> addOmicsEntryRow(null, ""));

        section.getChildren().addAll(titleLabel, descLabel, omicsEntriesContainer, addButton);
        return section;
    }

    private void addOmicsEntryRow(String sourceKey, String accession) {
        OmicsEntryRow row = new OmicsEntryRow(sourceKey, accession);
        omicsEntries.add(row);
        omicsEntriesContainer.getChildren().add(row.getNode());
    }

    private void removeOmicsEntryRow(OmicsEntryRow row) {
        omicsEntries.remove(row);
        omicsEntriesContainer.getChildren().remove(row.getNode());
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

            // Other omics links - parse from stored format "source:accession,source:accession"
            if (meta.hasOtherOmicsLink()) {
                String link = meta.getOtherOmicsLink();
                // Clear existing rows
                omicsEntries.clear();
                omicsEntriesContainer.getChildren().clear();
                // Parse entries
                if (link != null && !link.trim().isEmpty()) {
                    String[] parts = link.split(",");
                    for (String part : parts) {
                        part = part.trim();
                        int colonIdx = part.indexOf(':');
                        if (colonIdx > 0) {
                            String sourceValue = part.substring(0, colonIdx);
                            String accession = part.substring(colonIdx + 1);
                            // Find display key for this source value
                            String displayKey = findDisplayKeyForValue(sourceValue);
                            addOmicsEntryRow(displayKey, accession);
                        }
                    }
                }
            }

        }

        // Test mode: pre-fill example data
        if (model.isTrainingMode()) {
            if (pubmedField.getText() == null || pubmedField.getText().isEmpty()) {
                pubmedField.setText("12345678");
            }
            if (omicsEntries.isEmpty()) {
                addOmicsEntryRow("GEO", "GSE123456");
            }
        }
    }

    /** Find the display key (e.g. "GEO") for a submission.px value (e.g. "geo") */
    private String findDisplayKeyForValue(String value) {
        for (Map.Entry<String, String> entry : OMICS_SOURCES.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
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

        // Build omics link string: "source:accession,source:accession"
        StringBuilder omicsBuilder = new StringBuilder();
        for (OmicsEntryRow row : omicsEntries) {
            String sourceKey = row.getSelectedSourceKey();
            String accession = row.getAccession();
            if (sourceKey != null && !accession.isEmpty()) {
                String sourceValue = OMICS_SOURCES.get(sourceKey);
                if (sourceValue != null) {
                    if (omicsBuilder.length() > 0) {
                        omicsBuilder.append(",");
                    }
                    omicsBuilder.append(sourceValue).append(":").append(accession.trim());
                }
            }
        }
        meta.setOtherOmicsLink(omicsBuilder.length() > 0 ? omicsBuilder.toString() : null);

        logger.info("Saved references: {} PubMed IDs, {} omics links",
            meta.getNumberOfPubmedIds(),
            omicsEntries.stream().filter(r -> r.getSelectedSourceKey() != null && !r.getAccession().isEmpty()).count());
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

        // Validate omics entries - each must have both source and accession if partially filled
        for (OmicsEntryRow row : omicsEntries) {
            String sourceKey = row.getSelectedSourceKey();
            String accession = row.getAccession();
            if (sourceKey != null && accession.isEmpty()) {
                showError("Please enter an accession for the selected source '" + sourceKey + "', or remove the entry.");
                return false;
            }
            if (sourceKey == null && !accession.isEmpty()) {
                showError("Please select a source for the accession '" + accession + "', or remove the entry.");
                return false;
            }
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

    /**
     * Inner class representing a single omics link entry row (source dropdown + accession field + remove button).
     */
    private class OmicsEntryRow {
        private final HBox container;
        private final ComboBox<String> sourceCombo;
        private final TextField accessionField;

        OmicsEntryRow(String sourceKey, String accession) {
            container = new HBox(8);
            container.setAlignment(Pos.CENTER_LEFT);

            sourceCombo = new ComboBox<>();
            sourceCombo.getItems().addAll(OMICS_SOURCES.keySet());
            sourceCombo.setPromptText("Select source...");
            sourceCombo.setPrefWidth(180);
            if (sourceKey != null) {
                sourceCombo.setValue(sourceKey);
            }

            accessionField = new TextField(accession != null ? accession : "");
            accessionField.setPromptText("Accession / Identifier");
            accessionField.setPrefWidth(250);

            Button removeBtn = new Button("\u2715");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc3545; " +
                "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6;");
            removeBtn.setTooltip(new Tooltip("Remove this entry"));
            removeBtn.setOnAction(e -> removeOmicsEntryRow(this));

            container.getChildren().addAll(sourceCombo, accessionField, removeBtn);
        }

        HBox getNode() {
            return container;
        }

        String getSelectedSourceKey() {
            return sourceCombo.getValue();
        }

        String getAccession() {
            String text = accessionField.getText();
            return text != null ? text.trim() : "";
        }
    }
}
