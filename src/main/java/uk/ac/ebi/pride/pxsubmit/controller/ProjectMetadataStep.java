package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.OlsService;

import java.util.List;
import uk.ac.ebi.pride.pxsubmit.service.OlsService.OlsOntology;
import uk.ac.ebi.pride.pxsubmit.view.component.ChipInput;
import uk.ac.ebi.pride.pxsubmit.view.component.OlsAutocomplete;
import uk.ac.ebi.pride.pxsubmit.view.component.ValidationFeedback;

/**
 * Step for entering project metadata.
 * Collects: title, description, keywords, sample/data processing protocols.
 */
public class ProjectMetadataStep extends AbstractWizardStep {

    // Form fields
    private TextField titleField;
    private TextArea descriptionArea;
    private ChipInput keywordsInput;
    private TextArea sampleProtocolArea;
    private TextArea dataProtocolArea;
    private OlsAutocomplete experimentTypeField;

    // Character counters
    private Label titleCounter;
    private Label descriptionCounter;

    // Validation feedback
    private ValidationFeedback validationFeedback;

    // Validation
    private static final int MIN_TITLE_LENGTH = 5;
    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MIN_DESCRIPTION_LENGTH = 20;
    private static final int MAX_DESCRIPTION_LENGTH = 5000;

    public ProjectMetadataStep(SubmissionModel model) {
        super("project-metadata",
              "Project Information",
              "Enter the details about your submission",
              model);
    }

    @Override
    public boolean canSkip() {
        // Skip this step during resubmission - project metadata already exists on the server
        return model.isResubmissionMode();
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox form = new VBox(20);
        form.setPadding(new Insets(20));

        // Project Title
        VBox titleSection = createFieldSection("Project Title",
            "A short descriptive title for your project (5-500 characters)", true);
        titleField = new TextField();
        titleField.setPromptText("e.g., Proteome analysis of human liver cancer cells");
        Tooltip titleTooltip = new Tooltip(
            "Enter a descriptive title for your project.\n" +
            "This will be displayed in PRIDE Archive.\n" +
            "Minimum 5 characters, maximum 500.");
        titleTooltip.setShowDelay(Duration.millis(300));
        titleField.setTooltip(titleTooltip);
        titleCounter = new Label("0 / " + MAX_TITLE_LENGTH);
        titleCounter.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        HBox titleBox = new HBox(10);
        HBox.setHgrow(titleField, Priority.ALWAYS);
        titleBox.getChildren().addAll(titleField, titleCounter);
        titleSection.getChildren().add(titleBox);

        // Project Description
        VBox descSection = createFieldSection("Project Description",
            "Detailed description of your project (20-5000 characters)", true);
        descriptionArea = new TextArea();
        descriptionArea.setPromptText(
            "Describe your project, including:\n" +
            "• Scientific background and aims\n" +
            "• Experimental approach\n" +
            "• Key findings (if applicable)");
        descriptionArea.setPrefRowCount(6);
        descriptionArea.setWrapText(true);
        Tooltip descTooltip = new Tooltip(
            "Provide a detailed description of your project.\n" +
            "Include scientific background, aims, and key findings.\n" +
            "Minimum 20 characters, maximum 5000.");
        descTooltip.setShowDelay(Duration.millis(300));
        descriptionArea.setTooltip(descTooltip);
        descriptionCounter = new Label("0 / " + MAX_DESCRIPTION_LENGTH);
        descriptionCounter.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        descSection.getChildren().addAll(descriptionArea, descriptionCounter);

        // Keywords
        VBox keywordsSection = createFieldSection("Keywords",
            "Type a keyword and press Enter to add it. Click \u2715 to remove.", true);
        keywordsInput = new ChipInput();
        keywordsInput.setPromptText("Type keyword and press Enter...");
        Tooltip keywordsTooltip = new Tooltip(
            "Add keywords that describe your dataset.\n" +
            "Type a word and press Enter to add it as a tag.\n" +
            "Click X on a tag to remove it.");
        keywordsTooltip.setShowDelay(Duration.millis(300));
        Tooltip.install(keywordsInput, keywordsTooltip);
        Label keywordsHint = new Label("Examples: liver cancer, proteomics, mass spectrometry, biomarker");
        keywordsHint.setStyle("-fx-text-fill: #999; -fx-font-size: 11px; -fx-font-style: italic;");
        keywordsSection.getChildren().addAll(keywordsInput, keywordsHint);

        // Experiment Type
        VBox experimentTypeSection = createFieldSection("Experiment Type",
            "Select the type of mass spectrometry experiment", true);
        experimentTypeField = new OlsAutocomplete(OlsOntology.PRIDE);
        experimentTypeField.setPromptText("Search experiment type (e.g., Bottom-up, DIA, DDA)...");
        experimentTypeField.setCommonTerms(OlsService.getCommonExperimentTypes());
        experimentTypeField.setOnTermSelected(term -> model.addExperimentMethod(term));
        experimentTypeField.setOnTermRemoved(term -> model.removeExperimentMethod(term));
        experimentTypeSection.getChildren().add(experimentTypeField);
        experimentTypeSection.getChildren().add(createQuickSelectPane(OlsService.getCommonExperimentTypes(), experimentTypeField));

        // Sample Processing Protocol
        VBox sampleSection = createFieldSection("Sample Processing Protocol",
            "Describe how samples were prepared for analysis", true);
        sampleProtocolArea = new TextArea();
        sampleProtocolArea.setPromptText(
            "Describe sample processing steps:\n" +
            "• Sample collection and storage\n" +
            "• Protein extraction method\n" +
            "• Digestion protocol\n" +
            "• Fractionation (if applicable)");
        sampleProtocolArea.setPrefRowCount(4);
        sampleProtocolArea.setWrapText(true);
        Tooltip sampleTooltip = new Tooltip(
            "Describe your sample preparation protocol.\n" +
            "Include extraction methods, digestion, and fractionation.");
        sampleTooltip.setShowDelay(Duration.millis(300));
        sampleProtocolArea.setTooltip(sampleTooltip);
        sampleSection.getChildren().add(sampleProtocolArea);

        // Data Processing Protocol
        VBox dataSection = createFieldSection("Data Processing Protocol",
            "Describe how data was analyzed", true);
        dataProtocolArea = new TextArea();
        dataProtocolArea.setPromptText(
            "Describe data processing:\n" +
            "• Search engine and version\n" +
            "• Database used\n" +
            "• Search parameters\n" +
            "• Post-processing/validation");
        dataProtocolArea.setPrefRowCount(4);
        dataProtocolArea.setWrapText(true);
        Tooltip dataTooltip = new Tooltip(
            "Describe your data analysis workflow.\n" +
            "Include search engine, database, and parameters used.");
        dataTooltip.setShowDelay(Duration.millis(300));
        dataProtocolArea.setTooltip(dataTooltip);
        dataSection.getChildren().add(dataProtocolArea);

        // Validation feedback
        validationFeedback = new ValidationFeedback();

        form.getChildren().addAll(
            titleSection,
            descSection,
            keywordsSection,
            sampleSection,
            dataSection,
            experimentTypeSection,
            validationFeedback
        );

        scrollPane.setContent(form);
        return scrollPane;
    }

    private VBox createSection(String title, String description) {
        VBox section = new VBox(5);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        section.getChildren().addAll(titleLabel, descLabel);
        return section;
    }

    private VBox createFieldSection(String title, String description, boolean required) {
        VBox section = new VBox(5);

        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        if (required) {
            Label reqLabel = new Label("REQUIRED");
            reqLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: white; -fx-background-color: #dc3545; -fx-padding: 2 4; -fx-background-radius: 4;");
            titleBox.getChildren().addAll(titleLabel, reqLabel);
        } else {
            titleBox.getChildren().add(titleLabel);
        }

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        section.getChildren().addAll(titleBox, descLabel);
        return section;
    }

    /**
     * Creates a pane with clickable quick-select buttons for common terms.
     */
    private FlowPane createQuickSelectPane(List<CvParam> terms, OlsAutocomplete targetField) {
        FlowPane pane = new FlowPane();
        pane.setHgap(6);
        pane.setVgap(4);
        pane.setPadding(new Insets(4, 0, 0, 0));

        Label label = new Label("Examples:");
        label.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        pane.getChildren().add(label);

        for (CvParam term : terms) {
            Label chip = new Label(term.getName());
            chip.setStyle(
                "-fx-font-size: 11px; " +
                "-fx-padding: 2 8; " +
                "-fx-background-color: #e9ecef; " +
                "-fx-background-radius: 10; " +
                "-fx-cursor: hand;");
            chip.setOnMouseEntered(e -> chip.setStyle(
                "-fx-font-size: 11px; " +
                "-fx-padding: 2 8; " +
                "-fx-background-color: #0066cc; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 10; " +
                "-fx-cursor: hand;"));
            chip.setOnMouseExited(e -> chip.setStyle(
                "-fx-font-size: 11px; " +
                "-fx-padding: 2 8; " +
                "-fx-background-color: #e9ecef; " +
                "-fx-background-radius: 10; " +
                "-fx-cursor: hand;"));
            chip.setOnMouseClicked(e -> {
                if (!targetField.getSelectedTerms().contains(term)) {
                    targetField.addTerm(term);
                }
            });
            pane.getChildren().add(chip);
        }

        return pane;
    }

    @Override
    protected void initializeStep() {
        // Bidirectional bindings to model
        titleField.textProperty().bindBidirectional(model.projectTitleProperty());
        descriptionArea.textProperty().bindBidirectional(model.projectDescriptionProperty());
        keywordsInput.textProperty().bindBidirectional(model.keywordsProperty());
        sampleProtocolArea.textProperty().bindBidirectional(model.sampleProcessingProtocolProperty());
        dataProtocolArea.textProperty().bindBidirectional(model.dataProcessingProtocolProperty());

        // Character counters
        titleField.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            titleCounter.setText(len + " / " + MAX_TITLE_LENGTH);
            if (len > MAX_TITLE_LENGTH) {
                titleCounter.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px;");
            } else if (len >= MIN_TITLE_LENGTH) {
                titleCounter.setStyle("-fx-text-fill: #28a745; -fx-font-size: 11px;");
            } else {
                titleCounter.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            }
            updateValidationFeedback();
        });

        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            descriptionCounter.setText(len + " / " + MAX_DESCRIPTION_LENGTH);
            if (len > MAX_DESCRIPTION_LENGTH) {
                descriptionCounter.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px;");
            } else if (len >= MIN_DESCRIPTION_LENGTH) {
                descriptionCounter.setStyle("-fx-text-fill: #28a745; -fx-font-size: 11px;");
            } else {
                descriptionCounter.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            }
            updateValidationFeedback();
        });

        // Update validation feedback when other fields change
        keywordsInput.textProperty().addListener((obs, oldVal, newVal) -> updateValidationFeedback());
        sampleProtocolArea.textProperty().addListener((obs, oldVal, newVal) -> updateValidationFeedback());
        dataProtocolArea.textProperty().addListener((obs, oldVal, newVal) -> updateValidationFeedback());
        experimentTypeField.getSelectedTerms().addListener((javafx.collections.ListChangeListener.Change<? extends CvParam> c) -> updateValidationFeedback());

        // Load existing experiment methods from model
        if (!model.getExperimentMethods().isEmpty()) {
            experimentTypeField.setSelectedTerms(model.getExperimentMethods());
        }

        // Validation binding
        valid.bind(Bindings.createBooleanBinding(() -> {
            String title = titleField.getText();
            String desc = descriptionArea.getText();
            String keywords = keywordsInput.getText();
            String sampleProtocol = sampleProtocolArea.getText();
            String dataProtocol = dataProtocolArea.getText();
            boolean hasExperimentType = experimentTypeField.hasSelection();

            return isValidTitle(title) &&
                   isValidDescription(desc) &&
                   isNotEmpty(keywords) &&
                   hasExperimentType &&
                   isNotEmpty(sampleProtocol) &&
                   isNotEmpty(dataProtocol);
        },
        titleField.textProperty(),
        descriptionArea.textProperty(),
        keywordsInput.textProperty(),
        experimentTypeField.getSelectedTerms(),
        sampleProtocolArea.textProperty(),
        dataProtocolArea.textProperty()));
    }

    @Override
    public boolean validate() {
        // Check each field and show specific error
        String title = titleField.getText();
        if (title == null || title.trim().length() < MIN_TITLE_LENGTH) {
            showError("Project title must be at least " + MIN_TITLE_LENGTH + " characters");
            titleField.requestFocus();
            return false;
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            showError("Project title cannot exceed " + MAX_TITLE_LENGTH + " characters");
            titleField.requestFocus();
            return false;
        }

        String desc = descriptionArea.getText();
        if (desc == null || desc.trim().length() < MIN_DESCRIPTION_LENGTH) {
            showError("Project description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
            descriptionArea.requestFocus();
            return false;
        }
        if (desc.length() > MAX_DESCRIPTION_LENGTH) {
            showError("Project description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
            descriptionArea.requestFocus();
            return false;
        }

        if (keywordsInput.isEmpty()) {
            showError("Please enter at least one keyword");
            keywordsInput.requestFocus();
            return false;
        }

        if (!experimentTypeField.hasSelection()) {
            showError("Please select at least one experiment type");
            experimentTypeField.requestFocus();
            return false;
        }

        if (isNullOrEmpty(sampleProtocolArea.getText())) {
            showError("Please describe the sample processing protocol");
            sampleProtocolArea.requestFocus();
            return false;
        }

        if (isNullOrEmpty(dataProtocolArea.getText())) {
            showError("Please describe the data processing protocol");
            dataProtocolArea.requestFocus();
            return false;
        }

        // Sync to model
        model.syncMetadataToSubmission();
        return true;
    }

    @Override
    protected void onStepEntering() {
        // In test mode, pre-fill with example data if fields are empty
        if (model.isTrainingMode()) {
            populateTestModeExamples();
        }
        titleField.requestFocus();
    }

    /**
     * Populate fields with example data for test mode
     */
    private void populateTestModeExamples() {
        // Only populate if fields are empty (don't overwrite user data)
        if (isNullOrEmpty(titleField.getText())) {
            titleField.setText("Quantitative proteomics analysis of human liver cancer cells treated with sorafenib");
        }

        if (isNullOrEmpty(descriptionArea.getText())) {
            descriptionArea.setText(
                "This study investigates the proteomic changes in hepatocellular carcinoma (HCC) cell lines " +
                "following treatment with sorafenib, a multi-kinase inhibitor used in cancer therapy. " +
                "HepG2 cells were cultured and treated with 10 μM sorafenib for 24 hours. " +
                "Control and treated samples were collected in biological triplicates. " +
                "Proteins were extracted, digested with trypsin, and analyzed by LC-MS/MS. " +
                "The data reveals significant changes in signaling pathways related to cell survival and apoptosis."
            );
        }

        if (isNullOrEmpty(keywordsInput.getText())) {
            keywordsInput.setText("liver cancer, hepatocellular carcinoma, sorafenib, proteomics, drug response");
        }

        if (isNullOrEmpty(sampleProtocolArea.getText())) {
            sampleProtocolArea.setText(
                "HepG2 cells were cultured in DMEM supplemented with 10% FBS at 37°C and 5% CO2. " +
                "Cells were treated with 10 μM sorafenib or DMSO vehicle control for 24 hours. " +
                "After treatment, cells were washed with PBS and lysed in 8M urea buffer. " +
                "Proteins were reduced with DTT, alkylated with iodoacetamide, and digested with trypsin overnight. " +
                "Peptides were desalted using C18 spin columns and dried by vacuum centrifugation."
            );
        }

        if (isNullOrEmpty(dataProtocolArea.getText())) {
            dataProtocolArea.setText(
                "Peptides were analyzed on a Q Exactive HF mass spectrometer coupled to an Easy-nLC 1200 system. " +
                "Raw files were processed using MaxQuant (v2.0.3) with the Andromeda search engine. " +
                "Searches were performed against the UniProt human proteome database (UP000005640). " +
                "Label-free quantification (LFQ) was enabled with match between runs. " +
                "Statistical analysis was performed in Perseus using Student's t-test with FDR correction."
            );
        }

        // Add example experiment type if none selected
        if (!experimentTypeField.hasSelection()) {
            List<CvParam> commonTypes = uk.ac.ebi.pride.pxsubmit.service.OlsService.getCommonExperimentTypes();
            if (!commonTypes.isEmpty()) {
                experimentTypeField.addTerm(commonTypes.get(0)); // Add first common type (e.g., Bottom-up)
            }
        }
    }

    private boolean isValidTitle(String text) {
        return text != null &&
               text.trim().length() >= MIN_TITLE_LENGTH &&
               text.length() <= MAX_TITLE_LENGTH;
    }

    private boolean isValidDescription(String text) {
        return text != null &&
               text.trim().length() >= MIN_DESCRIPTION_LENGTH &&
               text.length() <= MAX_DESCRIPTION_LENGTH;
    }

    private boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private boolean isNullOrEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Update inline validation feedback based on current field values
     */
    private void updateValidationFeedback() {
        validationFeedback.clear();

        String title = titleField.getText();
        String desc = descriptionArea.getText();
        String keywords = keywordsInput.getText();
        String sampleProtocol = sampleProtocolArea.getText();
        String dataProtocol = dataProtocolArea.getText();
        boolean hasExperimentType = experimentTypeField != null && experimentTypeField.hasSelection();

        int completedFields = 0;
        int totalFields = 6;

        // Title validation
        if (title == null || title.trim().length() < MIN_TITLE_LENGTH) {
            validationFeedback.addWarning("Title: Minimum " + MIN_TITLE_LENGTH + " characters required");
        } else if (title.length() > MAX_TITLE_LENGTH) {
            validationFeedback.addError("Title: Maximum " + MAX_TITLE_LENGTH + " characters exceeded");
        } else {
            completedFields++;
        }

        // Description validation
        if (desc == null || desc.trim().length() < MIN_DESCRIPTION_LENGTH) {
            validationFeedback.addWarning("Description: Minimum " + MIN_DESCRIPTION_LENGTH + " characters required");
        } else if (desc.length() > MAX_DESCRIPTION_LENGTH) {
            validationFeedback.addError("Description: Maximum " + MAX_DESCRIPTION_LENGTH + " characters exceeded");
        } else {
            completedFields++;
        }

        // Keywords validation
        if (isNullOrEmpty(keywords)) {
            validationFeedback.addWarning("Keywords: At least one keyword required");
        } else {
            completedFields++;
        }

        // Experiment type validation
        if (!hasExperimentType) {
            validationFeedback.addWarning("Experiment Type: At least one type required");
        } else {
            completedFields++;
        }

        // Protocol validation
        if (isNullOrEmpty(sampleProtocol)) {
            validationFeedback.addWarning("Sample Processing Protocol: Required");
        } else {
            completedFields++;
        }

        if (isNullOrEmpty(dataProtocol)) {
            validationFeedback.addWarning("Data Processing Protocol: Required");
        } else {
            completedFields++;
        }

        // Show success if all complete
        if (completedFields == totalFields) {
            validationFeedback.setSuccess("All required fields complete - ready to proceed");
        } else {
            validationFeedback.addInfo(completedFields + "/" + totalFields + " required fields completed");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
